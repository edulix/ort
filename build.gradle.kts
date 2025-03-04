/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 * Copyright (C) 2019 Bosch Software Innovations GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

import java.net.URL

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val detektPluginVersion: String by project
val kotlinPluginVersion: String by project

val jacksonVersion: String by project
val kotestVersion: String by project
val log4jCoreVersion: String by project
val okhttpVersion: String by project

plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions")
    id("com.github.gmazzo.buildconfig")
    id("io.gitlab.arturbosch.detekt")
    id("org.barfuin.gradle.taskinfo")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        // For some reason "jgitVersion" needs to be declared here instead of globally.
        val jgitVersion: String by project
        classpath("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    }
}

if (version == Project.DEFAULT_VERSION) {
    version = org.eclipse.jgit.api.Git.open(rootDir).use { git ->
        // Make the output exactly match "git describe --abbrev=7 --always --tags --dirty", which is what is used in
        // "scripts/docker_build.sh".
        val description = git.describe().setAlways(true).setTags(true).call()
        val isDirty = git.status().call().hasUncommittedChanges()

        if (isDirty) "$description-dirty" else description
    }
}

logger.quiet("Building ORT version $version.")

// Note that Gradle's Java toolchain mechanism cannot be used here as that only applies to the Java version used in
// compile tasks. But already ORT's build scripts, like the compilation of this file itself, depend on Java 11 due to
// the Java target used by some plugins, see e.g. https://github.com/martoe/gradle-svntools-plugin#version-compatibility.
val javaVersion = JavaVersion.current()
if (!javaVersion.isCompatibleWith(JavaVersion.VERSION_11)) {
    throw GradleException("At least Java 11 is required, but Java $javaVersion is being used.")
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    val nonFinalQualifiers = listOf(
        "alpha", "b", "beta", "cr", "ea", "eap", "m", "milestone", "pr", "preview", "rc"
    ).joinToString("|", "(", ")")

    val nonFinalQualifiersRegex = Regex(".*[.-]$nonFinalQualifiers[.\\d-+]*", RegexOption.IGNORE_CASE)

    gradleReleaseChannel = "current"

    rejectVersionIf {
        candidate.version.matches(nonFinalQualifiersRegex)
    }
}

val mergeDetektReports by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.buildDir.resolve("reports/detekt/merged.sarif"))
}

allprojects {
    buildscript {
        repositories {
            mavenCentral()
        }
    }

    repositories {
        mavenCentral()
    }

    apply(plugin = "com.github.gmazzo.buildconfig")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Note: Kotlin DSL cannot directly access configurations that are created by applying a plugin in the very same
    // project, thus put configuration names in quotes to leverage lazy lookup.
    dependencies {
        "detektPlugins"(project(":detekt-rules"))

        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion")
    }

    detekt {
        // Align the detekt core and plugin versions.
        toolVersion = detektPluginVersion

        // Only configure differences to the default.
        buildUponDefaultConfig = true
        config = files("$rootDir/.detekt.yml")

        input = files("$rootDir/buildSrc", "build.gradle.kts", "src/main/kotlin", "src/test/kotlin",
            "src/funTest/kotlin")

        basePath = rootProject.projectDir.path

        reports {
            html.enabled = false
            sarif.enabled = true
            txt.enabled = false
            xml.enabled = false
        }
    }

    tasks.withType<Detekt> detekt@{
        dependsOn(":detekt-rules:assemble")

        finalizedBy(mergeDetektReports)

        mergeDetektReports {
            input.from(this@detekt.sarifReportFile)
        }
    }
}

subprojects {
    if (name == "reporter-web-app") return@subprojects

    // Apply core plugins.
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")

    // Apply third-party plugins.
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")

    sourceSets.create("funTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDirs("src/funTest/kotlin")
        }
    }

    // Associate the "funTest" compilation with the "main" compilation to be able to access "internal" objects from
    // functional tests.
    // TODO: The feature to access "internal" objects from functional tests is not actually used yet because the IDE
    //       would still highlight such access as an error. Still keep this code around until that bug in the IDE (see
    //       https://youtrack.jetbrains.com/issue/KT-34102) is fixed as the correct syntax for this code was not easy to
    //       determine.
    kotlin.target.compilations.run {
        getByName("funTest").associateWith(getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
    }

    plugins.withType<JavaLibraryPlugin> {
        dependencies {
            "testImplementation"(project(":utils:test-utils"))

            "testImplementation"("io.kotest:kotest-runner-junit5:$kotestVersion")
            "testImplementation"("io.kotest:kotest-assertions-core:$kotestVersion")

            "funTestImplementation"(sourceSets["main"].output)
        }

        configurations["funTestImplementation"].extendsFrom(configurations["testImplementation"])
    }

    configurations.all {
        // Do not tamper with configurations related to the detekt plugin, for some background information
        // https://github.com/detekt/detekt/issues/2501.
        if (!name.startsWith("detekt")) {
            resolutionStrategy {
                // Ensure all OkHttp versions match our version >= 4 to avoid Kotlin vs. Java issues with OkHttp 3.
                force("com.squareup.okhttp3:okhttp:$okhttpVersion")

                // Ensure all API library versions match our core library version.
                force("org.apache.logging.log4j:log4j-api:$log4jCoreVersion")

                // Ensure that all transitive versions of Kotlin libraries match our version of Kotlin.
                force("org.jetbrains.kotlin:kotlin-reflect:$kotlinPluginVersion")
                force("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinPluginVersion")
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        val customCompilerArgs = listOf(
            "-Xallow-result-return-type",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xopt-in=kotlin.io.path.ExperimentalPathApi",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )

        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "11"
            apiVersion = "1.5"
            freeCompilerArgs = freeCompilerArgs + customCompilerArgs
        }
    }

    tasks.dokkaHtml.configure {
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(11)

                externalDocumentationLink {
                    val baseUrl = "https://codehaus-plexus.github.io/plexus-containers/plexus-container-default/apidocs"
                    url.set(URL(baseUrl))
                    packageListUrl.set(URL("$baseUrl/package-list"))
                }

                externalDocumentationLink {
                    val majorMinorVersion = jacksonVersion.split('.').let { "${it[0]}.${it[1]}" }
                    val baseUrl = "https://fasterxml.github.io/jackson-databind/javadoc/$majorMinorVersion"
                    url.set(URL(baseUrl))
                    packageListUrl.set(URL("$baseUrl/package-list"))
                }

                externalDocumentationLink {
                    val baseUrl = "https://jakewharton.github.io/DiskLruCache"
                    url.set(URL(baseUrl))
                    packageListUrl.set(URL("$baseUrl/package-list"))
                }

                externalDocumentationLink {
                    val majorVersion = log4jCoreVersion.substringBefore('.')
                    val baseUrl = "https://logging.apache.org/log4j/$majorVersion.x/log4j-api/apidocs"
                    url.set(URL(baseUrl))
                    packageListUrl.set(URL("$baseUrl/package-list"))
                }
            }
        }
    }

    val funTest by tasks.registering(Test::class) {
        description = "Runs the functional tests."
        group = "Verification"

        classpath = sourceSets["funTest"].runtimeClasspath
        testClassesDirs = sourceSets["funTest"].output.classesDirs
    }

    // Enable JaCoCo only if a JacocoReport task is in the graph as JaCoCo
    // is using "append = true" which disables Gradle's build cache.
    gradle.taskGraph.whenReady {
        val enabled = allTasks.any { it is JacocoReport }

        tasks.withType<Test>().configureEach {
            extensions.configure(JacocoTaskExtension::class) {
                isEnabled = enabled
            }

            val testSystemProperties = mutableListOf("gradle.build.dir" to project.buildDir.path)

            listOf(
                "kotest.assertions.multi-line-diff",
                "kotest.tags.include",
                "kotest.tags.exclude"
            ).mapNotNullTo(testSystemProperties) { key ->
                System.getProperty(key)?.let { key to it }
            }

            systemProperties = testSystemProperties.toMap()

            testLogging {
                events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
            }

            useJUnitPlatform()
        }
    }

    tasks.named<JacocoReport>("jacocoTestReport").configure {
        reports {
            // Enable XML in addition to HTML for CI integration.
            xml.isEnabled = true
        }
    }

    tasks.register<JacocoReport>("jacocoFunTestReport").configure {
        description = "Generates code coverage report for the funTest task."
        group = "Reporting"

        executionData(funTest.get())
        sourceSets(sourceSets["main"])

        reports {
            // Enable XML in addition to HTML for CI integration.
            xml.isEnabled = true
        }
    }

    tasks.register("jacocoReport").configure {
        description = "Generates code coverage reports for all test tasks."
        group = "Reporting"

        dependsOn(tasks.withType<JacocoReport>())
    }

    tasks.named("check").configure {
        dependsOn(funTest)
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.register<Jar>("sourcesJar").configure {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    tasks.register<Jar>("dokkaHtmlJar").configure {
        dependsOn(tasks.dokkaHtml)

        description = "Assembles a jar archive containing the minimalistic HTML documentation."
        group = "Documentation"

        archiveClassifier.set("dokka")
        from(tasks.dokkaHtml)
    }

    tasks.register<Jar>("dokkaJavadocJar").configure {
        dependsOn(tasks.dokkaJavadoc)

        description = "Assembles a jar archive containing the Javadoc documentation."
        group = "Documentation"

        archiveClassifier.set("javadoc")
        from(tasks.dokkaJavadoc)
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>(name) {
                groupId = "org.ossreviewtoolkit"

                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["dokkaJavadocJar"])

                pom {
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://github.com/oss-review-toolkit/ort.git")
                        developerConnection.set("scm:git:git@github.com:oss-review-toolkit/ort.git")
                        tag.set(version.toString())
                        url.set("https://github.com/oss-review-toolkit/ort")
                    }
                }
            }
        }
    }
}

// Gradle's "dependencies" task selector only executes on a single / the current project [1]. However, sometimes viewing
// all dependencies at once is beneficial, e.g. for debugging version conflict resolution.
// [1]: https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html#sec:listing_dependencies
tasks.register("allDependencies").configure {
    val dependenciesTasks = allprojects.map { it.tasks.named("dependencies") }
    dependsOn(dependenciesTasks)

    // Ensure deterministic output by requiring to run tasks after each other in always the same order.
    dependenciesTasks.zipWithNext().forEach { (a, b) ->
        b.configure {
            mustRunAfter(a)
        }
    }
}

/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
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

@file:Suppress("TooManyFunctions")

package org.ossreviewtoolkit.analyzer.managers.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.node.ArrayNode

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

import org.ossreviewtoolkit.model.readJsonFile
import org.ossreviewtoolkit.utils.common.collectMessagesAsString
import org.ossreviewtoolkit.utils.common.toUri
import org.ossreviewtoolkit.utils.core.AuthenticatedProxy
import org.ossreviewtoolkit.utils.core.ProtocolProxyMap
import org.ossreviewtoolkit.utils.core.determineProxyFromURL
import org.ossreviewtoolkit.utils.core.log
import org.ossreviewtoolkit.utils.core.showStackTrace

/**
 * A dummy object to provide a logger for top-level functions.
 *
 * TODO: Remove this once https://youtrack.jetbrains.com/issue/KT-21599 is implemented.
 */
object NodeSupport

/**
 * Return whether the [directory] contains an NPM lock file.
 */
fun hasNpmLockFile(directory: File) =
    NPM_LOCK_FILES.any { lockfile ->
        File(directory, lockfile).isFile
    }

/**
 * Return whether the [directory] contains a Yarn lock file.
 */
fun hasYarnLockFile(directory: File) =
    YARN_LOCK_FILES.any { lockfile ->
        File(directory, lockfile).isFile
    }

/**
 * Map [definitionFiles] to contain only files handled by NPM.
 */
fun mapDefinitionFilesForNpm(definitionFiles: Collection<File>): Set<File> =
    getPackageJsonInfo(definitionFiles.toSet()).filter { entry ->
        !isHandledByYarn(entry)
    }.mapTo(mutableSetOf()) { it.definitionFile }

/**
 * Map [definitionFiles] to contain only files handled by Yarn.
 */
fun mapDefinitionFilesForYarn(definitionFiles: Collection<File>): Set<File> =
    getPackageJsonInfo(definitionFiles.toSet()).filter { entry ->
        isHandledByYarn(entry) && !entry.isYarnWorkspaceSubmodule
    }.mapTo(mutableSetOf()) { it.definitionFile }

/**
 * Expand an NPM shortcut [url] to a regular URL as used for dependencies, see
 * https://docs.npmjs.com/cli/v7/configuring-npm/package-json#urls-as-dependencies.
 */
fun expandNpmShortcutUrl(url: String): String {
    // A hierarchical URI looks like
    //     [scheme:][//authority][path][?query][#fragment]
    // where a server-based "authority" has the syntax
    //     [user-info@]host[:port]
    val uri = url.toUri().getOrElse {
        // Fall back to returning the original URL.
        return url
    }

    val path = uri.schemeSpecificPart

    // Do not mess with crazy URLs.
    if (path.startsWith("git@") || path.startsWith("github.com") || path.startsWith("gitlab.com")) return url

    return if (!path.isNullOrEmpty() && listOf(uri.authority, uri.query).all { it == null }) {
        // See https://docs.npmjs.com/cli/v7/configuring-npm/package-json#github-urls.
        val revision = uri.fragment?.let { "#$it" }.orEmpty()

        // See https://docs.npmjs.com/cli/v7/configuring-npm/package-json#repository.
        when (uri.scheme) {
            null, "github" -> "https://github.com/$path.git$revision"
            "gist" -> "https://gist.github.com/$path$revision"
            "bitbucket" -> "https://bitbucket.org/$path.git$revision"
            "gitlab" -> "https://gitlab.com/$path.git$revision"
            else -> url
        }
    } else {
        url
    }
}

/**
 * Return all proxies defined in the provided [NPM configuration][npmRc].
 */
fun readProxySettingsFromNpmRc(npmRc: String): ProtocolProxyMap {
    val map = mutableMapOf<String, MutableList<AuthenticatedProxy>>()

    npmRc.lines().forEach { line ->
        val keyAndValue = line.split('=', limit = 2).map { it.trim() }
        if (keyAndValue.size != 2) return@forEach

        val (key, value) = keyAndValue
        when (key) {
            "proxy" -> determineProxyFromURL(value)?.let {
                map.getOrPut("http") { mutableListOf() } += it
            }

            "https-proxy" -> determineProxyFromURL(value)?.let {
                map.getOrPut("https") { mutableListOf() } += it
            }
        }
    }

    return map
}

/**
 * Return the npm registry defined in the provided [NPM configuration][npmRc] or null.
 */
fun readRegistryFromNpmRc(npmRc: String): String? {
    npmRc.lines().forEach { line ->
        val keyAndValue = line.split('=', limit = 2).map { it.trim() }
        if (keyAndValue.size != 2) return@forEach

        val (key, value) = keyAndValue
        if (key == "registry") return value
    }

    return null
}

private val NPM_LOCK_FILES = listOf("npm-shrinkwrap.json", "package-lock.json")
private val YARN_LOCK_FILES = listOf("yarn.lock")

private data class PackageJsonInfo(
    val definitionFile: File,
    val hasYarnLockfile: Boolean = false,
    val hasNpmLockfile: Boolean = false,
    val isYarnWorkspaceRoot: Boolean = false,
    val isYarnWorkspaceSubmodule: Boolean = false
)

private fun isHandledByYarn(entry: PackageJsonInfo) =
    entry.isYarnWorkspaceRoot || entry.isYarnWorkspaceSubmodule || entry.hasYarnLockfile

private fun getPackageJsonInfo(definitionFiles: Set<File>): Collection<PackageJsonInfo> {
    val yarnWorkspaceSubmodules = getYarnWorkspaceSubmodules(definitionFiles)

    return definitionFiles.map { definitionFile ->
        PackageJsonInfo(
            definitionFile = definitionFile,
            isYarnWorkspaceRoot = isYarnWorkspaceRoot(definitionFile),
            hasYarnLockfile = hasYarnLockFile(definitionFile.parentFile),
            hasNpmLockfile = hasNpmLockFile(definitionFile.parentFile),
            isYarnWorkspaceSubmodule = yarnWorkspaceSubmodules.contains(definitionFile)
        )
    }
}

private fun isYarnWorkspaceRoot(definitionFile: File) =
    try {
        readJsonFile(definitionFile).has("workspaces")
    } catch (e: JsonProcessingException) {
        e.showStackTrace()

        NodeSupport.log.error {
            "Could not parse '${definitionFile.invariantSeparatorsPath}': ${e.collectMessagesAsString()}"
        }

        false
    }

private fun getYarnWorkspaceSubmodules(definitionFiles: Set<File>): Set<File> {
    val result = mutableSetOf<File>()

    definitionFiles.forEach { definitionFile ->
        val workspaceMatchers = getWorkspaceMatchers(definitionFile)
        workspaceMatchers.forEach { matcher ->
            definitionFiles.forEach inner@{ other ->
                // Since yarn workspaces matchers support '*' and '**' to match multiple directories the matcher
                // cannot be used as is for matching the 'package.json' file. Thus matching against the project
                // directory since this works out of the box. See also:
                //   https://github.com/yarnpkg/yarn/issues/3986
                //   https://github.com/yarnpkg/yarn/pull/5607
                val projectDir = other.parentFile.toPath()
                if (other != definitionFile && matcher.matches(projectDir)) {
                    result += other
                    return@inner
                }
            }
        }
    }

    return result
}

private fun getWorkspaceMatchers(definitionFile: File): List<PathMatcher> {
    var workspaces = try {
        readJsonFile(definitionFile).get("workspaces")
    } catch (e: JsonProcessingException) {
        e.showStackTrace()

        NodeSupport.log.error {
            "Could not parse '${definitionFile.invariantSeparatorsPath}': ${e.collectMessagesAsString()}"
        }

        null
    }

    if (workspaces != null && workspaces !is ArrayNode) {
        workspaces = workspaces["packages"]
    }

    return workspaces?.map {
        val pattern = "glob:${definitionFile.parentFile.invariantSeparatorsPath}/${it.textValue()}"
        FileSystems.getDefault().getPathMatcher(pattern)
    }.orEmpty()
}

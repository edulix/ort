/*
 * Copyright (C) 2019 HERE Europe B.V.
 * Copyright (C) 2020-2021 Bosch.IO GmbH
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

package org.ossreviewtoolkit.model.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.PropertySourceContext
import com.sksamuel.hoplite.fp.getOrElse
import com.sksamuel.hoplite.fp.valid
import com.sksamuel.hoplite.parsers.toNode

import java.io.File

import org.ossreviewtoolkit.model.Severity
import org.ossreviewtoolkit.utils.core.log

/**
 * The configuration model for all ORT components.
 */
data class OrtConfiguration(
    /**
     * The license file patterns.
     */
    val licenseFilePatterns: LicenseFilenamePatterns = LicenseFilenamePatterns.DEFAULT,

    /**
     * A flag to indicate whether authors should be considered as copyright holders.
     */
    val addAuthorsToCopyrights: Boolean = false,

    /**
     * The threshold from which on issues count as severe.
     */
    val severeIssueThreshold: Severity = Severity.WARNING,

    /**
     * The configuration of the analyzer.
     */
    val analyzer: AnalyzerConfiguration = AnalyzerConfiguration(),

    /**
     * The configuration of the advisors, using the advisor's name as the key.
     */
    val advisor: AdvisorConfiguration = AdvisorConfiguration(),

    /**
     * The configuration of the downloader.
     */
    val downloader: DownloaderConfiguration = DownloaderConfiguration(),

    /**
     * The configuration of the scanner.
     */
    val scanner: ScannerConfiguration = ScannerConfiguration(),

    /**
     * The configuration of the notifier.
     */
    val notifier: NotifierConfiguration = NotifierConfiguration()
) {
    companion object {
        /**
         * Load the [OrtConfiguration]. The different sources are used with this priority:
         *
         * 1. [Command line arguments][args]
         * 2. [Configuration file][file]
         *
         * The configuration file is optional and does not have to exist. However, if it exists, but does not
         * contain a valid configuration, an [IllegalArgumentException] is thrown.
         */
        fun load(args: Map<String, String>? = null, file: File? = null): OrtConfiguration {
            val sources = listOfNotNull(
                args?.filterKeys { it.startsWith("ort.") }?.takeUnless { it.isEmpty() }?.let {
                    log.info {
                        val argsList = it.map { (k, v) -> "\t$k=$v" }
                        "Using ORT configuration arguments:\n" + argsList.joinToString("\n")
                    }

                    argumentsSource(it)
                },
                file?.takeIf { it.isFile }?.let {
                    log.info { "Using ORT configuration file '$it'." }
                    PropertySource.file(it)
                }
            )

            val loader = ConfigLoader.Builder().addSources(sources).build()
            val config = loader.loadConfig<OrtConfigurationWrapper>()

            return config.getOrElse { failure ->
                if (sources.isNotEmpty()) {
                    throw IllegalArgumentException("Failed to load ORT configuration: ${failure.description()}")
                }

                OrtConfigurationWrapper(OrtConfiguration())
            }.ort
        }

        /**
         * Generate a [PropertySource] providing access to the [args] the user has passed on the command line.
         */
        private fun argumentsSource(args: Map<String, String>): PropertySource {
            val node = args.toProperties().toNode("arguments").valid()
            return object : PropertySource {
                override fun node(context: PropertySourceContext): ConfigResult<Node> = node
            }
        }
    }
}

/**
 * An internal wrapper class to hold an [OrtConfiguration]. This class is needed to correctly map the _ort_
 * prefix in configuration files when they are processed by the underlying configuration library.
 */
internal data class OrtConfigurationWrapper(
    val ort: OrtConfiguration
)

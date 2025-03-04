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

package org.ossreviewtoolkit.model.config

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

import org.ossreviewtoolkit.model.utils.FileArchiver
import org.ossreviewtoolkit.utils.core.storage.FileStorage

typealias ScannerOptions = Map<String, String>

/**
 * The configuration model of the scanner.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScannerConfiguration(
    /**
     * A flag to indicate whether packages that have a concluded license and authors set (to derive copyrights from)
     * should be skipped in the scan in favor of only using the declared information.
     */
    val skipConcluded: Boolean = false,

    /**
     * Configuration of a [FileArchiver] that archives certain scanned files in an external [FileStorage].
     */
    val archive: FileArchiverConfiguration? = null,

    /**
     * Create archives for packages that have a stored scan result but no license archive yet.
     */
    val createMissingArchives: Boolean = false,

    /**
     * Scanner specific configuration options. The key needs to match the name of the scanner class, e.g. "ScanCode"
     * for the ScanCode wrapper. See the documentation of the scanner for available options.
     */
    @JsonAlias("scanner")
    val options: Map<String, ScannerOptions>? = null,

    /**
     * A map with the configurations of the scan result storages available. Based on this information the actual
     * storages are created. Storages can be configured as readers or writers of scan results. Having this map
     * makes it possible for storage instances to act in both roles without having to duplicate configuration.
     */
    val storages: Map<String, ScanStorageConfiguration>? = null,

    /**
     * A list with the IDs of scan storages that are queried for existing scan results. The strings in this list
     * must match keys in the storages map.
     */
    val storageReaders: List<String>? = null,

    /**
     * A list with the IDs of scan storages that are called to persist scan results. The strings in this list
     * must match keys in the storages map.
     */
    val storageWriters: List<String>? = null,

    /**
     * A list of glob expressions that match file paths which are to be excluded from scan results.
     */
    val ignorePatterns: List<String> = listOf(
        "**/*.ort.yml",
        "**/META-INF/DEPENDENCIES"
    )
)

/*
 * Copyright (C) 2021 HERE Europe B.V.
 * Copyright (C) 2021 Bosch.IO GmbH
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

package org.ossreviewtoolkit.scanner.experimental

import java.io.File

import org.ossreviewtoolkit.model.KnownProvenance
import org.ossreviewtoolkit.model.Package
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.ScanResult
import org.ossreviewtoolkit.model.ScanSummary
import org.ossreviewtoolkit.model.ScannerDetails
import org.ossreviewtoolkit.model.config.ScannerConfiguration
import org.ossreviewtoolkit.scanner.ScannerCriteria

/**
 * The base interface for all types of scanners.
 */
sealed interface ScannerWrapper {
    /**
     * The name of the scanner.
     */
    val name: String

    /**
     * The details of the scanner.
     */
    val details: ScannerDetails

    /**
     * The [ScannerCriteria] object to be used when looking up existing scan results from a scan storage. By default,
     * the properties of this object are initialized by the scanner implementation. These defaults can be overridden
     * with the [ScannerConfiguration.options] property: Use properties of the form _scannerName.criteria.property_,
     * where _scannerName_ is the name of the scanner and _property_ is the name of a property of the [ScannerCriteria]
     * class. For instance, to specify that a specific minimum version of ScanCode is allowed, set this property:
     * `options.ScanCode.criteria.minScannerVersion=3.0.2`.
     */
    val criteria: ScannerCriteria
}

/**
 * A wrapper interface for remote scanners. A remote scanner is a scanner that is not executed locally but is running on
 * a different machine and can be accessed via a remote interface.
 */
sealed interface RemoteScannerWrapper : ScannerWrapper

/**
 * A wrapper interface for remote scanners that operate on [Package]s.
 */
interface PackageBasedRemoteScannerWrapper : RemoteScannerWrapper {
    fun scanPackage(pkg: Package): ScanResult
}

/**
 * A wrapper interface for remote scanners that operate on [Provenance]s.
 */
interface ProvenanceBasedRemoteScannerWrapper : RemoteScannerWrapper {
    fun scanProvenance(provenance: KnownProvenance): ScanResult
}

/**
 * A wrapper interface for local scanners. A local scanner is a scanner that is running on the same machine and scanning
 * files on the local filesystem.
 */
interface LocalScannerWrapper : ScannerWrapper {
    fun scanPath(path: File): ScanSummary
}

/*
 * Copyright (C) 2021 HERE Europe B.V.
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

import org.ossreviewtoolkit.model.ArtifactProvenance
import org.ossreviewtoolkit.model.KnownProvenance
import org.ossreviewtoolkit.model.RepositoryProvenance

/**
 * This class contains information about a [root] provenance and all nested [subRepositories].
 */
data class NestedProvenance(
    /**
     * The root provenance that contains the [nested provenances][subRepositories].
     */
    val root: KnownProvenance,

    /**
     * If [root] is a [RepositoryProvenance] this map contains all paths which contain nested repositories associated
     * with the [RepositoryProvenance] of the nested repository. If [root] is an [ArtifactProvenance] this map is always
     * empty.
     */
    val subRepositories: Map<String, RepositoryProvenance>
) {
    /**
     * Return a set of all contained [KnownProvenance]s.
     */
    fun getProvenances(): Set<KnownProvenance> =
        subRepositories.values.toMutableSet<KnownProvenance>().also { it += root }
}

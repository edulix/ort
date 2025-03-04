/*
 * Copyright (C) 2020 HERE Europe B.V.
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

package org.ossreviewtoolkit.utils.spdx

object SpdxConstants {
    /**
     * Represents a not present value, which has been determined to actually be not present. This representation must
     * not be used if [NOASSERTION] could be used instead.
     */
    const val NONE = "NONE"

    /**
     * Represents a not present value where any of the following cases applies:
     *
     * 1. no attempt was made to determine the information.
     * 2. intentionally no information is provided, whereas no meaning should be derived from the absence of the
     *    information.
     */
    const val NOASSERTION = "NOASSERTION"

    /**
     * A prefix used in fields like "originator", "supplier", or "annotator" to describe a person.
     */
    const val PERSON = "Person: "

    /**
     * A prefix used in fields like "originator", "supplier", or "annotator" to describe an organization.
     */
    const val ORGANIZATION = "Organization: "

    /**
     * A prefix used in fields like "annotator" to describe a tool.
     */
    const val TOOL = "Tool: "

    /**
     * The prefix to be used for SPDX document IDs or references.
     */
    const val REF_PREFIX = "SPDXRef-"

    /**
     * The prefix to be used for references to other SPDX documents.
     */
    const val DOCUMENT_REF_PREFIX = "DocumentRef-"

    /**
     * The prefix to be used for references to licenses that are not part of the SPDX license list.
     */
    const val LICENSE_REF_PREFIX = "LicenseRef-"

    /**
     * The URL that points to list of SPDX licenses.
     */
    const val LICENSE_LIST_URL = "https://spdx.org/licenses/"

    /**
     * Return true if and only if the given value is null or equals [NONE] or [NOASSERTION].
     */
    fun isNotPresent(value: String?) = value in setOf(null, NONE, NOASSERTION)

    /**
     * Return true if and only if the given value is not null and does not equal [NONE] or [NOASSERTION].
     */
    fun isPresent(value: String?) = !isNotPresent(value)
}

/*
 * Copyright (C) 2020 Bosch.IO GmbH
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

package org.ossreviewtoolkit.advisor

import java.time.Instant
import java.util.ServiceLoader

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

import org.ossreviewtoolkit.model.AdvisorRecord
import org.ossreviewtoolkit.model.AdvisorResult
import org.ossreviewtoolkit.model.AdvisorRun
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.config.AdvisorConfiguration
import org.ossreviewtoolkit.utils.core.Environment
import org.ossreviewtoolkit.utils.core.log

/**
 * The class to manage [AdviceProvider]s. It invokes the configured providers and adds their findings to the current
 * [OrtResult].
 */
class Advisor(
    private val providerFactories: List<AdviceProviderFactory>,
    private val config: AdvisorConfiguration
) {
    companion object {
        private val LOADER = ServiceLoader.load(AdviceProviderFactory::class.java)!!

        /**
         * The list of all available [AdviceProvider]s in the classpath.
         */
        val ALL by lazy { LOADER.iterator().asSequence().toList().sortedBy { it.providerName } }
    }

    fun retrieveFindings(ortResult: OrtResult, skipExcluded: Boolean = false): OrtResult {
        val startTime = Instant.now()

        if (ortResult.analyzer == null) {
            log.warn {
                "Cannot run the advisor as the provided ORT result does not contain an analyzer result. " +
                        "No result will be added."
            }

            return ortResult
        }

        val providers = providerFactories.map { it.create(config) }

        val results = sortedMapOf<Identifier, List<AdvisorResult>>()

        val packages = ortResult.getPackages(skipExcluded).map { it.pkg }

        runBlocking {
            providers.map { provider ->
                async {
                    provider.retrievePackageFindings(packages)
                }
            }.forEach { providerResults ->
                providerResults.await().forEach { (pkg, advisorResults) ->
                    results.merge(pkg.id, advisorResults) { oldResults, newResults ->
                        oldResults + newResults
                    }
                }
            }
        }

        val advisorRecord = AdvisorRecord(results)

        val endTime = Instant.now()

        val advisorRun = AdvisorRun(startTime, endTime, Environment(), config, advisorRecord)
        return ortResult.copy(advisor = advisorRun)
    }
}

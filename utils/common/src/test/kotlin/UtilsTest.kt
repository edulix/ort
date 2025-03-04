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

package org.ossreviewtoolkit.utils.common

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot

import java.io.File

import org.ossreviewtoolkit.utils.test.shouldNotBeNull

class UtilsTest : WordSpec({
    "getCommonFileParent" should {
        "return null for an empty list" {
            getCommonFileParent(emptyList()) should beNull()
        }

        "return null for files that have no directory in common".config(enabled = Os.isWindows) {
            // On non-Windows, all files have the root directory in common.
            getCommonFileParent(listOf(File("C:/foo"), File("D:/bar"))) should beNull()
        }

        "return the absolute common directory for relative files" {
            getCommonFileParent(listOf(File("foo"), File("bar"))) shouldBe File(".").absoluteFile.normalize()
        }

        "return the absolute parent directory for a single file" {
            getCommonFileParent(listOf(File("/foo/bar"))) shouldBe File("/foo").absoluteFile
        }
    }

    "getPathFromEnvironment" should {
        "find system executables on Windows".config(enabled = Os.isWindows) {
            val winverPath = File(Os.env["SYSTEMROOT"], "system32/winver.exe")

            getPathFromEnvironment("winver") shouldNot beNull()
            getPathFromEnvironment("winver") shouldBe winverPath

            getPathFromEnvironment("winver.exe") shouldNot beNull()
            getPathFromEnvironment("winver.exe") shouldBe winverPath

            getPathFromEnvironment("") should beNull()
            getPathFromEnvironment("*") should beNull()
            getPathFromEnvironment("nul") should beNull()
        }

        "find system executables on non-Windows".config(enabled = !Os.isWindows) {
            getPathFromEnvironment("sh") shouldNotBeNull {
                toString() shouldBeIn listOf("/bin/sh", "/usr/bin/sh")
            }

            getPathFromEnvironment("") should beNull()
            getPathFromEnvironment("/") should beNull()
        }
    }

    "getAllAncestorDirectories" should {
        "return all ancestor directories ordered along the path to root" {
            getAllAncestorDirectories("/a/b/c") should containExactly(
                "/a/b",
                "/a",
                "/"
            )
        }
    }
})

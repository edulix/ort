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

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.file.aDirectory
import io.kotest.matchers.file.exist
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot

import java.io.File
import java.io.IOException
import java.util.Locale

import org.ossreviewtoolkit.utils.test.containExactly
import org.ossreviewtoolkit.utils.test.createSpecTempDir
import org.ossreviewtoolkit.utils.test.createTestTempDir
import org.ossreviewtoolkit.utils.test.createTestTempFile
import org.ossreviewtoolkit.utils.test.shouldNotBeNull

class ExtensionsTest : WordSpec({
    "ByteArray.toHexString" should {
        "correctly convert a byte array to a string of hexadecimal digits" {
            byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte()).toHexString() shouldBe "deadbeef"
        }
    }

    "File.expandTilde" should {
        "expand the path if the SHELL environment variable is set".config(enabled = Os.env["SHELL"] != null) {
            File("~/Desktop").expandTilde() shouldBe Os.userHomeDirectory.resolve("Desktop")
        }

        "make the path absolute if the SHELL environment variable is unset".config(enabled = Os.env["SHELL"] == null) {
            File("~/Desktop").expandTilde() shouldBe File("~/Desktop").absoluteFile
        }
    }

    "File.isSymbolicLink" should {
        val tempDir = createSpecTempDir()
        val file = tempDir.resolve("file").apply { createNewFile() }
        val directory = tempDir.resolve("directory").apply { safeMkdirs() }

        "return 'false' for non-existent files" {
            tempDir.resolve("non-existent").let { nonExistent ->
                nonExistent shouldNot exist()
                nonExistent.isSymbolicLink() shouldBe false
            }
        }

        "return 'false' for files" {
            file.isFile shouldBe true
            file.isSymbolicLink() shouldBe false
        }

        "return 'false' for directories" {
            directory.isDirectory shouldBe true
            directory.isSymbolicLink() shouldBe false
        }

        "return 'false' for hard links on Windows".config(enabled = Os.isWindows) {
            ProcessCapture(tempDir, "cmd", "/c", "mklink", "/h", "hardlink", "file")

            tempDir.resolve("hardlink").let { hardlink ->
                hardlink.isFile shouldBe true
                hardlink.isSymbolicLink() shouldBe false
            }
        }

        "return 'true' for junctions on Windows".config(enabled = Os.isWindows) {
            ProcessCapture(tempDir, "cmd", "/c", "mklink", "/j", "junction", "directory")

            tempDir.resolve("junction").let { junction ->
                junction.isDirectory shouldBe true
                junction.isSymbolicLink() shouldBe true
            }
        }

        "return 'true' for symbolic links to files on Windows".config(enabled = Os.isWindows) {
            ProcessCapture(tempDir, "cmd", "/c", "mklink", "symlink-to-file", "file")

            tempDir.resolve("symlink-to-file").let { symlinkToFile ->
                symlinkToFile.isFile shouldBe true
                symlinkToFile.isSymbolicLink() shouldBe true
            }
        }

        "return 'true' for symbolic links to directories on Windows".config(enabled = Os.isWindows) {
            ProcessCapture(tempDir, "cmd", "/c", "mklink", "/d", "symlink-to-directory", "directory")

            tempDir.resolve("symlink-to-directory").let { symlinkToDirectory ->
                symlinkToDirectory.isDirectory shouldBe true
                symlinkToDirectory.isSymbolicLink() shouldBe true
            }
        }
    }

    "File.searchUpwardsForFile" should {
        "find the README.md file case insensitive" {
            val readmeFile = File(".").searchUpwardsForFile("ReadMe.MD", true)

            readmeFile shouldNotBeNull {
                this shouldBe File("../..").absoluteFile.normalize().resolve("README.md")
            }
        }

        "find the README.md file case sensitive" {
            val readmeFile = File(".").searchUpwardsForFile("README.md", false)

            readmeFile shouldNotBeNull {
                this shouldBe File("../..").absoluteFile.normalize().resolve("README.md")
            }
        }

        "not find the README.md with wrong cases" {
            val readmeFile = File(".").searchUpwardsForFile("ReadMe.MD", false)

            readmeFile should beNull()
        }
    }

    "File.searchUpwardsForSubdirectory" should {
        "find the root Git directory" {
            val gitRoot = File(".").searchUpwardsForSubdirectory(".git")

            gitRoot shouldNotBeNull {
                this shouldBe File("../..").absoluteFile.normalize()
            }
        }
    }

    "File.safeMkDirs" should {
        "succeed if directory already exists" {
            val directory = createTestTempDir()

            directory.isDirectory shouldBe true
            shouldNotThrow<IOException> { directory.safeMkdirs() }
            directory.isDirectory shouldBe true // should still be a directory afterwards
        }

        "succeed if directory could be created" {
            val parent = createTestTempDir()
            val child = File(parent, "child")

            parent.isDirectory shouldBe true
            shouldNotThrow<IOException> { child.safeMkdirs() }
            child.isDirectory shouldBe true
        }

        "succeed if file parent does not yet exist" {
            // Test case for an unexpected behaviour of File.mkdirs() which returns false for
            // File(File("parent1/parent2"), "/").mkdirs() if both "parent" directories do not exist, even when the
            // directory was successfully created.
            val parent = createTestTempDir()
            val nonExistingParent = File(parent, "parent1/parent2")
            val child = File(nonExistingParent, "/")

            parent shouldBe aDirectory()
            nonExistingParent shouldNot exist()
            child shouldNot exist()
            shouldNotThrow<IOException> { child.safeMkdirs() }
            child shouldBe aDirectory()
        }

        "throw exception if file is not a directory" {
            val file = createTestTempFile()

            file.isFile shouldBe true
            shouldThrow<IOException> { file.safeMkdirs() }
            file.isFile shouldBe true // should still be a file afterwards
        }
    }

    "String.isSemanticVersion" should {
        "return true for a semantic version" {
            "1.0.0".isSemanticVersion() shouldBe true
        }

        "return false for a URL" {
            "https://registry.npmjs.org/form-data/-/form-data-0.2.0.tgz".isSemanticVersion() shouldBe false
        }
    }

    "String.percentEncode" should {
        "encode characters according to RFC 3986" {
            val genDelims = listOf(':', '/', '?', '#', '[', ']', '@')
            val subDelims = listOf('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
            val reserved = genDelims + subDelims

            val alpha = CharArray(26) { 'a' + it } + CharArray(26) { 'A' + it }
            val digit = CharArray(10) { '0' + it }
            val special = listOf('-', '.', '_', '~')
            val unreserved = alpha + digit + special

            assertSoftly {
                reserved.forEach {
                    val hexString = String.format(Locale.ROOT, "%%%02X", it.code)
                    it.toString().percentEncode() shouldBe hexString
                }

                unreserved.forEach {
                    val singleCharString = it.toString()
                    singleCharString.percentEncode() shouldBe singleCharString
                }

                " ".percentEncode() shouldBe "%20"
            }
        }
    }

    "String.replaceCredentialsInUri" should {
        "strip the user name from a string representing a URL" {
            "ssh://bot@gerrit.host.com:29418/parent/project".replaceCredentialsInUri() shouldBe
                    "ssh://gerrit.host.com:29418/parent/project"
        }

        "strip the user name and password from a string representing a URL" {
            "ssh://bot:pass@gerrit.host.com:29418/parent/project".replaceCredentialsInUri() shouldBe
                    "ssh://gerrit.host.com:29418/parent/project"
        }

        "replace the user name from a string representing a URL" {
            "ssh://bot@gerrit.host.com:29418/parent/project".replaceCredentialsInUri("user") shouldBe
                    "ssh://user@gerrit.host.com:29418/parent/project"
        }

        "replace the user name and password from a string representing a URL" {
            "ssh://bot:pass@gerrit.host.com:29418/parent/project".replaceCredentialsInUri("user:secret") shouldBe
                    "ssh://user:secret@gerrit.host.com:29418/parent/project"
        }

        "not modify encodings in a URL" {
            "ssh://bot@gerrit.host.com:29418/parent/project%20with%20spaces".replaceCredentialsInUri() shouldBe
                    "ssh://gerrit.host.com:29418/parent/project%20with%20spaces"
        }

        "not modify a string not representing a URL" {
            "This is not a URL".replaceCredentialsInUri() shouldBe "This is not a URL"
        }
    }

    "String.urlencode" should {
        val str = "project: fünky\$name*>nul."

        "encode '*'" {
            "*".fileSystemEncode() shouldBe "%2A"
        }

        "encode '.'" {
            ".".fileSystemEncode() shouldBe "%2E"
        }

        "encode ':'" {
            ":".fileSystemEncode() shouldBe "%3A"
        }

        "create a valid file name" {
            val tempDir = createTestTempDir()
            val fileFromStr = tempDir.resolve(str.fileSystemEncode()).apply { writeText("dummy") }

            fileFromStr.isFile shouldBe true
        }
    }

    "Map.zip" should {
        val operation = { left: Int?, right: Int? -> (left ?: 0) + (right ?: 0) }

        "correctly merge maps" {
            val map = mapOf(
                "1" to 1,
                "2" to 2,
                "3" to 3
            )
            val other = mapOf(
                "3" to 3,
                "4" to 4
            )

            map.zip(other, operation) shouldBe mapOf(
                "1" to 1,
                "2" to 2,
                "3" to 6,
                "4" to 4
            )
        }

        "not fail if this map is empty" {
            val other = mapOf("1" to 1)

            emptyMap<String, Int>().zip(other, operation) should containExactly("1" to 1)
        }

        "not fail if other map is empty" {
            val map = mapOf("1" to 1)

            map.zip(emptyMap(), operation) should containExactly("1" to 1)
        }
    }

    "Map.zipWithDefault" should {
        val operation = { left: Int, right: Int -> left + right }

        "correctly merge maps" {
            val map = mapOf(
                "1" to 1,
                "2" to 2,
                "3" to 3
            )
            val other = mapOf(
                "3" to 3,
                "4" to 4
            )

            map.zipWithDefault(other, 1, operation) shouldBe mapOf(
                "1" to 2,
                "2" to 3,
                "3" to 6,
                "4" to 5
            )
        }

        "not fail if this map is empty" {
            val other = mapOf("1" to 1)

            emptyMap<String, Int>().zipWithDefault(other, 1, operation) should containExactly("1" to 2)
        }

        "not fail if other map is empty" {
            val map = mapOf("1" to 1)

            map.zipWithDefault(emptyMap(), 1, operation) should containExactly("1" to 2)
        }
    }

    "Map.zipWithCollections" should {
        "correctly merge maps" {
            val map = mapOf(
                "1" to listOf(1),
                "2" to listOf(2),
                "3" to listOf(3)
            )
            val other = mapOf(
                "3" to listOf(3),
                "4" to listOf(4)
            )

            map.zipWithCollections(other) shouldBe mapOf(
                "1" to listOf(1),
                "2" to listOf(2),
                "3" to listOf(3, 3),
                "4" to listOf(4)
            )
        }
    }

    "String.isValidUri" should {
        "return true for a valid URI" {
            "https://github.com/oss-review-toolkit/ort".isValidUri() shouldBe true
        }

        "return false for an invalid URI" {
            "https://github.com/oss-review-toolkit/ort, ".isValidUri() shouldBe false
        }
    }
})

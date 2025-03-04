/*
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

package org.ossreviewtoolkit.analyzer.managers

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import java.io.File

import org.ossreviewtoolkit.model.Hash
import org.ossreviewtoolkit.model.OrtIssue
import org.ossreviewtoolkit.model.RemoteArtifact
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.utils.spdx.SpdxConstants
import org.ossreviewtoolkit.utils.spdx.SpdxModelMapper
import org.ossreviewtoolkit.utils.spdx.model.SpdxChecksum
import org.ossreviewtoolkit.utils.spdx.model.SpdxDocument
import org.ossreviewtoolkit.utils.spdx.model.SpdxExternalDocumentReference
import org.ossreviewtoolkit.utils.spdx.model.SpdxPackage
import org.ossreviewtoolkit.utils.test.shouldNotBeNull

/*
 * The below package data is based on example data taken from the SPDX specification.
 */

private val pkgForBinaryArtifact = SpdxPackage(
    spdxId = "SPDXRef-Package-Dummy",
    downloadLocation = "https://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar",
    filesAnalyzed = false,
    copyrightText = "Copyright 2008-2010 John Smith",
    licenseConcluded = "NOASSERTION",
    licenseDeclared = "NOASSERTION",
    name = "Dummy"
)

private val pkgForSourceArtifact = SpdxPackage(
    spdxId = "SPDXRef-Package-Dummy",
    downloadLocation = "http://ftp.gnu.org/gnu/glibc/glibc-ports-2.15.tar.gz",
    filesAnalyzed = true,
    copyrightText = "Copyright 2008-2010 John Smith",
    licenseConcluded = "NOASSERTION",
    licenseDeclared = "NOASSERTION",
    name = "Dummy"
)

private val pkgForVcs = SpdxPackage(
    spdxId = "SPDXRef-Package-Dummy",
    downloadLocation = "git+https://git.myproject.org/MyProject.git@master#/src/MyClass.cpp",
    copyrightText = "Copyright 2008-2010 John Smith",
    licenseConcluded = "NOASSERTION",
    licenseDeclared = "NOASSERTION",
    name = "Dummy"
)

private fun createSpdxDocument(packages: List<SpdxPackage>?, hasExternalDocumentRefs: Boolean = true): SpdxDocument {
    val spdxDocument = createSpdxDocument()

    val spdxPackages = packages ?: spdxDocument.packages

    return if (hasExternalDocumentRefs) {
        spdxDocument.copy(packages = spdxPackages)
    } else {
        spdxDocument.copy(packages = spdxPackages, externalDocumentRefs = emptyList())
    }
}

private fun createSpdxDocument(): SpdxDocument {
    val projectDir = File("src/funTest/assets/projects/synthetic/spdx").absoluteFile
    val definitionFile = projectDir.resolve("project/project.spdx.yml")
    return SpdxModelMapper.read(definitionFile)
}

class SpdxDocumentFileTest : WordSpec({
    "getBinaryArtifact()" should {
        "return a RemoteArtifact for a downloadLocation that points to a binary artifact" {
            pkgForBinaryArtifact.getBinaryArtifact() shouldBe RemoteArtifact(
                url = "https://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar",
                hash = Hash.NONE
            )
        }

        "return null for a downloadLocation that does not point to a binary artifact" {
            pkgForBinaryArtifact.copy(downloadLocation = SpdxConstants.NONE).getBinaryArtifact() should beNull()
            pkgForBinaryArtifact.copy(downloadLocation = SpdxConstants.NOASSERTION).getBinaryArtifact() should beNull()
            pkgForSourceArtifact.getBinaryArtifact() should beNull()
            pkgForVcs.getBinaryArtifact() should beNull()
        }
    }

    "getSourceArtifact()" should {
        "return a RemoteArtifact for a downloadLocation that points to a source artifact" {
            pkgForSourceArtifact.getSourceArtifact() shouldBe RemoteArtifact(
                url = "http://ftp.gnu.org/gnu/glibc/glibc-ports-2.15.tar.gz",
                hash = Hash.NONE
            )
        }

        "return null for a downloadLocation that does not point to a source artifact" {
            pkgForSourceArtifact.copy(downloadLocation = SpdxConstants.NONE).getSourceArtifact() should beNull()
            pkgForSourceArtifact.copy(downloadLocation = SpdxConstants.NOASSERTION).getSourceArtifact() should beNull()
            pkgForBinaryArtifact.getSourceArtifact() should beNull()
            pkgForVcs.getSourceArtifact() should beNull()
        }
    }

    "getVcsInfo()" should {
        "return the VcsInfo for a downloadLocation that points to a VCS" {
            pkgForVcs.getVcsInfo() shouldBe VcsInfo(
                type = VcsType.GIT,
                url = "https://git.myproject.org/MyProject.git",
                revision = "master",
                path = "/src/MyClass.cpp"
            )
        }

        "return null for a downloadLocation that does not point to a VCS" {
            pkgForVcs.copy(downloadLocation = SpdxConstants.NONE).getVcsInfo() should beNull()
            pkgForVcs.copy(downloadLocation = SpdxConstants.NOASSERTION).getVcsInfo() should beNull()
            pkgForBinaryArtifact.getVcsInfo() should beNull()
            pkgForSourceArtifact.getVcsInfo() should beNull()
        }
    }

    "projectPackage()" should {
        "return project package when list of packages is given" {
            val spdxDocument = createSpdxDocument(null, false)
            val projectPackage = spdxDocument.packages.find { it.spdxId == "SPDXRef-Package-xyz" }

            spdxDocument.projectPackage() shouldBe projectPackage
        }

        "return project package when only one package in list, but external document references exist" {
            val projectPackage = createSpdxDocument().packages.find { it.spdxId == "SPDXRef-Package-xyz" }
            val spdxDocument = createSpdxDocument(listOf(projectPackage!!), true)

            spdxDocument.projectPackage() shouldBe projectPackage
        }

        "return no project package when just one package in list" {
            val projectPackage = createSpdxDocument().packages.find { it.spdxId == "SPDXRef-Package-xyz" }
            val spdxDocument = createSpdxDocument(listOf(projectPackage!!), false)

            spdxDocument.projectPackage() shouldBe null
        }
    }

    "getSpdxPackage()" should {
        val spdxProjectFile = File("src/funTest/assets/projects/synthetic/spdx/project/project.spdx.yml")

        "throw if an external package id does not match relationship id" {
            val externalDocumentReference = SpdxExternalDocumentReference(
                "DocumentRef-zlib-1.2.11",
                SpdxChecksum(SpdxChecksum.Algorithm.SHA1, "9ffefb62ce14b40f675345a05b45846900740689"),
                "../package/libs/zlib/package.spdx.yml"
            )
            val issues = mutableListOf<OrtIssue>()

            externalDocumentReference.getSpdxPackage("SPDXRef-Package_wrong_id", spdxProjectFile, issues)

            issues shouldHaveSize 1
            issues shouldHaveSingleElement { externalDocumentReference.externalDocumentId in it.message }
        }

        "return the correct SPDX package" {
            val externalDocumentReference = SpdxExternalDocumentReference(
                "DocumentRef-zlib-1.2.11",
                SpdxChecksum(SpdxChecksum.Algorithm.SHA1, "9ffefb62ce14b40f675345a05b45846900740689"),
                "../package/libs/zlib/package.spdx.yml"
            )

            val spdxPackageId = "SPDXRef-Package-zlib"
            val spdxPackage = externalDocumentReference.getSpdxPackage(spdxPackageId, spdxProjectFile, mutableListOf())

            spdxPackage shouldNotBeNull {
                spdxId shouldBe spdxPackageId
            }
        }
    }
})

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

package org.ossreviewtoolkit.utils.core

import java.io.File

import org.ossreviewtoolkit.utils.common.Os
import org.ossreviewtoolkit.utils.common.toSafeUri
import org.ossreviewtoolkit.utils.common.toUri
import org.ossreviewtoolkit.utils.common.withoutPrefix
import org.ossreviewtoolkit.utils.common.withoutSuffix

private val mavenCentralUrlPattern = Regex("^https?://repo1?\\.maven(\\.apache)?\\.org(/.*)?$")

/**
 * The directory to store ORT (read-only) configuration in.
 */
val ortConfigDirectory by lazy {
    Os.env[ORT_CONFIG_DIR_ENV_NAME]?.takeUnless {
        it.isEmpty()
    }?.let {
        File(it)
    } ?: ortDataDirectory.resolve("config")
}

/**
 * The directory to store ORT (read-write) tools in.
 */
val ortToolsDirectory by lazy {
    Os.env[ORT_TOOLS_DIR_ENV_NAME]?.takeUnless {
        it.isEmpty()
    }?.let {
        File(it)
    } ?: ortDataDirectory.resolve("tools")
}

/**
 * The directory to store ORT (read-write) data in, like caches and archives.
 */
val ortDataDirectory by lazy {
    Os.env[ORT_DATA_DIR_ENV_NAME]?.takeUnless {
        it.isEmpty()
    }?.let {
        File(it)
    } ?: Os.userHomeDirectory.resolve(".ort")
}

/**
 * Global variable that gets toggled by a command line parameter parsed in the main entry points of the modules.
 */
var printStackTrace = false

/**
 * Filter a list of [names] to include only those that likely belong to the given [version] of an optional [project].
 */
fun filterVersionNames(version: String, names: List<String>, project: String? = null): List<String> {
    if (version.isBlank() || names.isEmpty()) return emptyList()

    // If there are full matches, return them right away.
    val fullMatches = names.filter { it.equals(version, ignoreCase = true) }
    if (fullMatches.isNotEmpty()) return fullMatches

    // The list of supported version separators.
    val versionSeparators = listOf('-', '_', '.')
    val versionHasSeparator = versionSeparators.any { version.contains(it) }

    // Create variants of the version string to recognize.
    data class VersionVariant(val name: String, val separators: List<Char>)

    val versionLower = version.lowercase()
    val versionVariants = mutableListOf(VersionVariant(versionLower, versionSeparators))

    val separatorRegex = Regex(versionSeparators.joinToString("", "[", "]"))
    versionSeparators.mapTo(versionVariants) {
        VersionVariant(versionLower.replace(separatorRegex, it.toString()), listOf(it))
    }

    val filteredNames = names.filter {
        val name = it.lowercase()

        versionVariants.any { versionVariant ->
            // Allow to ignore suffixes in names that are separated by something else than the current separator, e.g.
            // for version "3.3.1" accept "3.3.1-npm-packages" but not "3.3.1.0".
            val hasIgnorableSuffix = name.withoutPrefix(versionVariant.name)?.let { tail ->
                tail.firstOrNull() !in versionVariant.separators
            } ?: false

            // Allow to ignore prefixes in names that are separated by something else than the current separator, e.g.
            // for version "0.10" accept "docutils-0.10" but not "1.0.10".
            val hasIgnorablePrefix = name.withoutSuffix(versionVariant.name)?.let { head ->
                val last = head.lastOrNull()
                val forelast = head.dropLast(1).lastOrNull()

                val currentSeparators = if (versionHasSeparator) versionVariant.separators else versionSeparators

                // Full match with the current version variant.
                last == null
                        // The prefix does not end with the current separators or a digit.
                        || (last !in currentSeparators && !last.isDigit())
                        // The prefix ends with the current separators but the forelast character is not a digit.
                        || (last in currentSeparators && (forelast == null || !forelast.isDigit()))
                        // The prefix ends with 'v' and the forelast character is a separator.
                        || (last == 'v' && (forelast == null || forelast in currentSeparators))
            } ?: false

            hasIgnorableSuffix || hasIgnorablePrefix
        }
    }

    return filteredNames.filter {
        // startsWith("") returns "true" for any string, so we get an unfiltered list if "project" is "null".
        it.startsWith(project.orEmpty())
    }.let {
        // Fall back to the original list if filtering by project results in an empty list.
        it.ifEmpty { filteredNames }
    }
}

/**
 * Install both the [OrtAuthenticator] and the [OrtProxySelector] to handle proxy authentication. Return the
 * [OrtProxySelector] instance for further configuration.
 */
fun installAuthenticatorAndProxySelector(): OrtProxySelector {
    OrtAuthenticator.install()
    return OrtProxySelector.install()
}

/**
 * Return whether the given [url] points to Maven Central or not.
 */
fun isMavenCentralUrl(url: String) = url.matches(mavenCentralUrlPattern)

/**
 * Normalize a string representing a [VCS URL][vcsUrl] to a common string form.
 */
fun normalizeVcsUrl(vcsUrl: String): String {
    var url = vcsUrl.trim().trimEnd('/')

    if (url.startsWith(":pserver:") || url.startsWith(":ext:")) {
        // Do not touch CVS URLs for now.
        return url
    }

    // URLs to Git repos may omit the scheme and use an scp-like URL that uses ":" to separate the host from the path,
    // see https://git-scm.com/docs/git-clone#_git_urls_a_id_urls_a. Make this an explicit ssh URL so it can be parsed
    // by Java's URI class.
    url = url.replace(Regex("^(.*)([a-zA-Z]+):([a-zA-Z]+)(.*)$")) {
        val tail = "${it.groupValues[1]}${it.groupValues[2]}/${it.groupValues[3]}${it.groupValues[4]}"
        if ("://" in url) tail else "ssh://$tail"
    }

    // Fixup scp-like Git URLs that do not use a ':' after the server part.
    if (url.startsWith("git@")) {
        url = "ssh://$url"
    }

    // Drop any non-SVN VCS name with "+" from the scheme.
    if (!url.startsWith("svn+")) {
        url = url.replace(Regex("^(.+)\\+(.+)(://.+)$")) {
            // Use the string to the right of "+" which should be the protocol.
            "${it.groupValues[2]}${it.groupValues[3]}"
        }
    }

    // If we have no protocol by now and the host is Git-specific, assume https.
    if (url.startsWith("github.com") || url.startsWith("gitlab.com")) {
        url = "https://$url"
    }

    // A hierarchical URI looks like
    //     [scheme:][//authority][path][?query][#fragment]
    // where a server-based "authority" has the syntax
    //     [user-info@]host[:port]
    val uri = url.toUri().getOrNull()

    if (uri == null || (uri.scheme == null && uri.path.isNotEmpty())) {
        // Fall back to a file if the URL is a Windows or Linux path.
        return File(url).toSafeUri().toString()
    }

    // Handle host-specific normalizations.
    if (uri.host != null) {
        when {
            uri.host.endsWith("github.com") || uri.host.endsWith("gitlab.com") -> {
                // Ensure the path to a repository ends with ".git".
                val path = uri.path.takeIf { path ->
                    path.endsWith(".git") || path.count { it == '/' } != 2
                } ?: "${uri.path}.git"

                return if (uri.scheme == "ssh") {
                    // Ensure the generic "git" user name is specified.
                    val host = uri.authority.let { if (it.startsWith("git@")) it else "git@$it" }
                    "ssh://$host$path"
                } else {
                    // Remove any user name and "www" prefix.
                    val host = uri.authority.substringAfter('@').removePrefix("www.")
                    "https://$host$path"
                }
            }
        }
    }

    return url
}

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

@file:Suppress("TooManyFunctions")

package org.ossreviewtoolkit.utils.spdx

import java.util.EnumSet

import org.ossreviewtoolkit.utils.spdx.SpdxExpression.Strictness

/**
 * Return the duplicates as identified by [keySelector] of a collection.
 */
fun <T, K> Collection<T>.getDuplicates(keySelector: (T) -> K): Set<K> =
    if (this is Set) emptySet() else groupBy(keySelector).filter { it.value.size > 1 }.keys

/**
 * Return the duplicates of a collection.
 */
fun <T> Collection<T>.getDuplicates(): Set<T> = getDuplicates { it }

/**
 * Return an [EnumSet] that contains the elements of [this] and [other].
 */
operator fun <E : Enum<E>> EnumSet<E>.plus(other: EnumSet<E>): EnumSet<E> = EnumSet.copyOf(this).apply { addAll(other) }

/**
 * Create an [SpdxExpression] by concatenating [this][SpdxLicense] and [other] using [SpdxOperator.AND].
 */
infix fun SpdxLicense.and(other: SpdxLicense) = this and other.toExpression()

/**
 * Create an [SpdxExpression] by concatenating [this][SpdxLicense] and [other] using [SpdxOperator.AND].
 */
infix fun SpdxLicense.and(other: SpdxExpression) =
    SpdxCompoundExpression(toExpression(), SpdxOperator.AND, other)

/**
 * Create an [SpdxExpression] by concatenating [this][SpdxLicense] and [other] using [SpdxOperator.OR].
 */
infix fun SpdxLicense.or(other: SpdxLicense) = this or other.toExpression()

/**
 * Create an [SpdxExpression] by concatenating [this][SpdxLicense] and [other] using [SpdxOperator.OR].
 */
infix fun SpdxLicense.or(other: SpdxExpression) =
    SpdxCompoundExpression(toExpression(), SpdxOperator.OR, other)

/**
 * Create an [SpdxExpression] by concatenating [this][SpdxLicense] and [exception] using [SpdxExpression.WITH].
 */
infix fun SpdxLicense.with(exception: SpdxLicenseException) =
    SpdxLicenseWithExceptionExpression(toExpression(), exception.id)

/**
 * Create an [SpdxLicenseIdExpression] from this [SpdxLicense].
 */
fun SpdxLicense.toExpression(): SpdxLicenseIdExpression {
    var expressionId = id

    // While in the current SPDX standard the "or later version" semantic is part of the id string itself, it is a
    // generic "+" operator for deprecated licenses.
    val orLaterVersion = if (deprecated) {
        expressionId = id.removeSuffix("+")
        id != expressionId
    } else {
        id.endsWith("-or-later")
    }

    return SpdxLicenseIdExpression(expressionId, orLaterVersion)
}

/**
 * Return true if and only if this String can be successfully parsed to a [SpdxExpression].
 */
fun String.isSpdxExpression(): Boolean =
    runCatching { SpdxExpression.parse(this, Strictness.ALLOW_DEPRECATED) }.isSuccess

/**
 * Return true if and only if this String can be successfully parsed to an [SpdxExpression] or if it equals
 * [SpdxConstants.NONE] or [SpdxConstants.NOASSERTION].
 */
fun String.isSpdxExpressionOrNotPresent(): Boolean =
    SpdxConstants.isNotPresent(this) || isSpdxExpression()

/**
 * Parses the string as an [SpdxExpression] and returns the result.
 * @throws SpdxException if the string is not a valid representation of an SPDX expression.
 */
fun String.toSpdx(strictness: Strictness = Strictness.ALLOW_ANY): SpdxExpression =
    SpdxExpression.parse(this, strictness)

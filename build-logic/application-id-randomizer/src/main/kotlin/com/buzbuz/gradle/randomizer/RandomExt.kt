/*
 * Copyright (C) 2025 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.gradle.randomizer

import kotlin.random.Random


internal fun Random.nextApplicationId(): String {
    val firstPart = nextString(length = 3)
    val secondPartLength = nextString(length = nextInt(3, 15))
    val thirdPartLength = nextString(length = nextInt(3, 15))

    return "$firstPart.$secondPartLength.$thirdPartLength"
}

internal fun Random.nextString(length: Int): String {
    var result = ""
    repeat(length) {
        result += nextChar()
    }
    return result
}

private fun Random.nextChar(): Char =
    nextInt(
        from = CHAR_CODE_A_LOWERCASE,
        until = CHAR_CODE_A_LOWERCASE + ALPHABET_SIZE,
    ).toChar()

private const val CHAR_CODE_A_LOWERCASE = 97
private const val ALPHABET_SIZE = 26
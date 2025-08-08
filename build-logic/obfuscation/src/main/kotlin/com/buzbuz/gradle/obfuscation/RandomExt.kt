
package com.buzbuz.gradle.obfuscation

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
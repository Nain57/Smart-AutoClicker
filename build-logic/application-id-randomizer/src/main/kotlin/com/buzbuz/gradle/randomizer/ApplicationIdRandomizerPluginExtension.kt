/*
 * Copyright (C) 2024 Kevin Buzeau
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

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra
import kotlin.random.Random


abstract class ApplicationIdRandomizerPluginExtension {

    internal lateinit var project: Project

    val applicationId: String
        get() = project.rootProject.extra.get(EXTRA_APPLICATION_ID) as String

    fun generateApplicationId(shouldRandomize: Boolean, regularApplicationId: String) {
        val appId =
            if (shouldRandomize) generateRandomAppId()
            else regularApplicationId

        project.rootProject.extra.set(EXTRA_APPLICATION_ID, appId)
    }

    private fun generateRandomAppId(): String =
        Random(System.currentTimeMillis()).let { random ->
            val firstPart = random.nextString(length = 3)
            val secondPartLength = random.nextString(length = random.nextInt(1, 15))
            val thirdPartLength = random.nextString(length = random.nextInt(1, 15))

            "$firstPart.$secondPartLength.$thirdPartLength"
        }

    private fun Random.nextString(length: Int): String {
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


    private companion object {
        private const val EXTRA_APPLICATION_ID = "randomizer_applicationId"
        private const val CHAR_CODE_A_LOWERCASE = 97
        private const val ALPHABET_SIZE = 26
    }
}


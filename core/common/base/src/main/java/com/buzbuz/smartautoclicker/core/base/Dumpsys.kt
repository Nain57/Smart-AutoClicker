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
package com.buzbuz.smartautoclicker.core.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.PrintWriter

/**
 * Defines a class as "dumpable".
 * This interface is a simple standardization of the dump of a class for the adb command "dumpsys".
 */
interface Dumpable {

    companion object {
        /** A tabulation for indentation in the application dumpsys. */
        const val DUMP_DISPLAY_TAB = "  "
    }

    /**
     * Dump the class in the given PrintWriter.
     * The [writer] argument must be the one provided by the dump method of an Android component (such as Activity,
     * Service...). All content printed in the [writer] must be prefixed with the [prefix] argument in order to ensure
     * correct display with the "dumpsys" adb command.
     */
    fun dump(writer: PrintWriter, prefix: CharSequence = "")
}

fun <T> Flow<T>.dumpWithTimeout(timeoutMs: Long = 20): T? =
    runBlocking { withTimeoutOrNull(timeoutMs) { firstOrNull() } }

fun CharSequence.addDumpTabulationLvl(): CharSequence =
    "${this}${Dumpable.DUMP_DISPLAY_TAB}"
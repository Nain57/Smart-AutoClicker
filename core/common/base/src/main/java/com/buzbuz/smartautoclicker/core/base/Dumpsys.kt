
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
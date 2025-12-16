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
package com.buzbuz.smartautoclicker.core.smart.debugging.data

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.extensions.safeBufferedOutputStream
import com.buzbuz.smartautoclicker.core.base.extensions.safeInputStream
import com.buzbuz.smartautoclicker.core.base.extensions.safeRecreate
import com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping.toDomain
import com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping.toProtobuf
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.google.protobuf.MessageLite

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportOverview as ProtoDebugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.DebugReportMessage as ProtoDebugReportMessage


/** Handle access to debug report files. */
@Singleton
internal class DebugReportLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val overviewFile: File = File(context.cacheDir, DEBUG_REPORT_OVERVIEW_FILE_NAME)
    private val messagesFile: File = File(context.cacheDir, DEBUG_REPORT_MESSAGES_FILE_NAME)
    private val filesMutex: Mutex = Mutex()

    private var messagesOutputStream: OutputStream? = null
    private val isWritingReport: Boolean
        get() = messagesOutputStream != null

    /**
     * Start the report writing process.
     * Required in order to use [writeEventOccurrenceToReport], can be stopped using [stopReportWrite].
     * While writing, you cannot read the report content via [readOverview] or [readMessages].
     */
    suspend fun startReportWrite() {
        filesMutex.withLock {
            Log.i(LOG_TAG, "Start debug report writing")

            // Check if the previous report is correctly closed and close it if needed
            if (messagesOutputStream != null) {
                Log.w(LOG_TAG, "Previous debug report was not finished")
                messagesOutputStream?.close()
                messagesOutputStream = null
            }

            // Removes any previous report files and recreate them
            messagesFile.safeRecreate()
            overviewFile.safeRecreate()

            // Open the file
            messagesOutputStream = messagesFile.safeBufferedOutputStream()
        }
    }

    /**
     * Write an event occurrence to the report.
     * The writing needs to be started with [startReportWrite] first.
     *
     * @param occurrence The occurrence to write in the report file.
     */
    suspend fun writeEventOccurrenceToReport(occurrence: DebugReportEventOccurrence) {
        filesMutex.withLock {
            messagesOutputStream?.safeWriteDelimited(occurrence.toProtobuf())
        }
    }

    /**
     * Stop the report writing.
     * The writing needs to be started with [startReportWrite] first
     *
     * @param overview the overview for the current report.
     */
    suspend fun stopReportWrite(overview: DebugReportOverview) {
        filesMutex.withLock {
            Log.i(LOG_TAG, "Stop debug report writing")

            // We only write on this file here, no need to keep the output stream
            overviewFile.safeBufferedOutputStream()?.use { outputStream ->
                outputStream.safeWriteDelimited(overview.toProtobuf())
            }

            // We no longer will be receiving any messages for this detection session, close the output stream
            messagesOutputStream?.safeClose()
            messagesOutputStream = null
        }
    }

    /**
     * Get the last detection session debug report overview.
     *
     * @return the debug report overview, if available. Writing needs to be stopped or this value will always be null.
     */
    suspend fun readOverview(): DebugReportOverview? =
        filesMutex.withLock {
            if (isWritingReport) return null

            overviewFile.safeInputStream()?.use { inputStream ->
                return inputStream.safeParseDebugReportOverview()?.toDomain()
            }

            return null
        }

    /**
     * Get the last detection session debug report event occurrences.
     *
     * @return the debug report event occurrences, if available. Writing needs to be stopped or this list will always
     * be empty.
     */
    suspend fun readMessages(): List<DebugReportEventOccurrence> =
        buildList {
            filesMutex.withLock {
                if (isWritingReport) return@buildList

                messagesFile.safeInputStream()?.use { inputStream ->
                    var eventOccurrence: DebugReportEventOccurrence?
                    while (true) {
                        eventOccurrence = inputStream.safeParseDebugReportMessage()?.toDomain() ?: break
                        add(eventOccurrence)
                        yield() // Allow loop stop upon coroutine cancellation
                    }
                }
            }
        }

    private fun OutputStream.safeWriteDelimited(message: MessageLite) {
        try {
            message.writeDelimitedTo(this)
            flush()
        } catch (ioEx: IOException) {
            Log.e(LOG_TAG, "Cannot write to file, IOException", ioEx)
        }
    }

    private fun OutputStream.safeClose() {
        try {
            close()
        } catch (ioEx: IOException) {
            Log.e(LOG_TAG, "Cannot close file, IOException", ioEx)
        }
    }

    private fun InputStream.safeParseDebugReportOverview(): ProtoDebugReportOverview? {
        try {
            return ProtoDebugReportOverview.parseDelimitedFrom(this)
        } catch (ioEx: IOException) {
            Log.e(LOG_TAG, "Cannot read DebugReportOverview from file, IOException", ioEx)
            return null
        }
    }

    private fun InputStream.safeParseDebugReportMessage(): ProtoDebugReportMessage? {
        try {
            return ProtoDebugReportMessage.parseDelimitedFrom(this)
        } catch (ioEx: IOException) {
            Log.e(LOG_TAG, "Cannot read DebugReportMessage from file, IOException", ioEx)
            return null
        }
    }
}

private const val DEBUG_REPORT_MESSAGES_FILE_NAME = "DebugReportMessages.pb"
private const val DEBUG_REPORT_OVERVIEW_FILE_NAME = "DebugReportOverview.pb"
private const val LOG_TAG = "DebugReportFileAccess"
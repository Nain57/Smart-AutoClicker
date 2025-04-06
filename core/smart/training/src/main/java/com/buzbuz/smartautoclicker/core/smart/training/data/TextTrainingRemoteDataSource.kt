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
package com.buzbuz.smartautoclicker.core.smart.training.data

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class TextTrainingRemoteDataSource @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _currentDownloadState: MutableStateFlow<TextTrainingFileDownload?> = MutableStateFlow(null)
    val currentDownloadState: Flow<TextTrainingFileDownload?> = _currentDownloadState

    fun downloadTrainingDataFile(language: TrainedTextLanguage, outputFile: File, onSucces: () -> Unit) {
        coroutineScopeIo.launch {
            val currentState = _currentDownloadState.value
            if (currentState is TextTrainingFileDownload.Downloading) {
                Log.w(TAG, "Can't download data for $language, other language file is downloading")
                return@launch
            }

            _currentDownloadState.update { TextTrainingFileDownload.Downloading(language, 0) }

            Log.i(TAG, "Downloading data for language $language")

            var connection: HttpURLConnection? = null
            var input: InputStream? = null
            var output: FileOutputStream? = null

            try {
                connection = language.getTrainedDataUrl().openConnection() as HttpURLConnection
                connection.connect()

                val contentLength = connection.contentLength
                if (connection.responseCode != HttpURLConnection.HTTP_OK || contentLength <= 0) {
                    Log.e(TAG, "Error, while downloading language file $language")
                    _currentDownloadState.update { TextTrainingFileDownload.Error(language) }
                    return@launch
                }

                input = connection.inputStream
                output = FileOutputStream(outputFile)

                val buffer = ByteArray(8 * 1024)
                var totalBytesRead = 0L
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    _currentDownloadState.update {
                        TextTrainingFileDownload.Downloading(
                            language = language,
                            progress = (totalBytesRead * 100 / contentLength).toInt(),
                        )
                    }
                }

                _currentDownloadState.update { TextTrainingFileDownload.Completed(language) }
            } catch (ex: Exception) {
                Log.e(TAG, "Error, while downloading language file $language", ex)
                _currentDownloadState.update { TextTrainingFileDownload.Error(language) }
            } finally {
                try {
                    input?.close()
                    output?.close()
                    connection?.disconnect()
                } catch (ignored: Exception) { }
            }
        }
    }
}


internal sealed class TextTrainingFileDownload {

    abstract val language: TrainedTextLanguage

    data class Downloading(override val language: TrainedTextLanguage, val progress: Int) : TextTrainingFileDownload()
    data class Completed(override val language: TrainedTextLanguage) : TextTrainingFileDownload()
    data class Error(override val language: TrainedTextLanguage) : TextTrainingFileDownload()
}

private const val TAG = "TextTrainingRemoteDataSource"
/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DirectDownloadModelRemoteDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
): RecognitionModelsRemoteDataSource {

    private val _currentlyDownloading: MutableStateFlow<Map<OCRAlphabet, Int>> = MutableStateFlow(emptyMap())
    override val currentlyDownloading: StateFlow<Map<OCRAlphabet, Int>> = _currentlyDownloading

    private val recognitionModelsDataDir: File = context.recognitionModelsDataDir()


    override suspend fun downloadRecognitionModel(alphabet: OCRAlphabet, onSuccess: () -> Unit) {
        if (currentlyDownloading.value.contains(alphabet)) {
            Log.w(TAG, "Ignore download request for $alphabet model")
            return
        }
        _currentlyDownloading.update { old -> old + (alphabet to 0) }

        withContext(ioDispatcher) {
            val destDir = recognitionModelsDataDir.recognitionModelDataDir(alphabet)
            val zipFile = File(destDir, "${alphabet.toRecognitionModelDataDirName()}_model.zip")

            try {
                // Download
                destDir.mkdirs()
                val downloadSuccess = downloadFile(
                    url = alphabet.getRecognitionModelUrl(),
                    dest = zipFile,
                    onProgress = { progress ->
                        _currentlyDownloading.update { old -> old + (alphabet to (progress * 0.85).toInt()) }
                    }
                )
                if (!downloadSuccess) return@withContext

                // Extract
                val extractSuccess = unzip(
                    zipFile = zipFile,
                    destDir = destDir,
                    onProgress = { progress ->
                        _currentlyDownloading.update { old -> old + (alphabet to (85 + progress * 0.15).toInt()) }
                    }
                )
                if (!extractSuccess) return@withContext

                onSuccess()
                _currentlyDownloading.update { old -> old - alphabet }
            } catch (ex: Exception) {
                Log.e(TAG, "Can't download recognition model for alphabet $alphabet", ex)
            } finally {
                zipFile.delete()
            }
        }

        _currentlyDownloading.update { old -> old - alphabet }
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Int) -> Unit): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            val totalBytes = connection.contentLengthLong
            var bytesRead = 0L
            connection.inputStream.use { input ->
                dest.outputStream().use { output ->

                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = input.read(buffer)

                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes
                        if (totalBytes > 0) onProgress((bytesRead * 100 / totalBytes).toInt())
                        bytes = input.read(buffer)
                    }
                }
            }

            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while downloading models file", ex)
            return false
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(zipFile: File, destDir: File, onProgress: (Int) -> Unit): Boolean {
        try {
            val totalEntries = ZipFile(zipFile).size()
            var processed = 0

            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {

                    if (!entry.isDirectory) {
                        // Strip any leading directory from the zip entry — we only want the file name
                        val outFile = File(destDir, File(entry.name).name)
                        outFile.outputStream().use { zis.copyTo(it) }
                    }

                    processed++
                    onProgress(processed * 100 / totalEntries)

                    zis.closeEntry()
                    entry = zis.nextEntry
                }

                return true
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error while installing models file", ex)
            return false
        }
    }
}

private fun OCRAlphabet.getRecognitionModelUrl(): String =
    "${GITHUB_BASE_URL}${name.lowercase()}.zip"

private const val GITHUB_BASE_URL = "https://github.com/Nain57/Smart-AutoClicker/releases/download/test-recognition-models/"
private const val TAG = "DirectDownloadModelRemoteDataSource"

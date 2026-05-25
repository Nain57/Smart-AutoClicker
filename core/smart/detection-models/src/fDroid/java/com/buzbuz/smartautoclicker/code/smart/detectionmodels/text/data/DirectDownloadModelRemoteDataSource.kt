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

import android.util.Log
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DirectDownloadModelRemoteDataSource @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
): RecognitionModelsRemoteDataSource {

    private val _currentlyDownloading: MutableStateFlow<Set<OCRAlphabet>> = MutableStateFlow(emptySet())
    override val currentlyDownloading: StateFlow<Set<OCRAlphabet>> = _currentlyDownloading

    override suspend fun downloadRecognitionModel(alphabet: OCRAlphabet, closure: (InputStream) -> Unit) {
        downloadAndSave("${GITHUB_BASE_URL}${alphabet.name.lowercase()}.zip", alphabet, closure)
    }

    private suspend fun downloadAndSave(url: String, alphabet: OCRAlphabet, closure: (InputStream) -> Unit) {
        withContext(ioDispatcher) {
            _currentlyDownloading.update { old -> old + alphabet }

            try {
                val connection = URL(url).openConnection()
                connection.connect()
                connection.getInputStream().use { input -> closure(input) }
            } catch (ex: Exception) {
                Log.w(TAG, "Error while downloading model for $alphabet", ex)
            } finally {
                _currentlyDownloading.update { old -> old - alphabet }
            }
        }
    }
}

private const val GITHUB_BASE_URL = "https://github.com/Nain57/Smart-AutoClicker/releases/download/test-recognition-models/"
private const val TAG = "DirectDownloadModelRemoteDataSource"

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
 * along with this program.  See <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.code.smart.detectionmodels.text

import android.util.Log
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data.OCRModelLocalDataSource
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.data.RecognitionModelsRemoteDataSource
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModel
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRModelState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OCRModelsRepositoryImpl @Inject constructor(
    private val localDataSource: OCRModelLocalDataSource,
    private val remoteDataSource: RecognitionModelsRemoteDataSource,
) : OCRModelsRepository {

    override val recognitionModels: Flow<Set<OCRModel.Recognition>> = localDataSource.recognitionModelsFiles
        .combine(remoteDataSource.currentlyDownloading) { installed, downloading ->
            installed.toModel(downloading)
        }

    override fun refreshOcrModels() {
        localDataSource.refreshModelsFiles()
    }

    override suspend fun getDetectionModel(): OCRModel.Detection? {
        if (!localDataSource.isDetectionModelAvailable()) {
            Log.e(TAG, "Can't get multilingual detection model")
            return null
        }

        return OCRModel.Detection(
            state = OCRModelState.Installed(path = localDataSource.getDetectionModelDir().path),
        )
    }

    override suspend fun getRecognitionModel(alphabet: OCRAlphabet): OCRModel.Recognition {
        if (!localDataSource.isRecognitionModelAvailable(alphabet)) {
            Log.e(TAG, "Can't get model $alphabet, it is not downloaded.")
            return OCRModel.Recognition(alphabet = alphabet, state = OCRModelState.Downloadable)
        }

        if (remoteDataSource.currentlyDownloading.value.contains(alphabet)) {
            Log.w(TAG, "Can't get model $alphabet, it is downloading")
            return OCRModel.Recognition(alphabet = alphabet, state = OCRModelState.Downloading)
        }

        return OCRModel.Recognition(
            alphabet = alphabet,
            state = OCRModelState.Installed(path = localDataSource.getRecognitionModelDir(alphabet).path),
        )
    }

    override suspend fun getRecognitionModelPath(alphabet: OCRAlphabet): String? {
        if (!localDataSource.isRecognitionModelAvailable(alphabet)) {
            Log.e(TAG, "Can't get model path for $alphabet, it is not downloaded.")
            return null
        }

        return localDataSource.getRecognitionModelDir(alphabet).path
    }

    override suspend fun downloadRecognitionModel(alphabet: OCRAlphabet) {
        remoteDataSource.downloadRecognitionModel(alphabet) { archiveStream ->
            localDataSource.saveAndExtractModel(alphabet, archiveStream)
        }
    }

    private fun Map<OCRAlphabet, String>.toModel(downloading: Set<OCRAlphabet>): Set<OCRModel.Recognition> =
        OCRAlphabet.entries.map { alphabet ->
            val path = get(alphabet)
            val state = when {
                downloading.contains(alphabet) -> OCRModelState.Downloading
                path != null -> OCRModelState.Installed(path)
                else -> OCRModelState.Downloadable
            }

            OCRModel.Recognition(
                alphabet = alphabet,
                state = state,
            )
        }.toSet()
}

private const val TAG = "TextRecognitionModelsRepository"

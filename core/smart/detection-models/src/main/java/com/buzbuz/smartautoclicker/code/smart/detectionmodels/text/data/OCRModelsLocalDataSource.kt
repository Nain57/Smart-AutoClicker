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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OCRModelLocalDataSource @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext context: Context,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val refresh: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

    private val detectionModelDataDir: File = context.detectionModelDataDir()
    private val recognitionModelsDataDir: File = context.recognitionModelsDataDir()

    val recognitionModelsFiles: Flow<Map<OCRAlphabet, String>> = refresh
        .onStart { emit(Unit) }
        .map { listRecognitionModels() }
        .flowOn(ioDispatcher)


    init {
        coroutineScopeIo.launch {
            ensureDefaultModelsExtracted(context)
        }
    }

    fun refreshModelsFiles() {
        coroutineScopeIo.launch {
            Log.i(TAG, "Refreshing text model folder content")
            refresh.emit(Unit)
        }
    }

    fun getDetectionModelDir(): File =
        detectionModelDataDir

    fun isDetectionModelAvailable(): Boolean {
        return File(detectionModelDataDir, OCR_DETECTION_MODEL_FILE).exists() &&
                File(detectionModelDataDir, OCR_DETECTION_MODEL_PARAMS_FILE).exists()
    }

    fun getRecognitionModelDir(alphabet: OCRAlphabet): File =
        recognitionModelsDataDir.recognitionModelDataDir(alphabet)

    fun isRecognitionModelAvailable(alphabet: OCRAlphabet): Boolean {
        val alphabetDir = getRecognitionModelDir(alphabet)
        return File(alphabetDir, OCR_RECOGNITION_MODEL_FILE).exists() &&
                File(alphabetDir, OCR_RECOGNITION_MODEL_PARAMS_FILE).exists() &&
                File(alphabetDir, OCR_RECOGNITION_MODEL_DICTIONARY_FILE).exists()
    }

    /** Ensure that models bundled in the APK assets are extracted to the local storage. */
    private fun ensureDefaultModelsExtracted(context: Context) {
        if (!isDetectionModelAvailable()) {
            context.extractAssetModel(
                assetDir = "$OCR_MODELS_ASSET_DIR/$OCR_DETECTION_MODEL_DIR",
                targetDir = getDetectionModelDir(),
            )
        }
        if (!isRecognitionModelAvailable(OCRAlphabet.LATIN)) {
            context.extractAssetModel(
                assetDir = "$OCR_MODELS_ASSET_DIR/$OCR_RECOGNITION_MODEL_DIR/${OCRAlphabet.LATIN.toRecognitionModelDataDirName()}",
                targetDir = getRecognitionModelDir(OCRAlphabet.LATIN),
            )
        }
    }

    private fun listRecognitionModels(): Map<OCRAlphabet, String> {
        if (!recognitionModelsDataDir.exists()) return emptyMap()

        return buildMap {
            recognitionModelsDataDir.listFiles()?.forEach { file ->
                if (!file.isDirectory) return@forEach
                try {
                    val alphabet = OCRAlphabet.valueOf(file.name.uppercase())
                    if (isRecognitionModelAvailable(alphabet)) {
                        Log.d(TAG, "Recognition model found: $alphabet ${file.path}")
                        put(alphabet, file.path)
                    }
                } catch (_ : IllegalArgumentException) {}
            }
        }
    }
}

private fun Context.extractAssetModel(assetDir: String, targetDir: File) {
    Log.d(TAG, "Extracting OCR model from asset dir $assetDir to $targetDir")
    targetDir.mkdirs()
    try {
        assets.list(assetDir)?.forEach { filename ->
            assets.open("$assetDir/$filename").use { input ->
                Log.d(TAG, "Extracting $assetDir/$filename")
                File(targetDir, filename).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to extract asset model $assetDir", e)
        targetDir.deleteRecursively()
    }
}

private const val TAG = "OCRModelsLocalDataSource"

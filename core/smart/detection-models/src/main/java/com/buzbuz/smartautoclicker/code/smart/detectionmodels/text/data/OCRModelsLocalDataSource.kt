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
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OCRModelLocalDataSource @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext context: Context,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val textModelDataDir: File = File(context.filesDir, OCR_MODELS_DATA_DIR)
    private val refresh: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

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

    /**
     * Get the directory for the detection model.
     * @return the directory.
     */
    fun getDetectionModelDir(): File =
        File(textModelDataDir, OCR_DETECTION_MODEL_DIR)

    /**
     * Check if the detection model is available.
     * @return true if the detection model files are present, false otherwise.
     */
    fun isDetectionModelAvailable(): Boolean {
        val detDir = getDetectionModelDir()
        return File(detDir, OCR_DETECTION_MODEL_FILE).exists() &&
                File(detDir, OCR_DETECTION_MODEL_PARAMS_FILE).exists()
    }

    /**
     * Get the directory for an alphabet model.
     * @param alphabet the alphabet.
     * @return the directory.
     */
    fun getRecognitionModelDir(alphabet: OCRAlphabet): File =
        File(textModelDataDir, alphabet.recognitionModelDataDir())

    /**
     * Check if a model is already downloaded and extracted for an alphabet.
     * @param alphabet the alphabet to check.
     * @return true if the model files are present, false otherwise.
     */
    fun isRecognitionModelAvailable(alphabet: OCRAlphabet): Boolean {
        val alphabetDir = getRecognitionModelDir(alphabet)
        return File(alphabetDir, OCR_RECOGNITION_MODEL_FILE).exists() &&
                File(alphabetDir, OCR_RECOGNITION_MODEL_PARAMS_FILE).exists() &&
                File(alphabetDir, OCR_RECOGNITION_MODEL_DICTIONARY_FILE).exists()
    }

    /**
     * Save and extract a model archive for an alphabet.
     * @param alphabet the alphabet for this model.
     * @param archiveStream the input stream of the .tar.gz archive.
     */
    fun saveAndExtractModel(alphabet: OCRAlphabet, archiveStream: InputStream) {
        saveAndExtract(getRecognitionModelDir(alphabet), archiveStream)
        refreshModelsFiles()
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
                assetDir = "$OCR_MODELS_ASSET_DIR/${OCRAlphabet.LATIN.recognitionModelDataDir()}",
                targetDir = getRecognitionModelDir(OCRAlphabet.LATIN),
            )
        }
    }

    private fun saveAndExtract(targetDir: File, archiveStream: InputStream) {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        try {
            ZipInputStream(archiveStream).use { zipInput ->
                var entry: ZipEntry? = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val filename = entry.name.substringAfterLast("/")
                        val targetFile = File(targetDir, filename)
                        targetFile.outputStream().use { output ->
                            zipInput.copyTo(output)
                        }
                    }
                    entry = zipInput.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract model into ${targetDir.path}", e)
            targetDir.deleteRecursively()
            throw e
        }
    }

    private fun listRecognitionModels(): Map<OCRAlphabet, String> {
        val recognitionModelsDir = File(textModelDataDir, OCR_RECOGNITION_MODEL_DIR)
        println("TOTO: modelDir=$recognitionModelsDir")
        if (!recognitionModelsDir.exists()) return emptyMap()

        return buildMap {
            recognitionModelsDir.listFiles()?.forEach { file ->
                if (!file.isDirectory) return@forEach
                try {
                    val alphabet = OCRAlphabet.valueOf(file.name.uppercase())
                    if (isRecognitionModelAvailable(alphabet)) put(alphabet, file.path)
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

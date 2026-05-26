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
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import java.io.File


internal const val OCR_MODELS_ASSET_DIR = "models/text"
internal const val OCR_MODELS_DATA_DIR = "models/text"

internal const val OCR_DETECTION_MODEL_DIR = "detect"
internal const val OCR_DETECTION_MODEL_FILE = "det.ncnn.bin"
internal const val OCR_DETECTION_MODEL_PARAMS_FILE = "det.ncnn.param"

internal const val ASSET_PACK_RECOGNITION_MODEL_PREFIX = "text_model_rec_"
internal const val OCR_RECOGNITION_MODEL_DIR = "recognize"
internal const val OCR_RECOGNITION_MODEL_FILE = "rec.ncnn.bin"
internal const val OCR_RECOGNITION_MODEL_PARAMS_FILE = "rec.ncnn.param"
internal const val OCR_RECOGNITION_MODEL_DICTIONARY_FILE = "dict.txt"


internal fun Context.detectionModelDataDir(): File =
    File(filesDir, "$OCR_MODELS_DATA_DIR/$OCR_DETECTION_MODEL_DIR")

internal fun Context.recognitionModelsDataDir(): File =
    File(filesDir, "$OCR_MODELS_DATA_DIR/$OCR_RECOGNITION_MODEL_DIR")

internal fun OCRAlphabet.toRecognitionModelDataDirName(): String =
    name.lowercase()

internal fun File.recognitionModelDataDir(alphabet: OCRAlphabet): File =
    File(this, alphabet.toRecognitionModelDataDirName())

internal fun OCRAlphabet.recognitionModelDataDirPath(): String =
    "$OCR_MODELS_DATA_DIR/${OCR_RECOGNITION_MODEL_DIR}/${toRecognitionModelDataDirName()}"

internal fun OCRAlphabet.recognitionModelAssetPackName(): String =
    "$ASSET_PACK_RECOGNITION_MODEL_PREFIX${toRecognitionModelDataDirName()}"

internal fun OCRAlphabet.recognitionModelAssetDir(): String =
    "$OCR_MODELS_ASSET_DIR/${OCR_RECOGNITION_MODEL_DIR}/${toRecognitionModelDataDirName()}"

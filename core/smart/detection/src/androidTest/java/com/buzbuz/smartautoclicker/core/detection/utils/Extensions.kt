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
package com.buzbuz.smartautoclicker.core.detection.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import java.io.File


internal fun Context.loadTestBitmap(image: TestImage) : Bitmap {
    return resources.openRawResource(image.fileRes).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("Test image file ${image.fileRes} can't be decoded")
    }
}

/**
 * Extracts the bundled OCR model assets to the device filesystem and returns the paths required
 * by [com.buzbuz.smartautoclicker.core.detection.ImageDetector.loadTextDetectionModels].
 *
 * @return Pair of (detectionModelDirPath, recognitionModels map).
 */
internal fun Context.extractTestOcrModels(): Pair<String, Map<String, String>> {
    val modelsRoot = File(cacheDir, "test_ocr_models")

    val detectDir = File(modelsRoot, "detect").also { it.mkdirs() }
    val latinDir  = File(modelsRoot, "recognize/latin").also { it.mkdirs() }

    copyAssetDir("models/text/detect",          detectDir)
    copyAssetDir("models/text/recognize/latin", latinDir)

    return detectDir.absolutePath to mapOf("latin" to latinDir.absolutePath)
}

private fun Context.copyAssetDir(assetDir: String, targetDir: File) {
    assets.list(assetDir)?.forEach { filename ->
        val target = File(targetDir, filename)
        if (!target.exists()) {
            assets.open("$assetDir/$filename").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

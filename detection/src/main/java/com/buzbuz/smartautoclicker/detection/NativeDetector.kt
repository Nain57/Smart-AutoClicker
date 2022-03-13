/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.detection

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.Keep

class NativeDetector : ImageDetector {

    companion object {
        // Used to load the 'smartautoclicker' library on application startup.
        init {
            System.loadLibrary("smartautoclicker")
        }
    }

    private val detectionResult = DetectionResult(false, 0, 0)

    @Keep
    private val nativePtr: Long = newDetector()

    override fun close() = deleteDetector()

    external override fun setScreenImage(screenBitmap: Bitmap)

    override fun detectCondition(conditionBitmap: Bitmap, threshold: Int): DetectionResult {
        detect(conditionBitmap, threshold, detectionResult)
        return detectionResult
    }

    override fun detectCondition(conditionBitmap: Bitmap, position: Rect, threshold: Int): DetectionResult {
        detectAt(conditionBitmap, position.left, position.top, position.width(), position.height(), threshold, detectionResult)
        return detectionResult
    }

    private external fun newDetector(): Long

    private external fun deleteDetector()

    private external fun detect(conditionBitmap: Bitmap, threshold: Int, result: DetectionResult)

    private external fun detectAt(
        conditionBitmap:
        Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        result: DetectionResult
    )
}
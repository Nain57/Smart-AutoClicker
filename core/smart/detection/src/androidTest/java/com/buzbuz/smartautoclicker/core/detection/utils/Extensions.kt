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
import com.buzbuz.smartautoclicker.core.detection.ScreenDetector
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import java.nio.ByteBuffer
import java.nio.channels.Channels


internal fun Context.loadTestBitmap(image: TestImage) : Bitmap {
    return resources.openRawResource(image.fileRes).use { inputStream ->
        val channel = Channels.newChannel(inputStream)
        val buffer = ByteBuffer.allocateDirect(inputStream.available())
        channel.read(buffer)
        buffer.position(0)

        try {
            Bitmap.createBitmap(image.size.x, image.size.y, Bitmap.Config.ARGB_8888).apply {
                copyPixelsFromBuffer(buffer)
            }
        } catch (rEx: RuntimeException) {
            throw IllegalArgumentException("Test image file $this can't be read")
        }
    }
}

internal fun ScreenDetector.setScreenMetrics(screenBitmap: Bitmap, quality: Double) {
    setScreenMetrics(
        metricsKey = "testTag",
        screenBitmap = screenBitmap,
        detectionQuality = quality,
    )
}


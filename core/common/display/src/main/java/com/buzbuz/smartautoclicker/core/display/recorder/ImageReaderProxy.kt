/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.display.recorder

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import javax.inject.Inject


internal class ImageReaderProxy @Inject constructor(
    private val bitmapRepository: BitmapRepository,
) {

    /** Allow access to [Image] rendered into the surface view of the [VirtualDisplay] */
    private var imageReader: ImageReader? = null
    /** The last frame received from the active [imageReader]. */
    private var lastFrame: Bitmap? = null

    val surface: Surface
        get() = imageReader!!.surface

    fun resize(size: Point) {
        imageReader?.close()
        imageReader = ImageReader.newInstance(size.x, size.y, PixelFormat.RGBA_8888, 2)
    }

    fun close() {
        imageReader?.close()
        imageReader = null
        lastFrame = null
    }

    fun getLastFrame(): Bitmap? {
        val reader = imageReader ?: run {
            Log.e(TAG, "Can't get last frame, ImageReader is null")
            return null
        }

        return reader.acquireLatestImage()
            ?.use { image -> image.toBitmap().also { lastFrame = it } }
            ?: lastFrame
    }

    private fun Image.toBitmap(): Bitmap {
        val imageWidth = width + (planes[0].rowStride - planes[0].pixelStride * width) / planes[0].pixelStride
        return bitmapRepository.getDisplayRecorderBitmap(imageWidth, height).apply {
            copyPixelsFromBuffer(planes[0].buffer)
        }
    }
}

private const val TAG = "ImageReaderProxy"
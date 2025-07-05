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

    /**
     * A pixels row in an image.
     * Kept to avoid instantiating a new array at each image. It is reset at each [resize] call.
     */
    private var copyImageRow: IntArray? = null

    val surface: Surface
        get() = imageReader!!.surface

    fun resize(size: Point) {
        copyImageRow = IntArray(size.x)
        imageReader?.close()
        imageReader = ImageReader.newInstance(size.x, size.y, PixelFormat.RGBA_8888, 2)
    }

    fun close() {
        imageReader?.close()
        imageReader = null
        lastFrame = null
        copyImageRow = null
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
        val bitmap = bitmapRepository.getDisplayRecorderBitmap(width, height)

        // If the image provided by the device doesn't have padding, use direct pixels copy
        if (!haveRowPadding() && haveSameSize(bitmap)) {
            if (directPixelsCopyTo(bitmap)) return bitmap
        }

        // Copy manually
        pixelsCopyTo(bitmap)

        return bitmap
    }

    private fun Image.haveRowPadding(): Boolean =
        planes[0].rowStride != (width * planes[0].pixelStride)

    private fun Image.haveSameSize(bitmap: Bitmap): Boolean =
        bitmap.width == width && bitmap.height == height

    private fun Image.directPixelsCopyTo(bitmap: Bitmap): Boolean =
        try {
            bitmap.copyPixelsFromBuffer(planes[0].buffer.asReadOnlyBuffer().rewind())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to direct copy pixels from buffer", e)
            false
        }

    private fun Image.pixelsCopyTo(bitmap: Bitmap) {
        val imageRow = copyImageRow ?: return
        val srcByteBuffer = planes[0].buffer.asIntBuffer()

        for (y in 0 until height) {
            srcByteBuffer.position((y * planes[0].rowStride) / 4)
            srcByteBuffer.get(imageRow)

            // ABGR -> ARGB (swap R & B)
            for (i in imageRow.indices) {
                imageRow[i] = (imageRow[i] and 0xFF00FF00.toInt()) or
                        ((imageRow[i] and 0x00FF0000) ushr 16) or
                        ((imageRow[i] and 0x000000FF) shl 16)
            }
            bitmap.setPixels(imageRow, 0, width, 0, y, width, 1)
        }
    }
}

private const val TAG = "ImageReaderProxy"
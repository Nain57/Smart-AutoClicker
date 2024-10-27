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
import javax.inject.Inject


internal class ImageReaderProxy @Inject constructor() {

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
            ?.use { image -> image.toBitmap(lastFrame).also { lastFrame = it } }
            ?: lastFrame
    }

    /**
     * Transform an Image into a bitmap.
     *
     * @param resultBitmap a bitmap to use as a cache in order to avoid instantiating an new one. If null, a new one is
     *                     created.
     * @return the bitmap corresponding to the image. If [resultBitmap] was provided, it will be the same object.
     */
    private fun Image.toBitmap(resultBitmap: Bitmap? = null): Bitmap {
        var bitmap = resultBitmap
        val imageWidth = width + (planes[0].rowStride - planes[0].pixelStride * width) / planes[0].pixelStride

        if (bitmap == null) {
            Log.d(TAG, "Creating new screen frame bitmap with size $imageWidth/$height")
            bitmap = Bitmap.createBitmap(imageWidth, height, Bitmap.Config.ARGB_8888)

        } else if (bitmap.width != imageWidth || bitmap.height != height) {
            try {
                Log.d(TAG, "Resizing screen frame bitmap with size $imageWidth/$height")
                bitmap.reconfigure(imageWidth, height, Bitmap.Config.ARGB_8888)
            } catch (ex: IllegalArgumentException) {
                bitmap = Bitmap.createBitmap(imageWidth, height, Bitmap.Config.ARGB_8888)
            }
        }

        bitmap.copyPixelsFromBuffer(planes[0].buffer)
        return bitmap
    }
}

private const val TAG = "ImageReaderProxy"
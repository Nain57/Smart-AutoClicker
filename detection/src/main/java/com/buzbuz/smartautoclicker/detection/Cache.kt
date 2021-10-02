/*
 * Copyright (C) 2021 Nain57
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
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.util.LruCache

import androidx.annotation.WorkerThread

import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.testing.OpenForTesting

/**
 * Unified cache for the image processing.
 *
 * In order to avoid unwanted instantiations during image processing and unwanted garbage collection between each
 * processing, we use this class to store all values required to process an [Image].
 * This cache is not synchronized and thus, should be accessed from the same thread or safely.
 *
 * @param bitmapSupplier provides the bitmap for the given path, width and height. Will be called on the processing thread.
 */
@OpenForTesting
@WorkerThread
internal class Cache(private val bitmapSupplier: (String, Int, Int) -> Bitmap?) {

    private companion object {
        /**
         * The ratio of the Jvm max memory size the pixels can take in the cache. Smaller ratio will lead to less
         * memory taken, but more processing time (and more garbage collection).
         */
        private const val CACHE_SIZE_RATIO = 0.5
    }

    /** The size of the display providing the images. */
    val displaySize: Rect = Rect()
    /** The image currently processed. */
    var currentImage: Image? = null
    /** The bitmap of the currently processed [Image] */
    var screenBitmap: Bitmap? = null
    /** The pixels of the currently processed [Image] */
    var screenPixels: IntArray? = null
    /** The difference between the condition currently checked and the corresponding part of the screen. */
    var currentDiff = 0L
    /** The index in the pixels array currently cropped. */
    var cropIndex = 0

    /**
     * The cache for the pixels.
     *
     * The key is the event condition, with [Pair.first] the area on the screen that should be checked, and
     * [Pair.second] the bitmap of the image that should matched.
     *
     * The value is the cached pixels, with [Pair.first] the pixels of the condition, i.o.w. the [Pair.second] value
     * of the key, and [Pair.second] is the instance of the array that will contains the pixels of the currently
     * checked image.
     */
    val pixelsCache: LruCache<Condition, Pair<IntArray, IntArray>?> = object
        : LruCache<Condition, Pair<IntArray, IntArray>?>(
        ((Runtime.getRuntime().maxMemory() / 1024) * CACHE_SIZE_RATIO).toInt()
    ) {

        override fun sizeOf(key: Condition, arrays: Pair<IntArray, IntArray>?): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return arrays?.run { first.size * 2 * 32 / 1024 } ?: 0
        }

        override fun create(condition: Condition): Pair<IntArray, IntArray>? {
            // Pixels of the condition. Size and content never changes during detection.
            val expectedPixels = IntArray(condition.area.height() * condition.area.width())
            val conditionBitmap = bitmapSupplier.invoke(condition.path!!, condition.area.width(), condition.area.height()) ?: return null
            conditionBitmap.getPixels(expectedPixels, 0, condition.area.width(), 0, 0,
                condition.area.width(), condition.area.height())

            // Pixels of the part of the screen currently checked. Size never changes during detection (as the
            // condition size won't change), but content will be updated for each [Image].
            return expectedPixels to IntArray(condition.area.height() * condition.area.width())
        }
    }

    /**
     * Refresh the cached values for the [currentImage].
     *
     * This method must be called before all condition verification methods in order to ensure the processing on the
     * correct values.
     *
     * @param currentDisplaySize the current size of the display.
     */
    fun refreshProcessedImage(currentDisplaySize: Point) {
        if (currentDisplaySize.x != displaySize.width() || currentDisplaySize.y != displaySize.height()) {
            displaySize.apply {
                right = currentDisplaySize.x
                bottom = currentDisplaySize.y
            }
            screenBitmap = null
        }

        currentImage?.let { image ->
            // If this is the first time we process, we need to create the cached bitmap.
            screenBitmap ?: run {
                val pixelStride = image.planes[0].pixelStride
                val rowPadding = image.planes[0].rowStride - pixelStride * displaySize.width()

                // All images should have the same size, as we are detecting on the whole screen, so keep using the same
                // bitmap to avoid instantiating one every time.
                screenBitmap = Bitmap.createBitmap(displaySize.width() + rowPadding / pixelStride, displaySize.height(),
                    Bitmap.Config.ARGB_8888)
                screenPixels = IntArray(screenBitmap!!.width * screenBitmap!!.height)
            }

            screenBitmap!!.apply {
                // Put the pixels from the image buffer into the screen bitmap
                copyPixelsFromBuffer(image.planes[0].buffer)
                // Fill the screen pixel cache.
                getPixels(screenPixels, 0, screenBitmap!!.width, 0, 0, screenBitmap!!.width,
                    screenBitmap!!.height)
            }
        }
    }

    /** Release all cached values. */
    fun release() {
        displaySize.apply {
            right = 0
            bottom = 0
        }
        currentImage?.close()
        currentImage = null
        screenBitmap = null
        screenPixels = null
        pixelsCache.evictAll()
        currentDiff = 0L
        cropIndex = 0
    }
}
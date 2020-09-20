/*
 * Copyright (C) 2020 Nain57
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.util.LruCache
import androidx.annotation.WorkerThread

import com.buzbuz.smartautoclicker.clicks.BitmapManager
import com.buzbuz.smartautoclicker.clicks.ClickCondition
import com.buzbuz.smartautoclicker.clicks.ClickInfo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

/**
 * Process [Image] from an [android.media.ImageReader] in order to provide detection capabilities.
 *
 * This class has two purposes:
 *  - Capture (via [captureArea]): allows you to take a screenshot of a part of the screen
 *  - Detection: (via [detect]): allows you to check if a click from a list must be performed on the current screen.
 *
 * Once you are done with this object, ensure calling [releaseCache] to free all intermediates detection resources.
 *
 * @param context the Android context.
 * @param displaySize the size of the display of the phone.
 */
@WorkerThread
class ImageProcessor(context: Context, private val displaySize: Point) {

    private companion object {
        /** Threshold, in percent (0-100%) for the differences between the conditions and the screen content. */
        private const val DIFFERENCE_THRESHOLD = 1
        /** The ratio of the Jvm max memory size the pixels can take in the cache. */
        private const val CACHE_SIZE_RATIO = 0.5
    }

    /** Cached values avoiding unnecessary allocation at each processing. */
    private val cache = Cache(CACHE_SIZE_RATIO)
    /** The bitmap manager storing all click condition bitmaps. */
    private val bitmapManager = BitmapManager.getInstance(context)

    /**
     * Capture the content of an image at a specified area and creates a bitmap from it.
     *
     * @param image the image to capture from.
     * @param area the area on the image to be captured.
     *
     * @return the newly created click condition.
     */
    fun captureArea(image: Image, area: Rect) : ClickCondition {
        refreshProcessedImage(image)

        return runBlocking(Dispatchers.IO) {
            val conditionPath = bitmapManager.saveBitmap(
                Bitmap.createBitmap(cache.screenBitmap!!, area.left, area.top, area.width(), area.height()))
            ClickCondition(area, conditionPath)
        }
    }

    /**
     * Find a click with the conditions fulfilled on the provided Image.
     *
     * @param image the image to detect on.
     * @param clicks the list of clicks to be verified.
     *
     * @return the first ClickInfo with all conditions fulfilled, or null if none has been found.
     */
    fun detect(image: Image, clicks: List<ClickInfo>) : ClickInfo? {
        refreshProcessedImage(image)

        for (click in clicks) {

            // No conditions ? This should not happen, skip this click
            if (click.conditionList.isEmpty()) {
                continue
            }

            // If conditions are fulfilled, execute this click !
            if (verifyConditions(click.conditionOperator, click.conditionList)) {
                return click
            }
        }

        return null
    }

    /** Release all values cached. **/
    fun releaseCache() {
        cache.release()
    }

    /**
     * Refresh the cached values for the [Image] to be processed.
     *
     * This method must be called before all condition verification methods in order to ensure the processing on the
     * correct values.
     *
     * @param image the new image to process on.
     */
    private fun refreshProcessedImage(image: Image) {
        // If this is the first time we process, we need to create the cached bitmap.
        cache.screenBitmap ?: run {
            val pixelStride = image.planes[0].pixelStride
            val rowPadding = image.planes[0].rowStride - pixelStride * displaySize.x

            // All images should have the same size, as we are detecting on the whole screen, so keep using the same
            // bitmap to avoid instantiating one every time.
            cache.screenBitmap = Bitmap.createBitmap(displaySize.x + rowPadding / pixelStride, displaySize.y,
                Bitmap.Config.ARGB_8888)
            cache.screenPixels = IntArray(cache.screenBitmap!!.width * cache.screenBitmap!!.height)
        }

        cache.screenBitmap!!.apply {
            // Put the pixels from the image buffer into the screen bitmap
            copyPixelsFromBuffer(image.planes[0].buffer)
            // Fill the screen pixel cache.
            getPixels(cache.screenPixels, 0, cache.screenBitmap!!.width, 0, 0, cache.screenBitmap!!.width,
                cache.screenBitmap!!.height)
        }
    }

    /**
     * Verifies if the provided conditions are fulfilled.
     *
     * Applies the provided conditions the currently processed [Image] according to the provided operator.
     * This method must be called after [refreshProcessedImage] in order to check the correct values.
     *
     * @param operator the operator to apply between the conditions. Must be one of [ClickInfo.Operator] values.
     * @param conditions the condition to be checked on the currently processed [Image].
     */
    private fun verifyConditions(
        @ClickInfo.Companion.Operator operator: Int,
        conditions: List<ClickCondition>
    ) : Boolean {

        for (condition in conditions) {
            if (!checkCondition(condition)) {
                if (operator == ClickInfo.AND) {
                    // One of the condition isn't fulfilled.
                    return false
                }
            } else if (operator  == ClickInfo.OR) {
                // One of the condition is fulfilled.
                return true
            }
        }

        // All conditions passed for AND
        return operator == ClickInfo.AND
    }

    /**
     * Check if the provided condition is fulfilled.
     *
     * First, if the condition don't have a cache, initialize it. Then, check if the condition bitmap match the content
     * of the condition area on the currently processed [Image].
     * This method must be called after [refreshProcessedImage] in order to check the correct values.
     *
     * @param condition the area on the currently processed Image to compare with the condition bitmap.
     *
     * @return true if the currently processed [Image] contains the condition bitmap at the condition area.
     */
    private fun checkCondition(condition: ClickCondition) : Boolean {
        // If we have no cache for this condition, create it.
        cache.pixelsCache.get(condition) ?: run {
            // Pixels of the condition. Size and content never changes during detection.
            val conditionPixels = IntArray(condition.area.height() * condition.area.width())
            runBlocking(Dispatchers.IO) {
                bitmapManager.loadBitmap(condition.path, condition.area.width(), condition.area.height())
                    .getPixels(conditionPixels, 0, condition.area.width(), 0, 0,
                        condition.area.width(), condition.area.height())
            }

            // Pixels of the part of the screen currently checked. Size never changes during detection (as the
            // condition size won't change), but content will be updated for each [Image].
            val checkCache = IntArray(condition.area.height() * condition.area.width())

            cache.pixelsCache.put(condition, conditionPixels to checkCache)
        }

        // Now we have a condition cache, so let's detect !
        cache.pixelsCache.get(condition)?.let { pixels ->
            // Get the pixels of the part of the [Image] that will be compared.
            getCroppedPixels(pixels.second, condition.area)

            // For each pixel, compare the RGB values of the condition pixels and the cropped image pixels and keep
            // the difference.
            cache.currentDiff = 0
            for (i in pixels.second.indices) {
                cache.currentDiff += abs((pixels.first[i] shr 16 and 0xff) - (pixels.second[i] shr 16 and 0xff)) +
                        abs((pixels.first[i] shr 8 and 0xff) - (pixels.second[i] shr 8 and 0xff)) +
                        abs((pixels.first[i] and 0xff) - (pixels.second[i] and 0xff))
            }

            // If the difference % is lower than the threshold, the condition is fulfilled; returns true.
            return 100.0 * cache.currentDiff / (3L * 255 * pixels.second.size) < DIFFERENCE_THRESHOLD
        }

        // This should never happen, or a single condition is bigger than the cache size ?
        return false
    }

    /**
     * Fills the provided array with the pixels at the area on the currently processed [Image]
     *
     * This method must be called after [refreshProcessedImage] in order to crop the correct values.
     *
     * @param pixels the array to be filled. It's size must match the area one or an [IndexOutOfBoundsException] will
     *               thrown.
     * @param area the area on the currently processed [Image] to take the pixels from.
     */
    private fun getCroppedPixels(pixels: IntArray, area: Rect) {
        cache.screenBitmap!!.apply {
            cache.cropIndex = 0

            // Pixels are ordered by row in the image they represents; first value is the upper left pixel and last
            // value is the lower right one. A row length is screenBitmap.width.

            // For each row between the crop area top and bottom
            for (y in (area.top * width) until area.bottom * width step width) {
                // In this row, for each pixel between the crop area left and right
                for (x in (area.left + y) until area.right + y) {
                    // We want this pixel in the crop, put it and continue
                    pixels[cache.cropIndex] = cache.screenPixels!![x]
                    cache.cropIndex++
                }
            }
        }
    }

    /**
     * Cache for the [ImageProcessor].
     *
     * In order to avoid unwanted instantiations during image processing and unwanted garbage collection between each
     * processing, we use this class to store all values required to process an [Image].
     *
     * @param pixelsCacheSizeRatio the ratio of the Jvm max memory size the pixels can take in the cache. Smaller ratio
     *                             will lead to less memory taken, but more processing time (and more garbage collection)
     */
    private class Cache(pixelsCacheSizeRatio: Double) {

        /**
         * The cache for the pixels.
         *
         * The key is the click condition, with [Pair.first] the area on the screen that should be checked, and
         * [Pair.second] the bitmap of the image that should matched.
         *
         * The value is the cached pixels, with [Pair.first] the pixels of the condition, i.o.w. the [Pair.second] value
         * of the key, and [Pair.second] is the instance of the array that will contains the pixels of the currently
         * checked image.
         */
        val pixelsCache: LruCache<ClickCondition, Pair<IntArray, IntArray>> = object
            : LruCache<ClickCondition, Pair<IntArray, IntArray>>(
            ((Runtime.getRuntime().maxMemory() / 1024).toInt() * pixelsCacheSizeRatio).toInt()
        ) {

            override fun sizeOf(key: ClickCondition, arrays: Pair<IntArray, IntArray>): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                return arrays.first.size * 2 * 32 / 1024
            }
        }

        /** The bitmap of the currently processed [Image] */
        var screenBitmap: Bitmap? = null
        /** The pixels of the currently processed [Image] */
        var screenPixels: IntArray? = null
        /** The difference between the condition currently checked and the corresponding part of the screen. */
        var currentDiff = 0L
        /** The index in the pixels array currently cropped. */
        var cropIndex = 0

        /** Release all cached values. */
        fun release() {
            screenBitmap = null
            screenPixels = null
            pixelsCache.evictAll()
            currentDiff = 0L
            cropIndex = 0
        }
    }
}
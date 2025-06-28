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
package com.buzbuz.smartautoclicker.core.bitmaps

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject


internal class BitmapLRUCache @Inject constructor() : LruCache<String, Bitmap>(
    ((Runtime.getRuntime().maxMemory() / 1024).toInt() * CACHE_SIZE_RATIO).toInt()
) {

    override fun sizeOf(key: String, bitmap: Bitmap): Int {
        // The cache size will be measured in kilobytes rather than number of items.
        return bitmap.byteCount / 1024
    }

    fun putImageConditionBitmap(path: String, width: Int, height: Int, bitmap: Bitmap) {
        put(getImageConditionKey(path, width, height), bitmap)
    }

    fun getImageConditionBitmapOrDefault(path: String, width: Int, height: Int, insert: () -> Bitmap?) =
        getOrDefault(getImageConditionKey(path, width, height), insert)

    fun getDisplayRecorderBitmapOrDefault(width: Int, height: Int, insert: () -> Bitmap?) =
        getOrDefault(getDisplayRecorderKey(width, height), insert)

    private fun getOrDefault(key: String, insert: () -> Bitmap?) =
        get(key) ?: insert()?.also { newBitmap ->
            put(key, newBitmap)
        }

    private fun getDisplayRecorderKey(width: Int, height: Int): String =
        "key:DISPLAY_RECORDER:$width:$height"

    private fun getImageConditionKey(path: String, width: Int, height: Int): String =
        "key:IMAGE_CONDITION:$path:$width:$height"
}

/** The ratio of the total application size for the size of the bitmap cache in the memory. */
private const val CACHE_SIZE_RATIO = 0.5
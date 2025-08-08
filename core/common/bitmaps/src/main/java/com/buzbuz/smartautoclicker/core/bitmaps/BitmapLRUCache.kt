
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

package com.buzbuz.smartautoclicker.core.bitmaps

import android.graphics.Bitmap
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.FILE_EXTENSION_PNG

/** Manages the bitmaps for the application. */
interface BitmapRepository : Dumpable {

    /**
     * Save the provided bitmap into the persistent memory.
     * If the bitmap is already saved, does nothing.
     *
     * @param bitmap the bitmap to be saved on the persistent memory.
     *
     * @return the path of the bitmap.
     */
    suspend fun saveImageConditionBitmap(bitmap: Bitmap, prefix: String) : String

    /**
     * Load a bitmap.
     * If it was already loaded, returns immediately with the value from the cache. If not, load it from the persistent
     * memory.
     *
     * @param path the path of the bitmap.
     * @param width the width of the bitmap.
     * @param height the height of the bitmap.
     *
     * @return the loaded bitmap, or null if the path is invalid
     */
    suspend fun getImageConditionBitmap(path: String, width: Int, height: Int) : Bitmap?

    /**
     * Get the bitmap for the display recorder
     *
     * @param width the width of the bitmap.
     * @param height the height of the bitmap.
     *
     * @return the loaded bitmap, or null if the path is invalid
     */
    fun getDisplayRecorderBitmap(width: Int, height: Int): Bitmap

    /**
     * Delete the specified bitmaps from the persistent memory.
     *
     * @param paths the paths of the bitmaps to be deleted.
     */
    suspend fun deleteImageConditionBitmaps(paths: List<String>)

    /**
     * Migrate the provided legacy bitmap into the new format.
     *
     * @param path the path of the bitmap to be migrated.
     * @param width the width of the bitmap.
     * @param height the height of the bitmap.
     *
     * @return true if the migration was successful, false otherwise.
     */
    suspend fun migrateImageConditionBitmap(path: String, width: Int, height: Int): String?

    /** Clear the cache of bitmaps. */
    fun clearCache()
}

/** The prefix appended to all bitmap file names. */
const val CONDITION_FILE_PREFIX = "Condition_"
/** File extension for all image conditions */
const val CONDITION_FILE_EXTENSION = FILE_EXTENSION_PNG
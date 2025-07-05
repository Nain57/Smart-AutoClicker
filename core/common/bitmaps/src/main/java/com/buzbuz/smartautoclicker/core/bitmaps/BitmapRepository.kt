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
package com.buzbuz.smartautoclicker.core.bitmaps

import android.graphics.Bitmap
import com.buzbuz.smartautoclicker.core.base.Dumpable

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
/** The prefix appended to all bitmap file names. */
const val TUTORIAL_CONDITION_FILE_PREFIX = "Tutorial_Condition_"
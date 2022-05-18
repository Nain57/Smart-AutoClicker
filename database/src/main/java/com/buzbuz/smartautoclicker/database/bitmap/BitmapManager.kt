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
package com.buzbuz.smartautoclicker.database.bitmap

import android.content.Context
import android.graphics.Bitmap

/** Manages the bitmaps for the click conditions. */
interface BitmapManager {

    companion object {
        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: BitmapManager? = null

        /**
         * Get the repository singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the repository singleton.
         */
        fun getBitmapManager(context: Context): BitmapManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BitmapManagerImpl(context.filesDir)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Save the provided bitmap into the persistent memory.
     * If the bitmap is already saved, does nothing.
     *
     * @param bitmap the bitmap to be saved on the persistent memory.
     *
     * @return the path of the bitmap.
     */
    suspend fun saveBitmap(bitmap: Bitmap) : String

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
    suspend fun loadBitmap(path: String, width: Int, height: Int) : Bitmap?

    /**
     * Delete the specified bitmaps from the persistent memory.
     *
     * @param paths the paths of the bitmaps to be deleted.
     */
    fun deleteBitmaps(paths: List<String>)

    /** Release the cache of bitmaps. */
    fun releaseCache()
}

/** The prefix appended to all bitmap file names. */
const val CLICK_CONDITION_FILE_PREFIX = "Condition_"
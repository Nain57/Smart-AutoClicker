/*
 * Copyright (C) 2023 Kevin Buzeau
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
import android.util.Log
import android.util.LruCache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Manages the bitmaps for the event conditions.
 * Handle the save/load into the persistent memory, as well as the cache loading them in ram.
 *
 * @param appDataDir the directory where all bitmaps will be saved/loaded.
 */
internal class BitmapManagerImpl(private val appDataDir: File) : BitmapManager {

    companion object {
        /** Tag for logs */
        private const val TAG = "BitmapManager"
        /** The ratio of the total application size for the size of the bitmap cache in the memory. */
        private const val CACHE_SIZE_RATIO = 0.5
    }

    /** Cache for the bitmaps loaded in memory. */
    private val memoryCache: LruCache<String, Bitmap> = object
        : LruCache<String, Bitmap>(((Runtime.getRuntime().maxMemory() / 1024).toInt() * CACHE_SIZE_RATIO).toInt()) {

        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return bitmap.byteCount / 1024
        }
    }

    override suspend fun saveBitmap(bitmap: Bitmap, prefix: String) : String {
        val uncompressedBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(uncompressedBuffer)
        uncompressedBuffer.position(0)

        val path = "$prefix${uncompressedBuffer.hashCode()}"
        val file = File(appDataDir, path)
        if (!file.exists()) {
            Log.d(TAG, "Saving $path")

            withContext(Dispatchers.IO) {
                FileOutputStream(file).use {
                    it.channel.write(uncompressedBuffer)
                }
            }
        }

        memoryCache.put(path, bitmap)

        return path
    }

    override suspend fun loadBitmap(path: String, width: Int, height: Int) : Bitmap? {
        var cachedBitmap = memoryCache.get(path)
        if (cachedBitmap != null) {
            return cachedBitmap
        }

        val file = File(appDataDir, path)
        if (!file.exists()) {
            Log.e(TAG, "Invalid path $path, bitmap file can't be found.")
            return null
        }

        return withContext(context = Dispatchers.IO) {
            FileInputStream(file).use {
                Log.d(TAG, "Loading $path")

                val buffer = ByteBuffer.allocateDirect(it.channel.size().toInt())
                it.channel.read(buffer)
                buffer.position(0)

                cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                cachedBitmap.copyPixelsFromBuffer(buffer)

                memoryCache.put(path, cachedBitmap)
                cachedBitmap
            }
        }
    }

    override fun deleteBitmaps(paths: List<String>) {
        for (path in paths) {
            val file = File(appDataDir, path)
            if (!file.exists()) {
                return
            }

            Log.d(TAG, "Deleting $path")
            file.delete()
        }
    }

    override fun releaseCache() {
        memoryCache.evictAll()
    }
}
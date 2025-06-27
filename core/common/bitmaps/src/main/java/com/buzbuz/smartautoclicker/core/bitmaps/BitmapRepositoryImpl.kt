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
import android.util.Log
import androidx.core.graphics.createBitmap
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import javax.inject.Inject

internal class BitmapRepositoryImpl @Inject constructor(
    private val bitmapLRUCache: BitmapLRUCache,
    private val conditionBitmapsDataSource: ConditionBitmapsDataSource,
) : BitmapRepository {

    override suspend fun saveImageConditionBitmap(bitmap: Bitmap, prefix: String): String {
        val path = conditionBitmapsDataSource.saveBitmap(bitmap, prefix)
        bitmapLRUCache.put(path, bitmap)
        return path
    }

    override suspend fun getImageConditionBitmap(path: String, width: Int, height: Int): Bitmap? =
        bitmapLRUCache.getOrDefault(path) {
            runBlocking { conditionBitmapsDataSource.loadBitmap(path, width, height) }
        }

    override fun getDisplayRecorderBitmap(width: Int, height: Int): Bitmap =
        bitmapLRUCache.getOrDefault(getDisplayRecorderKey(width, height)) {
            createBitmap(width, height)
        } ?: throw IllegalStateException("Can't create display recorder bitmap with size $width/$height")

    override suspend fun deleteImageConditionBitmaps(paths: List<String>) {
        conditionBitmapsDataSource.deleteBitmaps(paths)
    }

    override suspend fun migrateImageConditionBitmap(path: String, width: Int, height: Int): String? {
        Log.d(TAG, "Migrating legacy bitmap $path")

        val legacyBitmap = conditionBitmapsDataSource.loadLegacyBitmap(path, width, height) ?: return null
        conditionBitmapsDataSource.deleteBitmaps(listOf(path))

        return conditionBitmapsDataSource.saveBitmap(legacyBitmap, CONDITION_FILE_PREFIX)
    }

    override fun releaseCache() {
        bitmapLRUCache.evictAll()
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* BitmapManager:")
            append(contentPrefix)
                .append("- cacheSize=[${bitmapLRUCache.size()}/${bitmapLRUCache.maxSize()}]; ")
                .append("hit/miss=[${bitmapLRUCache.hitCount()}/${bitmapLRUCache.missCount()}]; ")
                .println()
        }
    }

    private fun getDisplayRecorderKey(width: Int, height: Int): String =
        "key:DISPLAY_RECORDER:$width:$height"
}

private const val TAG = "BitmapRepository"
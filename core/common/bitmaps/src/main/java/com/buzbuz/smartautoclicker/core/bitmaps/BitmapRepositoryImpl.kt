
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
        bitmapLRUCache.putImageConditionBitmap(path, bitmap.width, bitmap.height, bitmap)
        return path
    }

    override suspend fun getImageConditionBitmap(path: String, width: Int, height: Int): Bitmap? =
        bitmapLRUCache.getImageConditionBitmapOrDefault(path, width, height) {
            runBlocking { conditionBitmapsDataSource.loadBitmap(path, width, height) }
        }

    override fun getDisplayRecorderBitmap(width: Int, height: Int): Bitmap =
        bitmapLRUCache.getDisplayRecorderBitmapOrDefault(width, height) {
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

    override fun clearCache() {
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
}

private const val TAG = "BitmapRepository"
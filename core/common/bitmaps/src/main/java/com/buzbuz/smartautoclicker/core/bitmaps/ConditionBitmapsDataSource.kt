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
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.createBitmap

import com.buzbuz.smartautoclicker.core.base.FILE_EXTENSION_PNG
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

internal class ConditionBitmapsDataSource @Inject constructor(
    @Dispatcher(HiltCoroutineDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val appDataDir: File,
) {

    suspend fun saveBitmap(bitmap: Bitmap, prefix: String) : String {
        val path = "$prefix${bitmap.getBitmapIdentifier()}${FILE_EXTENSION_PNG}"
        val file = File(appDataDir, path)

        if (file.exists()) {
            Log.w(TAG, "Can't save bitmap $path, file already exists")
            return path
        }

        Log.d(TAG, "Saving $path")

        withContext(ioDispatcher) {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

        return path
    }

    suspend fun loadBitmap(path: String, targetWidth: Int, targetHeight: Int) : Bitmap? {
        if (!path.endsWith(FILE_EXTENSION_PNG)) return null

        val file = File(appDataDir, path)
        if (!file.exists()) {
            Log.e(TAG, "Invalid path $path, bitmap file can't be found.")
            return null
        }

        Log.d(TAG, "Loading $path")

        return withContext(ioDispatcher) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                options.apply {
                    inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val loadedBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                loadedBitmap

            } catch (e: Exception) {
                Log.e(TAG, "Can't load image, size is invalid")
                null
            }
        }
    }

    suspend fun deleteBitmaps(paths: List<String>) {
        paths.forEach { path ->
            val file = File(appDataDir, path)
            if (!file.exists()) {
                return
            }

            withContext(ioDispatcher) {
                Log.d(TAG, "Deleting $path")
                file.delete()
            }
        }
    }

    /** Load a bitmap from pre 3.4.0 format (raw pixel data). */
    suspend fun loadLegacyBitmap(path: String, width: Int, height: Int) : Bitmap? {
        val file = File(appDataDir, path)
        if (!file.exists()) {
            Log.e(TAG, "Invalid path $path, bitmap file can't be found.")
            return null
        }

        return withContext(ioDispatcher) {
            FileInputStream(file).use {
                Log.d(TAG, "Loading $path")

                val buffer = ByteBuffer.allocateDirect(it.channel.size().toInt())
                it.channel.read(buffer)
                buffer.position(0)

                try {
                    createBitmap(width, height).apply {
                        copyPixelsFromBuffer(buffer)
                    }
                } catch (rEx: RuntimeException) {
                    Log.e(TAG, "Can't load image, size is invalid")
                    null
                }
            }
        }
    }

    /**
     * BitmapFactory API allows to decode a bitmap with a scaling ratio that needs to be a power of two.
     *
     * This will almost never produce a bitmap with the exact requested size, leading to not all images having the
     * same actual scaled ratio. With their current format, condition bitmaps won't be directly usable for template
     * matching, and we will need a second scaling pass (during openCv pre processing) to scale to the actual target
     * size.
     *
     * Why scaling twice ? Because this avoids loading the full size bitmap into the java heap, saving lot
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, targetWidth: Int, targetHeight: Int): Int {
        if (targetWidth == 0 || targetHeight == 0) return 1 // No target size, load full

        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > targetHeight || width > targetWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun Bitmap.getBitmapIdentifier(): Int =
        ByteBuffer.allocateDirect(byteCount).let { buffer ->
            copyPixelsToBuffer(buffer)
            buffer.position(0)
            buffer.hashCode()
        }
}

private const val TAG = "ConditionBitmapsDataSource"
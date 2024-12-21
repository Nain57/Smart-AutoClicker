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

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject


internal class ConditionBitmapsDataSource @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val appDataDir: File,
) {

    suspend fun saveBitmap(bitmap: Bitmap, prefix: String) : String {
        val uncompressedBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(uncompressedBuffer)
        uncompressedBuffer.position(0)

        val path = "$prefix${uncompressedBuffer.hashCode()}"
        val file = File(appDataDir, path)
        if (!file.exists()) {
            Log.d(TAG, "Saving $path")

            withContext(ioDispatcher) {
                FileOutputStream(file).use {
                    it.channel.write(uncompressedBuffer)
                }
            }
        }

        return path
    }

    suspend fun loadBitmap(path: String, width: Int, height: Int) : Bitmap? {
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
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        copyPixelsFromBuffer(buffer)
                    }
                } catch (rEx: RuntimeException) {
                    Log.e(TAG, "Can't load image, size is invalid")
                    null
                }
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
}

private const val TAG = "ConditionBitmapsDataSource"
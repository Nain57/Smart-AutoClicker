
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

        return withContext(ioDispatcher) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                val fileWidth = options.outWidth
                val fileHeight = options.outHeight
                options.apply {
                    inSampleSize = calculateInSampleSize(fileWidth, fileHeight, targetWidth, targetHeight)
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val loadedBitmap = BitmapFactory.decodeFile(file.absolutePath, options)

                Log.d(TAG, "Bitmap loaded: onDisk=[$fileWidth/$fileHeight], " +
                        "requested=[$targetWidth/$targetHeight], " +
                        "actual=[${loadedBitmap.width}/${loadedBitmap.height}]")

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
     * Why scaling twice ? Because this avoids loading the full size bitmap into the java heap, saving lot of RAM.
     * Why not doing it at once in native code ? Open CV doesn't offers an API for reading at a scale and requires a
     * custom implementation.
     */
    private fun calculateInSampleSize(fileWidth: Int, fileHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        if (targetWidth == 0 || targetHeight == 0) return 1 // No target size, load full

        var inSampleSize = 1

        if (fileHeight > targetHeight || fileWidth > targetWidth) {
            val halfHeight: Int = fileHeight / 2
            val halfWidth: Int = fileWidth / 2

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
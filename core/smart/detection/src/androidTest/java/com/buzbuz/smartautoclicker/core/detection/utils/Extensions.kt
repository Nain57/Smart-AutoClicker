
package com.buzbuz.smartautoclicker.core.detection.utils

import android.content.Context
import android.graphics.Bitmap
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.detection.data.TestImage
import java.nio.ByteBuffer
import java.nio.channels.Channels


internal fun Context.loadTestBitmap(image: TestImage) : Bitmap {
    return resources.openRawResource(image.fileRes).use { inputStream ->
        val channel = Channels.newChannel(inputStream)
        val buffer = ByteBuffer.allocateDirect(inputStream.available())
        channel.read(buffer)
        buffer.position(0)

        try {
            Bitmap.createBitmap(image.size.x, image.size.y, Bitmap.Config.ARGB_8888).apply {
                copyPixelsFromBuffer(buffer)
            }
        } catch (rEx: RuntimeException) {
            throw IllegalArgumentException("Test image file $this can't be read")
        }
    }
}

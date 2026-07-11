/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.detection.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.io.File

/**
 * One-shot utility; not a real test.
 * Run it once on the emulator to regenerate screen_text_conditions.png, then pull with:
 *   adb pull /data/local/tmp/number_test_images/screen_text_conditions.png
 * and copy it into core/smart/detection/src/androidTest/res/raw/.
 *
 * Layout: numbers from [NUMBERS] stacked vertically, each row [ROW_HEIGHT]px tall,
 * with [ROW_GAP]px between rows. Detection areas match [TestImage.NumberConditionsScreen.numberTestCases].
 */
class NumberBitmapGenerator {

    @Test
    fun generateNumberImages() {
        val outputDir = File("/data/local/tmp/number_test_images").also { it.mkdirs() }
        val bitmap = renderNumberScreen()
        File(outputDir, "screen_text_conditions.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        android.util.Log.i("NumberBitmapGenerator", "Saved screen_text_conditions.png (${bitmap.width}x${bitmap.height})")
    }
}

private val NUMBERS = listOf(
    "42",
    "42.5",
    "42,5",
    "42.588",
    "42,588",
    "1.234.567,890",
    "1,234,567.890",
)

private const val FONT_SIZE = 48f
private const val PADDING   = 20
private const val ROW_GAP   = 20

internal fun renderNumberScreen(): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = FONT_SIZE
        typeface = Typeface.MONOSPACE
    }

    val fm = paint.fontMetrics
    val rowHeight = (-fm.ascent + fm.descent).toInt() + PADDING * 2
    val canvasW = NUMBERS.maxOf { paint.measureText(it).toInt() } + PADDING * 2
    val canvasH = rowHeight * NUMBERS.size + ROW_GAP * (NUMBERS.size - 1)

    val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply {
        drawColor(Color.WHITE)
        NUMBERS.forEachIndexed { index, text ->
            val rowY = index * (rowHeight + ROW_GAP)
            val baseline = rowY + PADDING + (-fm.ascent)
            drawText(text, PADDING.toFloat(), baseline, paint)
        }
    }
    return bitmap
}

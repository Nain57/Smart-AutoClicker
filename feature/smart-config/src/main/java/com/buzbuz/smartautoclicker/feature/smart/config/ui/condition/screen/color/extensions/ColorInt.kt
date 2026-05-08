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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions

import android.graphics.Color
import androidx.annotation.ColorInt


fun Int.toRgbaHexString(): String {
    val a = (this shr 24) and 0xFF
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return "#%02X%02X%02X%02X".format(r, g, b, a)
}

fun Int.getRedValue(): Int =
    (this shr 16) and 0xFF

fun Int.getGreenValue(): Int =
    (this shr 8) and 0xFF

fun Int.getBlueValue(): Int =
    this and 0xFF

@ColorInt
fun rgbToColorInt(red: Int, green: Int, blue: Int): Int =
    Color.rgb(
        red.coerceIn(0, 255),
        green.coerceIn(0, 255),
        blue.coerceIn(0, 255),
    )

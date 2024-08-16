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
package com.buzbuz.smartautoclicker.core.display

import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.view.RoundedCorner
import androidx.annotation.RequiresApi

import java.io.PrintWriter

data class DisplayConfig(
    val sizePx: Point,
    val orientation: Int,
    val safeInsetTopPx: Int,
    val roundedCorners: Map<Corner, DisplayRoundedCorner?>,
)

fun DisplayConfig.haveRoundedCorner(): Boolean =
    if (roundedCorners.isEmpty()) false
    else roundedCorners.values.find { corner -> corner != null } != null

data class DisplayRoundedCorner(
    val centerPx: Point,
    val radiusPx: Int,
)

enum class Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun Corner.toAndroidApiValue(): Int =
    when (this) {
        Corner.TOP_LEFT -> RoundedCorner.POSITION_TOP_LEFT
        Corner.TOP_RIGHT -> RoundedCorner.POSITION_TOP_RIGHT
        Corner.BOTTOM_LEFT -> RoundedCorner.POSITION_BOTTOM_LEFT
        Corner.BOTTOM_RIGHT -> RoundedCorner.POSITION_BOTTOM_RIGHT
    }

internal fun PrintWriter.append(prefix: CharSequence, displayConfig: DisplayConfig): PrintWriter {
    append(prefix).append("DisplayConfig:")

    val contentPrefix = "$prefix - "
    append(contentPrefix).append("Size (Px): ").append(displayConfig.sizePx.toString()).println()
    append(contentPrefix).append("Orientation: ").append(displayConfig.orientation.toOrientationString()).println()
    append(contentPrefix).append("Safe inset top (Px): ").append(displayConfig.safeInsetTopPx.toString()).println()
    displayConfig.roundedCorners.entries.forEach { (corner, roundedCorner) ->
        roundedCorner?.let { append(contentPrefix).append(corner, roundedCorner).println() }
    }

    return this
}

private fun PrintWriter.append(corner: Corner, rounderCorner: DisplayRoundedCorner): PrintWriter =
    append("Display corner ").append(corner.name)
        .append(": [center (Px): ").append(rounderCorner.centerPx.toString())
        .append(", radius (Px): ").append(rounderCorner.radiusPx.toString())
        .append("]")


private fun Int?.toOrientationString(): String = when (this) {
    Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
    Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
    else -> "UNDEFINED"
}

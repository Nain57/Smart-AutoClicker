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
package com.buzbuz.smartautoclicker.core.display.config

import android.content.res.Configuration
import android.view.Display
import android.view.Surface
import java.io.PrintWriter


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

internal fun PrintWriter.append(prefix: CharSequence, display: Display): PrintWriter {
    append(prefix).append("Android Display:")

    val contentPrefix = "$prefix - "
    append(contentPrefix).append("Display: ${display.name}#${display.displayId}").println()
    append(contentPrefix).append("Rotation: ${display.rotation.toSurfaceRotationString()}").println()

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

private fun Int?.toSurfaceRotationString(): String = when (this) {
    Surface.ROTATION_0 -> "ROTATION_0"
    Surface.ROTATION_180 -> "ROTATION_180"
    Surface.ROTATION_90 -> "ROTATION_90"
    Surface.ROTATION_270 -> "ROTATION_270"
    else -> "UNDEFINED"
}
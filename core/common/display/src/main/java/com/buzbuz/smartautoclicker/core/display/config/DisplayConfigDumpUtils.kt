
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
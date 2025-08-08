

package com.buzbuz.smartautoclicker.core.processing.data.scaling

import android.graphics.Point
import android.graphics.Rect
import kotlin.math.roundToInt

internal fun Point.toArea(): Rect =
    Rect(0, 0, x, y)

internal fun Point.scale(scalingRatio: Double): Point =
    if (scalingRatio == 1.0) this
    else Point(
        (x * scalingRatio).roundToInt(),
        (y * scalingRatio).roundToInt()
    )

internal fun Rect.scale(scalingRatio: Double): Rect =
    if (scalingRatio == 1.0) this
    else {
        val x = (left * scalingRatio).roundToInt()
        val y = (top * scalingRatio).roundToInt()
        Rect(
            x,
            y,
            x + (width() * scalingRatio).roundToInt(),
            y + (height() * scalingRatio).roundToInt()
        )
    }

internal fun Rect.grow(bounds: Rect, growValue: Int = 1): Rect =
    Rect(
        (left - growValue).coerceIn(bounds.left, bounds.right),
        (top - growValue).coerceIn(bounds.top, bounds.bottom),
        (right + growValue).coerceIn(bounds.left, bounds.right),
        (bottom + growValue).coerceIn(bounds.top, bounds.bottom),
    )

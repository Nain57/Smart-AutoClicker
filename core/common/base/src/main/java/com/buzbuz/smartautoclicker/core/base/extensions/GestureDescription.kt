
package com.buzbuz.smartautoclicker.core.base.extensions

import android.accessibilityservice.GestureDescription
import android.graphics.Path

import kotlin.math.max
import kotlin.math.min

fun GestureDescription.Builder.buildSingleStroke(path: Path, durationMs: Long, startTime: Long = 0): GestureDescription {
    try {
        addStroke(
            GestureDescription.StrokeDescription(
                path,
                startTime.toNormalizedStrokeStartTime(),
                durationMs.toNormalizedStrokeDurationMs(),
            )
        )
    } catch (ex: IllegalStateException) {
        throw IllegalStateException("Invalid gesture; Duration=$durationMs", ex)
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid gesture; Duration=$durationMs", ex)
    }

    return build()
}

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))


private const val MINIMUM_STROKE_DURATION_MS = 1L
private const val MAXIMUM_STROKE_DURATION_MS = 59_999L
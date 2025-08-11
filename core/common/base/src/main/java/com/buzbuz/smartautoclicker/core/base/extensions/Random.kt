
package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.PointF
import android.graphics.RectF

import kotlin.random.Random

fun Random.nextFloat(from: Float, until: Float): Float =
    (until - from) * nextFloat()

fun Random.nextPositionIn(area: RectF): PointF =
    PointF(nextFloat(area.left, area.right), nextFloat(area.top, area.bottom))

fun Random.nextIntInOffset(value: Int, offset: Int): Int = nextInt(
    from = value - offset,
    until = value + offset + 1,
)

fun Random.nextLongInOffset(value: Long, offset: Long): Long = nextLong(
    from = value - offset,
    until = value + offset + 1,
)

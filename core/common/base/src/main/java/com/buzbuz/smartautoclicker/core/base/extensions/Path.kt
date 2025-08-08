
package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.Path
import kotlin.math.max

fun Path.safeMoveTo(x: Int, y: Int): Unit =
    moveTo(max(0, x).toFloat(), max(0, y).toFloat())

fun Path.safeLineTo(x: Int, y: Int): Unit =
    lineTo(max(0, x).toFloat(), max(0, y).toFloat())
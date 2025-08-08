
package com.buzbuz.smartautoclicker.core.display.config

import android.graphics.Point
import android.os.Build
import android.view.RoundedCorner
import androidx.annotation.RequiresApi


data class DisplayConfig(
    val sizePx: Point,
    val orientation: Int,
    val safeInsetTopPx: Int,
    val roundedCorners: Map<Corner, DisplayRoundedCorner?>,
)

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


fun DisplayConfig.haveRoundedCorner(): Boolean =
    if (roundedCorners.isEmpty()) false
    else roundedCorners.values.find { corner -> corner != null } != null

@RequiresApi(Build.VERSION_CODES.S)
internal fun Corner.toAndroidApiValue(): Int =
    when (this) {
        Corner.TOP_LEFT -> RoundedCorner.POSITION_TOP_LEFT
        Corner.TOP_RIGHT -> RoundedCorner.POSITION_TOP_RIGHT
        Corner.BOTTOM_LEFT -> RoundedCorner.POSITION_BOTTOM_LEFT
        Corner.BOTTOM_RIGHT -> RoundedCorner.POSITION_BOTTOM_RIGHT
    }
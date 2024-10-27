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
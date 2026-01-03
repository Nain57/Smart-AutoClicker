/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.clickoffset

import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.ColorInt
import com.buzbuz.smartautoclicker.core.ui.R


internal class ClickOffsetStyle(
    @field:ColorInt val innerColor: Int,
    @field:ColorInt val outerColor: Int,
    @field:ColorInt val backgroundColor: Int,
    val radiusPx: Float,
    val innerRadiusPx: Float,
    val thicknessPx: Float,
)

internal fun TypedArray.getClickOffsetStyle() =
    ClickOffsetStyle(
        innerColor = getColor(
            R.styleable.ClickOffsetView_colorInner,
            DEFAULT_CLICK_OFFSET_INNER_COLOR,
        ),
        outerColor = getColor(
            R.styleable.ClickOffsetView_colorOutlinePrimary,
            DEFAULT_CLICK_OFFSET_OUTER_COLOR,
        ),
        backgroundColor = getColor(
            R.styleable.ClickOffsetView_colorBackground,
            DEFAULT_CLICK_OFFSET_BACKGROUND_COLOR,
        ),
        radiusPx = getDimensionPixelSize(
            R.styleable.ClickOffsetView_radius,
            DEFAULT_CLICK_OFFSET_RADIUS_PX,
        ).toFloat(),
        innerRadiusPx = getDimensionPixelSize(
            R.styleable.ClickOffsetView_innerRadius,
            DEFAULT_CLICK_OFFSET_INNER_RADIUS_PX,
        ).toFloat(),
        thicknessPx = getDimensionPixelSize(
            R.styleable.ClickOffsetView_thickness,
            DEFAULT_CLICK_OFFSET_THICKNESS_PX,
        ).toFloat()
    )

private const val DEFAULT_CLICK_OFFSET_INNER_COLOR = Color.RED
private const val DEFAULT_CLICK_OFFSET_OUTER_COLOR = Color.BLUE
private const val DEFAULT_CLICK_OFFSET_BACKGROUND_COLOR = Color.BLACK
private const val DEFAULT_CLICK_OFFSET_INNER_RADIUS_PX = 10
private const val DEFAULT_CLICK_OFFSET_RADIUS_PX = 30
private const val DEFAULT_CLICK_OFFSET_THICKNESS_PX = 4
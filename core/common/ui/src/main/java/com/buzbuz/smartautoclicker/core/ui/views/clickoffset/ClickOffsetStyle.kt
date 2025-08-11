
package com.buzbuz.smartautoclicker.core.ui.views.clickoffset

import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.ColorInt
import com.buzbuz.smartautoclicker.core.ui.R


internal class ClickOffsetStyle(
    @ColorInt val innerColor: Int,
    @ColorInt val outerColor: Int,
    @ColorInt val backgroundColor: Int,
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
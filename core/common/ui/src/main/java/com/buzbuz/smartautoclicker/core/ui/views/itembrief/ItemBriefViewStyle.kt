/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.itembrief

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.ColorInt

import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickBriefRendererStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.DefaultBriefRendererStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions.ScreenConditionBriefRendererStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.PauseBriefRendererStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.SwipeBriefRendererStyle


internal class ItemBriefViewStyle(
    val clickStyle: ClickBriefRendererStyle,
    val swipeStyle: SwipeBriefRendererStyle,
    val pauseStyle: PauseBriefRendererStyle,
    val imageConditionStyle: ScreenConditionBriefRendererStyle,
    val defaultStyle: DefaultBriefRendererStyle,
)

internal fun Context.getItemBriefStyle(attrs: AttributeSet, defStyleAttr: Int): ItemBriefViewStyle =
    obtainStyledAttributes(attrs, R.styleable.ItemBriefView, R.attr.itemBriefStyle, defStyleAttr).use { ta ->

        val thickness = ta.getDimensionPixelSize(R.styleable.ItemBriefView_thickness, 4).toFloat()
        val outerRadius = ta.getDimensionPixelSize(R.styleable.ItemBriefView_radius, 30).toFloat()
        val innerRadius = ta.getDimensionPixelSize(R.styleable.ItemBriefView_innerRadius, 4).toFloat()
        val cornerRadius = ta.getDimensionPixelSize(R.styleable.ItemBriefView_cornerRadius, 2).toFloat()

        val innerColor = ta.getColor(R.styleable.ItemBriefView_colorInner, Color.WHITE)
        val backgroundColor = ta.getColor(R.styleable.ItemBriefView_colorBackground, Color.TRANSPARENT)
        val colorOutlinePrimary = ta.getColor(R.styleable.ItemBriefView_colorOutlinePrimary, Color.RED)
        val colorOutlineSecondary = ta.getColor(R.styleable.ItemBriefView_colorOutlineSecondary, Color.GREEN)

        ItemBriefViewStyle(
            clickStyle = ClickBriefRendererStyle(
                backgroundColor = backgroundColor,
                outerPaint = newStrokePaint(colorOutlinePrimary, thickness),
                innerPaint = newFillPaint(innerColor),
                outerRadiusPx = outerRadius,
                innerRadiusPx = innerRadius,
            ),
            swipeStyle = SwipeBriefRendererStyle(
                backgroundColor = backgroundColor,
                linePaint = newFillPaint(innerColor, innerRadius / 2f),
                outerFromPaint = newStrokePaint(colorOutlinePrimary, thickness),
                innerFromPaint = newFillPaint(innerColor),
                outerToPaint = newStrokePaint(colorOutlineSecondary, thickness),
                innerToPaint = newFillPaint(innerColor),
                outerRadiusPx = outerRadius,
                innerRadiusPx = innerRadius,
            ),
            pauseStyle = PauseBriefRendererStyle(
                backgroundColor = backgroundColor,
                outerPaint = newStrokePaint(colorOutlinePrimary, thickness),
                linePaint = newFillPaint(innerColor, innerRadius / 2f),
                thicknessPx = thickness,
                outerRadiusPx = outerRadius,
                innerRadiusPx = innerRadius,
            ),
            imageConditionStyle = ScreenConditionBriefRendererStyle(
                backgroundColor = backgroundColor,
                selectorColor = colorOutlinePrimary,
                iconSize = outerRadius,
                thicknessPx = thickness.toInt(),
                cornerRadiusPx = cornerRadius,
            ),
            defaultStyle = DefaultBriefRendererStyle(
                backgroundColor = backgroundColor,
                iconColor = colorOutlinePrimary,
                iconSize = outerRadius,
                outerPaint = newStrokePaint(colorOutlinePrimary, thickness),
            ),
        )
    }

private fun newStrokePaint(@ColorInt paintColor: Int, strokeWidthPx: Float) = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    color = paintColor
    strokeWidth = strokeWidthPx
}

private fun newFillPaint(@ColorInt paintColor: Int, thicknessPx: Float? = null) = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.FILL
    color = paintColor
    thicknessPx?.let { strokeWidth = it }
}
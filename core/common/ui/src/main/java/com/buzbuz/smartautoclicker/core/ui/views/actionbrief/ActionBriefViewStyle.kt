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
package com.buzbuz.smartautoclicker.core.ui.views.actionbrief

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet

import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.buzbuz.smartautoclicker.core.ui.R

/**
 * Defines the style for the [ActionBriefView].
 *
 * @param outerFromPaint the paint drawing the outer circle of the position 1.
 * @param innerFromPaint the paint drawing the inner circle of the position 1.
 * @param outerToPaint the paint drawing the outer circle of the position 2.
 * @param innerToPaint the point drawing the inner circle of the position 2.
 * @param linePaint the paint for drawing the swipe lane.
 * @param thickness the thickness of the outer circle.
 * @param outerRadius the circles radius.
 * @param innerRadius the inner small circle radius.
 * @param backgroundCircleRadius the radius of the transparent background between the inner and outer circle.
 * @param backgroundColor the color of the faded background.
 */
internal class ActionBriefViewStyle(
    val outerFromPaint: Paint,
    val innerFromPaint: Paint,
    val outerToPaint: Paint,
    val innerToPaint: Paint,
    val linePaint: Paint,
    val thickness: Float,
    val outerRadius: Float,
    val innerRadius: Float,
    val backgroundCircleRadius: Float,
    @ColorInt val backgroundColor: Int,
)

internal fun Context.getActionBriefStyle(attrs: AttributeSet, defStyleAttr: Int): ActionBriefViewStyle =
    obtainStyledAttributes(attrs, R.styleable.ActionBriefView, R.attr.actionBriefStyle, defStyleAttr).use { ta ->

        val thickness = ta.getDimensionPixelSize(R.styleable.ActionBriefView_thickness, 4).toFloat()
        val outerRadius = ta.getDimensionPixelSize(R.styleable.ActionBriefView_radius, 30).toFloat()
        val innerRadius = ta.getDimensionPixelSize(R.styleable.ActionBriefView_innerRadius, 4)
            .toFloat()
        val innerColor = ta.getColor(R.styleable.ActionBriefView_colorInner, Color.WHITE)
        val backgroundCircleStroke = outerRadius - (thickness / 2 + innerRadius)

        ActionBriefViewStyle(
            outerFromPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = ta.getColor(R.styleable.ActionBriefView_colorOutlinePrimary, Color.RED)
                strokeWidth = thickness
            },
            innerFromPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = innerColor
            },
            outerToPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = ta.getColor(R.styleable.ActionBriefView_colorOutlineSecondary, Color.GREEN)
                strokeWidth = thickness
            },
            innerToPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = innerColor
            },
            linePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = innerColor
                strokeWidth = innerRadius / 2f
            },
            outerRadius = outerRadius,
            innerRadius = innerRadius,
            thickness = thickness,
            backgroundCircleRadius = outerRadius - thickness / 2 - backgroundCircleStroke / 2,
            backgroundColor = ta.getColor(R.styleable.ActionBriefView_colorBackground, Color.TRANSPARENT),
        )
    }

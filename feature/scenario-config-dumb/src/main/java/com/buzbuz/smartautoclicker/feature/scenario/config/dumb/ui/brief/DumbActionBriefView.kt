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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R

class DumbActionBriefView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Position of the first point selected. */
    var position1: PointF? = null
        private set
    /** Position of the second point selected. */
    var position2: PointF? = null
        private set

    /** Paint drawing the outer circle of the position 1. */
    private val outerFromPaint = Paint()
    /** Paint drawing the inner circle of the position 1. */
    private val innerFromPaint = Paint()
    /** Paint drawing the outer circle of the position 2. */
    private val outerToPaint = Paint()
    /** Paint drawing the inner circle of the position 2. */
    private val innerToPaint = Paint()
    /** Paint for the background of the circles. */
    private val backgroundPaint = Paint()

    /** The circle radius. */
    private var outerRadius: Float = 0f
    /** The inner small circle radius. */
    private var innerCircleRadius: Float = 0F
    /** The radius of the transparent background between the inner and outer circle. */
    private var backgroundCircleRadius: Float = 0F

    init {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.DumbActionBriefView, R.attr.dumbActionBriefStyle, defStyleAttr).use { ta ->
                val thickness = ta.getDimensionPixelSize(R.styleable.DumbActionBriefView_thickness, 4).toFloat()
                outerRadius = ta.getDimensionPixelSize(R.styleable.DumbActionBriefView_radius, 30).toFloat()
                innerCircleRadius = ta.getDimensionPixelSize(R.styleable.DumbActionBriefView_innerRadius, 4)
                    .toFloat()
                val backgroundCircleStroke = outerRadius - (thickness / 2 + innerCircleRadius)
                backgroundCircleRadius = outerRadius - thickness / 2 - backgroundCircleStroke / 2

                outerFromPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = ta.getColor(R.styleable.DumbActionBriefView_colorOutlinePrimary, Color.RED)
                    strokeWidth = thickness
                }

                innerFromPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = ta.getColor(R.styleable.DumbActionBriefView_colorInner, Color.WHITE)
                }

                outerToPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = ta.getColor(R.styleable.DumbActionBriefView_colorOutlineSecondary, Color.GREEN)
                    strokeWidth = thickness
                }

                innerToPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = innerFromPaint.color
                }

                backgroundPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = ta.getColor(R.styleable.DumbActionBriefView_colorBackground, Color.TRANSPARENT)
                    strokeWidth = backgroundCircleStroke
                }
            }
        }
    }

    fun setState(pos1: PointF?, pos2: PointF? = null) {
        if (pos1 == position1 && pos2 == position2) return

        position1 = pos1
        position2 = pos2
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        position1?.let { drawSelectorCircle(canvas, it, outerFromPaint, innerFromPaint) }
        position2?.let { drawSelectorCircle(canvas, it, outerToPaint, innerToPaint) }
    }

    /**
     * Draw the selector circle at the specified position.
     *
     * @param canvas the canvas to draw the circles on.
     * @param position the position of the circle selector.
     * @param outerPaint the paint used to draw the big circle.
     * @param innerPaint the paint used to draw the small inner circle.
     */
    private fun drawSelectorCircle(canvas: Canvas, position: PointF, outerPaint: Paint, innerPaint: Paint) {
        canvas.drawCircle(position.x, position.y, outerRadius, outerPaint)
        canvas.drawCircle(position.x, position.y, innerCircleRadius, innerPaint)
        canvas.drawCircle(position.x, position.y, backgroundCircleRadius, backgroundPaint)
    }
}

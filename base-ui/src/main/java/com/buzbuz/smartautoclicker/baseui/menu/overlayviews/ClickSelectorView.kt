/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View

import androidx.annotation.IntDef
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.ui.R

/**
 * Overlay view used as [com.buzbuz.smartautoclicker.baseui.dialog.OverlayMenuController.screenOverlayView] showing
 * the positions selected by the user.
 */
class ClickSelectorView(context: Context) : View(context) {

    /** Position of the first point selected. */
    var position1: PointF? = null
        private set
    /** Position of the second point selected. */
    var position2: PointF? = null
        private set
    /** Current selection step. */
    @SelectionIndex
    var selectionStep: Int = FIRST
        private set
    /** Listener upon touch events */
    var onTouchListener: (() -> Unit)? = null

    /** Paint drawing the outer circle of the [position1]. */
    private val outerFromPaint = Paint()
    /** Paint drawing the inner circle of the [position1]. */
    private val innerFromPaint = Paint()
    /** Paint drawing the outer circle of the [position2]. */
    private val outerToPaint = Paint()
    /** Paint drawing the inner circle of the [position2]. */
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
        context.obtainStyledAttributes(R.style.OverlaySelectorView_Click, R.styleable.ClickSelectorView).use { ta ->
            val thickness = ta.getDimensionPixelSize(R.styleable.ClickSelectorView_thickness, 4).toFloat()
            outerRadius = ta.getDimensionPixelSize(R.styleable.ClickSelectorView_radius, 30).toFloat()
            innerCircleRadius = ta.getDimensionPixelSize(R.styleable.ClickSelectorView_innerRadius, 4)
                .toFloat()
            val backgroundCircleStroke = outerRadius - (thickness / 2 + innerCircleRadius)
            backgroundCircleRadius = outerRadius - thickness / 2 - backgroundCircleStroke / 2

            outerFromPaint.apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = ta.getColor(R.styleable.ClickSelectorView_colorOutlinePrimary, Color.RED)
                strokeWidth = thickness
            }

            innerFromPaint.apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = ta.getColor(R.styleable.ClickSelectorView_colorInner, Color.WHITE)
            }

            outerToPaint.apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = ta.getColor(R.styleable.ClickSelectorView_colorOutlineSecondary, Color.GREEN)
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
                color = ta.getColor(R.styleable.ClickSelectorView_colorBackground, Color.TRANSPARENT)
                strokeWidth = backgroundCircleStroke
            }
        }
    }

    /**
     * Change the selection step.
     * In [FIRST], only [position1] is displayed. In [SECOND], only [position2] can be moved, but both are displayed.
     *
     * @param step the user selection step. Must be one of [SelectionIndex]
     */
    fun toSelectionStep(@SelectionIndex step: Int) {
        selectionStep = step
        if (selectionStep == FIRST) {
            position2 = null
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        position1?.apply {
            x = x.coerceIn(outerRadius, w.toFloat() - outerRadius)
            y = y.coerceIn(outerRadius, h.toFloat() - outerRadius)
        }
        position2?.apply {
            x = x.coerceIn(outerRadius, w.toFloat() - outerRadius)
            y = y.coerceIn(outerRadius, h.toFloat() - outerRadius)
        }
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        if (event.action != MotionEvent.ACTION_DOWN || event.action != MotionEvent.ACTION_MOVE) {
            super.onTouchEvent(event)
        }

        onTouchListener?.invoke()
        when (selectionStep) {
            FIRST -> position1 = PointF(event.x, event.y)
            SECOND -> position2 = PointF(event.x, event.y)
        }

        invalidate()
        return true
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

/** Defines the different points captured by a [ClickSelectorView]. */
@IntDef(FIRST, SECOND)
@Retention(AnnotationRetention.SOURCE)
annotation class SelectionIndex
/** The user is selecting the first click. */
const val FIRST = 1
/** The user is selecting the second click. */
const val SECOND = 2
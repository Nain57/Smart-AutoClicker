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

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.ExtendedValueAnimator
import kotlin.math.sqrt

class DumbActionBriefView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

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
    /** */
    private val linePaint = Paint()

    /** The thickness of the outer circle. */
    private var thickness: Float = 0f
    /** The circle radius. */
    private var outerRadius: Float = 0f
    /** The inner small circle radius. */
    private var innerCircleRadius: Float = 0F
    /** The radius of the transparent background between the inner and outer circle. */
    private var backgroundCircleRadius: Float = 0F

    init {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.DumbActionBriefView, R.attr.dumbActionBriefStyle, defStyleAttr).use { ta ->
                thickness = ta.getDimensionPixelSize(R.styleable.DumbActionBriefView_thickness, 4).toFloat()
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

                linePaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = innerFromPaint.color
                    strokeWidth = innerCircleRadius / 2f
                }

                backgroundPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = ta.getColor(R.styleable.DumbActionBriefView_colorBackground, Color.TRANSPARENT)
                }
            }
        }
    }

    private var animatedOuterRadius: Float = outerRadius
    private val outerRadiusAnimator: Animator = ValueAnimator.ofFloat(outerRadius * 0.75f, outerRadius).apply {
        duration = 750
        interpolator = AccelerateDecelerateInterpolator()
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            (it.animatedValue as Float).let { radius ->
                animatedOuterRadius = radius
                postInvalidate()
            }
        }
    }

    private var animatedSwipeProgressPosition: PointF = PointF()
    private val swipeProgressAnimator: Animator = ExtendedValueAnimator.ofFloat(0f, 1f).apply {
        startDelay = 500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { (it.animatedValue as Float).let { ratio -> updateSwipePosition(ratio) } }
    }

    private var dumbAction: DumbActionDescription? = null

    fun setDescription(dumbActionDescription: DumbActionDescription?) {
        dumbAction = dumbActionDescription

        when (dumbActionDescription) {
            is DumbActionDescription.Click -> {
                if (!outerRadiusAnimator.isStarted) outerRadiusAnimator.start()
                if (swipeProgressAnimator.isStarted) swipeProgressAnimator.end()
            }

            is DumbActionDescription.Swipe -> {
                if (!outerRadiusAnimator.isStarted) outerRadiusAnimator.start()
                swipeProgressAnimator.apply {
                    if (isStarted) cancel()
                    duration = dumbActionDescription.swipeDurationMs
                    start()
                }
            }

            is DumbActionDescription.Pause -> {
                if (outerRadiusAnimator.isStarted) outerRadiusAnimator.end()
                if (swipeProgressAnimator.isStarted) swipeProgressAnimator.end()
            }

            else -> Unit
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (val action = dumbAction) {
            is DumbActionDescription.Click -> {
                canvas.drawSelectorCircle(action.position, outerFromPaint, innerFromPaint)
            }

            is DumbActionDescription.Swipe -> {
                canvas.drawSelectorCircle(action.from, outerFromPaint, innerFromPaint)
                canvas.drawSelectorCircle(action.to, outerToPaint, innerToPaint)
                canvas.drawLine(action.from.x, action.from.y, action.to.x, action.to.y, linePaint)
                canvas.drawCircle(
                    animatedSwipeProgressPosition.x,
                    animatedSwipeProgressPosition.y,
                    innerCircleRadius * 2,
                    linePaint,
                )
            }

            else -> Unit
        }
    }

    /**
     * Draw the selector circle at the specified position.
     *
     * @param position the position of the circle selector.
     * @param outerPaint the paint used to draw the big circle.
     * @param innerPaint the paint used to draw the small inner circle.
     */
    private fun Canvas.drawSelectorCircle(position: PointF, outerPaint: Paint, innerPaint: Paint) {
        drawCircle(position.x, position.y, animatedOuterRadius, backgroundPaint)
        drawCircle(position.x, position.y, animatedOuterRadius, outerPaint)
        drawCircle(position.x, position.y, innerCircleRadius, innerPaint)
    }

    private fun updateSwipePosition(completionRatio: Float) {
        val dumbSwipe = (dumbAction as? DumbActionDescription.Swipe) ?: return

        var vx = dumbSwipe.to.x - dumbSwipe.from.x
        var vy = dumbSwipe.to.y - dumbSwipe.from.y
        val mag = sqrt(vx * vx + vy * vy)

        vx /= mag
        vy /= mag

        animatedSwipeProgressPosition.x = dumbSwipe.from.x + vx * (mag * completionRatio)
        animatedSwipeProgressPosition.y = dumbSwipe.from.y + vy * (mag * completionRatio)

        postInvalidate()
    }
}

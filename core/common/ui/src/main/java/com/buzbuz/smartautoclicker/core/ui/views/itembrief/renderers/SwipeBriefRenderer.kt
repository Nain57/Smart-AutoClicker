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
package com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefViewStyle

import kotlin.math.max
import kotlin.math.sqrt

internal class SwipeBriefRenderer(
    briefView: View,
    viewStyle: SwipeBriefRendererStyle,
) : ItemBriefRenderer<SwipeBriefRendererStyle>(briefView, viewStyle) {

    private val swipeProgressAnimator: ExtendedValueAnimator = ExtendedValueAnimator.ofFloat(0f, 1f).apply {
        startDelay = 250
        repeatDelay = 500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { updateSwipePosition((it.animatedValue as Float)) }
    }

    private val progressBorderPaint: Paint = Paint().apply {
        isAntiAlias = true
        this.style = Paint.Style.STROKE
        color = viewStyle.backgroundColor
        strokeWidth = viewStyle.innerRadiusPx * 0.75f
    }
    private val gradientBackgroundPaintFrom: Paint = Paint()
    private val gradientBackgroundPaintTo: Paint = Paint()

    private var animatedSwipeProgressPosition: PointF = PointF()
    private var positions: Pair<PointF?, PointF?> = Pair(null, null)

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is SwipeDescription) return

        if (swipeProgressAnimator.isStarted) swipeProgressAnimator.cancel()

        animatedSwipeProgressPosition = PointF()
        positions = description.from to description.to

        description.from?.let { from ->
            gradientBackgroundPaintFrom.shader = createRadialGradientShader(
                position = from,
                radius = viewStyle.outerRadiusPx * 1.75f,
                color = viewStyle.backgroundColor,
            )
        }

        description.to?.let { to ->
            gradientBackgroundPaintTo.shader = createRadialGradientShader(
                position = to,
                radius = viewStyle.outerRadiusPx * 1.75f,
                color = viewStyle.backgroundColor,
            )
        }

        if (animate && description.from != null && description.to != null) {
            swipeProgressAnimator.duration = max(description.swipeDurationMs, MINIMAL_ANIMATION_DURATION_MS)
            swipeProgressAnimator.start()
        }
        invalidate()
    }

    private fun updateSwipePosition(completionRatio: Float) {
        val (from, to) = positions
        from ?: return
        to ?: return

        var vx = to.x - from.x
        var vy = to.y - from.y
        val mag = sqrt(vx * vx + vy * vy)

        vx /= mag
        vy /= mag

        animatedSwipeProgressPosition.x = from.x + vx * (mag * completionRatio)
        animatedSwipeProgressPosition.y = from.y + vy * (mag * completionRatio)

        invalidate()
    }

    override fun onStop() {
        swipeProgressAnimator.cancel()
        animatedSwipeProgressPosition = PointF()
        positions = Pair(null, null)
    }

    override fun onDraw(canvas: Canvas) {
        val (from, to) = positions
        if (from == null && to == null) return

        from?.let {
            canvas.drawSelectorCircle(
                from,
                viewStyle.outerRadiusPx,
                viewStyle.outerFromPaint,
                viewStyle.innerFromPaint,
                gradientBackgroundPaintFrom,
            )
        }

        to?.let {
            canvas.drawSelectorCircle(
                to,
                viewStyle.outerRadiusPx,
                viewStyle.outerToPaint,
                viewStyle.innerToPaint,
                gradientBackgroundPaintTo,
            )
        }

        if (from != null && to != null) {
            canvas.drawLine(from.x, from.y, to.x, to.y, viewStyle.linePaint)

            canvas.drawCircle(
                animatedSwipeProgressPosition.x,
                animatedSwipeProgressPosition.y,
                viewStyle.innerRadiusPx * 2,
                viewStyle.linePaint,
            )
            canvas.drawCircle(
                animatedSwipeProgressPosition.x,
                animatedSwipeProgressPosition.y,
                viewStyle.innerRadiusPx * 2,
                progressBorderPaint,
            )
        }
    }

    private fun Canvas.drawSelectorCircle(
        position: PointF,
        outerRadius: Float,
        outerPaint: Paint,
        innerPaint: Paint,
        backgroundPaint: Paint,
    ) {
        drawCircle(position.x, position.y, outerRadius * 2f, backgroundPaint)
        drawCircle(position.x, position.y, outerRadius, outerPaint)
        drawCircle(position.x, position.y, viewStyle.innerRadiusPx, innerPaint)
    }
}

data class SwipeDescription(
    val swipeDurationMs: Long = MINIMAL_ANIMATION_DURATION_MS,
    val from: PointF? = null,
    val to: PointF? = null,
) : ItemBriefDescription

internal data class SwipeBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    val linePaint: Paint,
    val outerFromPaint: Paint,
    val outerToPaint: Paint,
    val innerFromPaint: Paint,
    val innerToPaint: Paint,
    val outerRadiusPx: Float,
    val innerRadiusPx: Float,
)

private const val MINIMAL_ANIMATION_DURATION_MS = 250L

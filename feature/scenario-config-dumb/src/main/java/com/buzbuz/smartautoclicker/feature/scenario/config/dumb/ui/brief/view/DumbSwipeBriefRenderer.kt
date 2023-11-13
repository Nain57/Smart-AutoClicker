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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.view

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import android.view.animation.LinearInterpolator

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.DumbActionDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.ExtendedValueAnimator

import kotlin.math.sqrt

internal class DumbSwipeBriefRenderer(
    briefView: View,
    style: DumbActionBriefViewStyle,
    viewInvalidator: () -> Unit,
) : DumbActionBriefRenderer(briefView, style, viewInvalidator) {

    private val swipeProgressAnimator: ExtendedValueAnimator = ExtendedValueAnimator.ofFloat(0f, 1f).apply {
        startDelay = 250
        repeatDelay = 500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { updateSwipePosition((it.animatedValue as Float)) }
    }

    private var animatedSwipeProgressPosition: PointF = PointF()
    private var positions: Pair<PointF, PointF>? = null

    override fun onNewDescription(description: DumbActionDescription) {
        if (description !is DumbActionDescription.Swipe) return

        swipeProgressAnimator.apply {
            if (isStarted) cancel()

            animatedSwipeProgressPosition = PointF()
            positions = description.from to description.to
            duration = description.swipeDurationMs
            start()
        }
    }

    private fun updateSwipePosition(completionRatio: Float) {
        val (from, to) = positions ?: return

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
        positions = null
    }

    override fun onDraw(canvas: Canvas) {
        positions?.let { (from, to) ->
            canvas.drawSelectorCircle(from, style.outerRadius, style.outerFromPaint, style.innerFromPaint)
            canvas.drawSelectorCircle(to, style.outerRadius, style.outerToPaint, style.innerToPaint)
            canvas.drawLine(from.x, from.y, to.x, to.y, style.linePaint)
            canvas.drawCircle(
                animatedSwipeProgressPosition.x,
                animatedSwipeProgressPosition.y,
                style.innerRadius * 2,
                style.linePaint,
            )
        }

    }

    /**
     * Draw the selector circle at the specified position.
     *
     * @param position the position of the circle selector.
     * @param outerPaint the paint used to draw the big circle.
     * @param innerPaint the paint used to draw the small inner circle.
     */
    private fun Canvas.drawSelectorCircle(position: PointF, outerRadius: Float, outerPaint: Paint, innerPaint: Paint) {
        drawCircle(position.x, position.y, outerRadius, style.backgroundPaint)
        drawCircle(position.x, position.y, outerRadius, outerPaint)
        drawCircle(position.x, position.y, style.innerRadius, innerPaint)
    }
}
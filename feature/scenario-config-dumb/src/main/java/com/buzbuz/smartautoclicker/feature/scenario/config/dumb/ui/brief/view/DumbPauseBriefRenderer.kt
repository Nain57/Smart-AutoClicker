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
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.DumbActionDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.ExtendedValueAnimator

internal class DumbPauseBriefRenderer(
    briefView: View,
    style: DumbActionBriefViewStyle,
    viewInvalidator: () -> Unit,
) : DumbActionBriefRenderer(briefView, style, viewInvalidator) {

    private val rotationAnimator: ExtendedValueAnimator =
        ExtendedValueAnimator.ofFloat(0f, 360f).apply {
            startDelay = 250
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            repeatDelay = 500
            addUpdateListener {
                animatedRotationAngleDegree = (it.animatedValue as Float)
                invalidate()
            }
        }

    private var viewCenter: PointF = PointF()
    private var baseHandPosition: RectF? = null
    private var animatedRotationAngleDegree: Float? = null

    override fun onNewDescription(description: DumbActionDescription) {
        if (description !is DumbActionDescription.Pause) return

        updateDisplayValues(briefView.width, briefView.height)
        rotationAnimator.apply {
            if (isStarted) cancel()

            animatedRotationAngleDegree = 0f
            duration = description.pauseDurationMs
            start()
        }
    }

    override fun onStop() {
        rotationAnimator.cancel()
        animatedRotationAngleDegree = null
        baseHandPosition = null
    }

    override fun onSizeChanged(w: Int, h: Int) {
        super.onSizeChanged(w, h)
        updateDisplayValues(w, h)
    }

    private fun updateDisplayValues(viewWidth: Int, viewHeight: Int) {
        viewCenter = PointF(
            viewWidth / 2f,
            viewHeight / 2f,
        )

        baseHandPosition = RectF(
            viewCenter.x - TIMER_HAND_HALF_WIDTH_PX,
            viewCenter.y - (style.outerRadius - (style.thickness * 1.5f)),
            viewCenter.x + TIMER_HAND_HALF_WIDTH_PX,
            viewCenter.y + TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val handPosition = baseHandPosition ?: return
        val handRotation = animatedRotationAngleDegree ?: return

        canvas.apply {
            drawCircle(viewCenter.x, viewCenter.y, style.outerRadius, style.backgroundPaint)
            drawCircle(viewCenter.x, viewCenter.y, style.outerRadius, style.outerFromPaint)

            save()
            rotate(handRotation, viewCenter.x, viewCenter.y)
            drawRoundRect(handPosition, 4f, 4f, style.linePaint)
            restore()
        }
    }
}

private const val TIMER_HAND_HALF_WIDTH_PX = 5f
private const val TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET = 5f
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

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator

import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator

import kotlin.math.max

internal class PauseBriefRenderer(
    briefView: View,
    style: ActionBriefViewStyle,
    viewInvalidator: () -> Unit,
) : ActionBriefRenderer(briefView, style, viewInvalidator) {

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

    private val gradientBackgroundPaint: Paint = Paint()

    private var viewCenter: PointF = PointF()
    private var baseHandPosition: RectF? = null
    private var animatedRotationAngleDegree: Float? = null

    override fun onNewDescription(description: ActionDescription) {
        if (description !is PauseDescription) return

        updateDisplayValues(briefView.width, briefView.height)
        rotationAnimator.apply {
            if (isStarted) cancel()

            animatedRotationAngleDegree = 0f
            duration = max(description.pauseDurationMs, MINIMAL_ANIMATION_DURATION_MS)
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
            viewCenter.y + TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET_PX,
        )

        gradientBackgroundPaint.shader = createRadialGradientShader(
            position = viewCenter,
            radius = style.outerRadius * 1.75f,
            color = style.backgroundColor,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val handPosition = baseHandPosition ?: return
        val handRotation = animatedRotationAngleDegree ?: return

        canvas.apply {
            drawCircle(viewCenter.x, viewCenter.y, style.outerRadius * 2f, gradientBackgroundPaint)
            drawCircle(viewCenter.x, viewCenter.y, style.outerRadius, style.outerFromPaint)

            save()
            rotate(handRotation, viewCenter.x, viewCenter.y)
            drawRoundRect(handPosition, 4f, 4f, style.linePaint)
            restore()
        }
    }
}

data class PauseDescription(
    val pauseDurationMs: Long = MINIMAL_ANIMATION_DURATION_MS,
) : ActionDescription

private const val MINIMAL_ANIMATION_DURATION_MS = 500L
private const val TIMER_HAND_HALF_WIDTH_PX = 5f
private const val TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET_PX = 5f
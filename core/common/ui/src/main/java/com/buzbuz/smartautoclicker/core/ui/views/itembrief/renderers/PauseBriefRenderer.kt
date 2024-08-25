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
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

import kotlin.math.max

internal class PauseBriefRenderer(
    briefView: View,
    viewStyle: PauseBriefRendererStyle,
) : ItemBriefRenderer<PauseBriefRendererStyle>(briefView, viewStyle) {

    private val rotationAnimator: ExtendedValueAnimator =
        ExtendedValueAnimator.ofFloat(0f, 360f).apply {
            startDelay = 250
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            repeatDelay = 500
            addUpdateListener {
                animatedRotationAngleDegree = (it.animatedValue as Float)
                invalidateView()
            }
        }

    private val gradientBackgroundPaint: Paint = Paint()

    private var viewCenter: PointF = PointF()
    private var baseHandPosition: RectF? = null
    private var animatedRotationAngleDegree: Float? = null
    private var animationDurationMs: Long? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is PauseDescription) return

        if (rotationAnimator.isStarted) rotationAnimator.cancel()

        val animDurationMs = max(description.pauseDurationMs, MINIMAL_ANIMATION_DURATION_MS)
        animationDurationMs = animDurationMs
        animatedRotationAngleDegree = 0f

        if (animate) {
            rotationAnimator.duration = max(animDurationMs, MINIMAL_ANIMATION_DURATION_MS)
            rotationAnimator.start()
        }
    }

    override fun onInvalidate() {
        val viewSize = getViewSize()
        viewCenter = PointF(
            viewSize.x / 2f,
            viewSize.y / 2f,
        )

        baseHandPosition = RectF(
            viewCenter.x - TIMER_HAND_HALF_WIDTH_PX,
            viewCenter.y - (viewStyle.outerRadiusPx - (viewStyle.thicknessPx * 1.5f)),
            viewCenter.x + TIMER_HAND_HALF_WIDTH_PX,
            viewCenter.y + TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET_PX,
        )

        gradientBackgroundPaint.shader = createRadialGradientShader(
            position = viewCenter,
            radius = viewStyle.outerRadiusPx * 1.75f,
            color = viewStyle.backgroundColor,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val handPosition = baseHandPosition ?: return
        val handRotation = animatedRotationAngleDegree ?: return

        canvas.apply {
            drawCircle(viewCenter.x, viewCenter.y, viewStyle.outerRadiusPx * 2f, gradientBackgroundPaint)
            drawCircle(viewCenter.x, viewCenter.y, viewStyle.outerRadiusPx, viewStyle.outerPaint)

            save()
            rotate(handRotation, viewCenter.x, viewCenter.y)
            drawRoundRect(handPosition, 4f, 4f, viewStyle.linePaint)
            restore()
        }
    }

    override fun onStop() {
        rotationAnimator.cancel()
        animatedRotationAngleDegree = null
        baseHandPosition = null
        animationDurationMs = null
    }
}

data class PauseDescription(
    val pauseDurationMs: Long = MINIMAL_ANIMATION_DURATION_MS,
) : ItemBriefDescription

internal data class PauseBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    val outerPaint: Paint,
    val linePaint: Paint,
    val thicknessPx: Float,
    val outerRadiusPx: Float,
    val innerRadiusPx: Float,
)

private const val MINIMAL_ANIMATION_DURATION_MS = 500L
private const val TIMER_HAND_HALF_WIDTH_PX = 5f
private const val TIMER_HAND_ROTATION_CENTER_BOTTOM_OFFSET_PX = 5f
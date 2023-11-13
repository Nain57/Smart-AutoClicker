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
import android.view.animation.AccelerateDecelerateInterpolator

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.DumbActionDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.ExtendedValueAnimator

internal class DumbClickBriefRenderer(
    briefView: View,
    style: DumbActionBriefViewStyle,
    viewInvalidator: () -> Unit,
) : DumbActionBriefRenderer(briefView, style, viewInvalidator) {

    private val outerRadiusAnimator: ExtendedValueAnimator =
        ExtendedValueAnimator.ofFloat(style.outerRadius, style.outerRadius * 0.75f).apply {
            startDelay = 250
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            repeatDelay = 500
            addUpdateListener {
                animatedOuterRadius = (it.animatedValue as Float)
                invalidate()
            }
        }

    private val gradientBackgroundPaint: Paint = Paint()

    private var animatedOuterRadius: Float = style.outerRadius
    private var position: PointF? = null

    override fun onNewDescription(description: DumbActionDescription) {
        if (description !is DumbActionDescription.Click) return

        outerRadiusAnimator.apply {
            if (isStarted) cancel()

            animatedOuterRadius = style.outerRadius
            position = description.position
            gradientBackgroundPaint.shader = createRadialGradientShader(
                position = description.position,
                radius = style.outerRadius * 1.75f,
                color = style.backgroundColor,
            )

            reverseDelay = description.pressDurationMs
            start()
        }
    }

    override fun onStop() {
        outerRadiusAnimator.cancel()
        animatedOuterRadius = style.outerRadius
        position = null
    }

    override fun onDraw(canvas: Canvas) {
        position?.let { pos ->
            canvas.drawCircle(pos.x, pos.y, animatedOuterRadius * 2f, gradientBackgroundPaint)
            canvas.drawCircle(pos.x, pos.y, animatedOuterRadius, style.outerFromPaint)
            canvas.drawCircle(pos.x, pos.y, style.innerRadius, style.innerFromPaint)
        }
    }
}
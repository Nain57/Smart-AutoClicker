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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View

internal class DefaultBriefRenderer(
    briefView: View,
    style: ActionBriefViewStyle,
    viewInvalidator: () -> Unit,
) : ActionBriefRenderer(briefView, style, viewInvalidator) {

    private val gradientBackgroundPaint: Paint = Paint()

    private var viewCenter = PointF(0f, 0f)
    private var iconDrawable: Drawable? = null

    override fun onNewDescription(description: ActionDescription, animate: Boolean) {
        if (description !is DefaultDescription) return

        iconDrawable = description.icon?.mutate()?.apply {
            setTint(Color.WHITE)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int) {
        super.onSizeChanged(w, h)
        invalidate()
    }

    override fun invalidate() {
        viewCenter = PointF(briefView.width / 2f, briefView.height / 2f)

        iconDrawable?.bounds = Rect(
            viewCenter.x.toInt() - style.outerRadius.toInt(),
            viewCenter.y.toInt() - style.outerRadius.toInt(),
            viewCenter.x.toInt() + style.outerRadius.toInt(),
            viewCenter.y.toInt() + style.outerRadius.toInt(),
        )

        gradientBackgroundPaint.shader = createRadialGradientShader(
            position = viewCenter,
            radius = style.outerRadius * 1.75f,
            color = style.backgroundColor,
        )

        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            drawCircle(viewCenter.x, viewCenter.y, style.outerRadius * 2f, gradientBackgroundPaint)
            iconDrawable?.draw(canvas)
        }
    }

    override fun onStop() = Unit
}

data class DefaultDescription(
    val icon: Drawable? = null,
) : ActionDescription
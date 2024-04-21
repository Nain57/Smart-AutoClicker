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
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

import androidx.annotation.ColorInt

internal abstract class ActionBriefRenderer(
    protected val briefView: View,
    protected val style: ActionBriefViewStyle,
    private val viewInvalidator: () -> Unit,
) {
    abstract fun onNewDescription(description: ActionDescription)
    open fun onSizeChanged(w: Int, h: Int) = Unit
    abstract fun onDraw(canvas: Canvas)
    abstract fun onStop()
    protected fun invalidate() { viewInvalidator() }

    protected fun createRadialGradientShader(position: PointF, radius: Float, @ColorInt color: Int): Shader =
        RadialGradient(
            position.x,
            position.y,
            radius,
            color,
            color.setAlpha(0),
            Shader.TileMode.CLAMP,
        )

    @ColorInt
    private fun Int.setAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
}
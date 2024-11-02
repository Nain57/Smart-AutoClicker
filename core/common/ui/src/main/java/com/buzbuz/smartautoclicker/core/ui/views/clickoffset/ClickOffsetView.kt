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
package com.buzbuz.smartautoclicker.core.ui.views.clickoffset

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.ui.R

class ClickOffsetView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    private val viewStyle = context
        .obtainStyledAttributes(null, R.styleable.ClickOffsetView, R.attr.clickOffsetStyle, 0)
        .use { ta -> ta.getClickOffsetStyle()}

    private val gradiantBgRadiusPx: Float = viewStyle.radiusPx * 2
    private val gradientBackgroundPaint: Paint = Paint()

    private val innerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.innerColor
    }

    private val outerCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = viewStyle.outerColor
        strokeWidth = viewStyle.thicknessPx
    }

    private var offsetPosition: PointF? = null

    var viewSize: Size = Size(0, 0)
        set(value) {
            field = value
            invalidate()
        }

    var offsetValue: PointF? = null
        set(value) {
            field = value
            invalidate()
        }

    var onOffsetChangedListener: ((PointF) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewSize = Size(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        offsetValue = PointF(
            event.x - (viewSize.width / 2),
            event.y - (viewSize.height / 2),
        )
        onOffsetChangedListener?.invoke(offsetValue!!)

        return true
    }

    override fun invalidate() {
        val offset = offsetValue ?: return

        offsetPosition = PointF(
            offset.x + (viewSize.width / 2),
            offset.y + (viewSize.height / 2),
        )

        offsetPosition?.let { position ->
            gradientBackgroundPaint.shader = createRadialGradientShader(
                position = position,
                radius = gradiantBgRadiusPx,
                color = viewStyle.backgroundColor,
            )
        }

        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        offsetPosition?.let { offsetPos ->
            canvas.drawCircle(offsetPos.x, offsetPos.y, gradiantBgRadiusPx, gradientBackgroundPaint)
            canvas.drawCircle(offsetPos.x, offsetPos.y, viewStyle.radiusPx, outerCirclePaint)
            canvas.drawCircle(offsetPos.x, offsetPos.y, viewStyle.innerRadiusPx, innerCirclePaint)
        }
    }

    private fun createRadialGradientShader(position: PointF, radius: Float, @ColorInt color: Int): Shader =
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

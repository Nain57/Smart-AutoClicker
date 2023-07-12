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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TutorialFullScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint: Paint = Paint()
    private val drawPath: Path =
        Path().apply {
            fillType = Path.FillType.WINDING
        }

    var expectedViewPosition: Rect? = null
        set(value) {
            if (value == expectedViewPosition) return

            field = value
            invalidate()
        }

    var onMonitoredViewClickedListener: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) invalidate()
    }

    override fun invalidate() {
        super.invalidate()

        measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        backgroundPaint.shader = LinearGradient(
            0f, 0f,
            0f, height.toFloat(),
            intArrayOf(Color.BLACK, 0x77000000, Color.TRANSPARENT),
            floatArrayOf(0f , 0.5f, 0.9f),
            Shader.TileMode.CLAMP,
        )

        drawPath.apply {
            reset()
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            expectedViewPosition?.let { position ->
                addCircle(position.left.toFloat(), position.top.toFloat(), height * 2f, Path.Direction.CCW)
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        canvas.drawPath(drawPath, backgroundPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        return if (event.action == MotionEvent.ACTION_UP && event.isOnMonitoredView()) {
            onMonitoredViewClickedListener?.invoke()
            true
        } else super.onTouchEvent(event)
    }

    private fun MotionEvent.isOnMonitoredView(): Boolean =
        expectedViewPosition?.contains(x.toInt(), y.toInt()) ?: false
}
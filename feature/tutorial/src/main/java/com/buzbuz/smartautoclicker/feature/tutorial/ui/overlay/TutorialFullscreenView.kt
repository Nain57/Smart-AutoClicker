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
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import androidx.core.graphics.toRectF
import com.buzbuz.smartautoclicker.core.base.extensions.center
import com.buzbuz.smartautoclicker.core.base.extensions.scale

import com.buzbuz.smartautoclicker.feature.tutorial.R

class TutorialFullscreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint: Paint = Paint().apply {
        isAntiAlias = true
        color = context.getColor(R.color.tutorial_overlay_background)
    }
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
        drawPath.apply {
            reset()
            addBackgroundRect()
            addExpectedViewHole()
        }
    }

    // SDK 34 defines MotionEvents as NonNull. But previous bad experiences with the same case on
    // SDK 33 gave me trust issues
    @Suppress("NOTHING_TO_OVERRIDE", "ACCIDENTAL_OVERRIDE")
    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        super.onDraw(canvas)

        canvas.drawPath(drawPath, backgroundPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        return if (event.action == MotionEvent.ACTION_DOWN && event.isOnMonitoredView()) {
            onMonitoredViewClickedListener?.invoke()
            true
        } else super.onTouchEvent(event)
    }

    private fun MotionEvent.isOnMonitoredView(): Boolean =
        expectedViewPosition?.contains(x.toInt(), y.toInt()) ?: false

    private fun Path.addBackgroundRect(): Unit =
        addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)

    private fun Path.addExpectedViewHole() {
        val viewPosition = expectedViewPosition ?: return

        if (viewPosition.width().toDouble() in (0.5 * viewPosition.height())..(1.5 * viewPosition.height())) {
            addCircle(
                /* x */ viewPosition.centerX().toFloat(),
                /* y */ viewPosition.centerY().toFloat(),
                /* radius */ viewPosition.height().toFloat() * 0.8f,
                /* dir */ Path.Direction.CCW,
            )
        } else {
            addRoundRect(
                /* rect */ viewPosition.toRectF().apply {
                    scale(1.05f, center())
                },
                /* rx */ 25f,
                /* ry */ 25f,
                /* dir */ Path.Direction.CCW,
            )
        }
    }
}
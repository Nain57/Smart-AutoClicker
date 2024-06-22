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
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.ui.R


class GestureRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val viewStyle = context
        .obtainStyledAttributes(null, R.styleable.GestureRecordView, R.attr.gestureRecordStyle, 0)
        .use { ta -> ta.getGestureRecorderStyle()}

    private val recordPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.color
    }

    private val drawingRectList: MutableList<Rect> = mutableListOf()

    private val gestureRecorder = GestureRecorder { gesture, isFinished ->
        gestureCaptureListener?.invoke(gesture, isFinished)
    }

    var gestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)? = null

    fun clearAndHide() {
        visibility = GONE
        gestureRecorder.clearCapture()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    override fun invalidate() {
        drawingRectList.apply {
            clear()

            // Top Left
            add(Rect(0, 0, viewStyle.lengthPx, viewStyle.thicknessPx))
            add(Rect(0, 0, viewStyle.thicknessPx, viewStyle.lengthPx))

            // Top Right
            add(Rect(width - viewStyle.lengthPx, 0, width, viewStyle.thicknessPx))
            add(Rect(width - viewStyle.thicknessPx, 0, width, viewStyle.lengthPx))

            // Bottom Left
            add(Rect(0, height - viewStyle.thicknessPx, viewStyle.lengthPx, height))
            add(Rect(0, height - viewStyle.lengthPx, viewStyle.thicknessPx, height))

            // Bottom Right
            add(Rect(width - viewStyle.lengthPx, height - viewStyle.thicknessPx, width, height))
            add(Rect(width - viewStyle.thicknessPx, height - viewStyle.lengthPx, width, height))
        }
        super.invalidate()
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureRecorder.processEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawingRectList.forEach { rect ->
            canvas.drawRect(rect, recordPaint)
        }
    }
}
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
        style = Paint.Style.STROKE
        color = viewStyle.color
        strokeWidth = viewStyle.thicknessPx.toFloat()
    }

    private var viewBorderRect: Rect? = null

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
        viewBorderRect = Rect(0, 0, width, height)
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureRecorder.processEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        viewBorderRect?.let { borderRect ->
            canvas.drawRect(borderRect, recordPaint)
        }
    }
}
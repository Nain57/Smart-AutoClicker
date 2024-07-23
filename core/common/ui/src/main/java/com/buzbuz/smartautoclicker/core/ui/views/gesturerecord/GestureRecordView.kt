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
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use

import com.buzbuz.smartautoclicker.core.display.Corner
import com.buzbuz.smartautoclicker.core.display.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.DisplayRoundedCorner
import com.buzbuz.smartautoclicker.core.display.di.DisplayEntryPoint
import com.buzbuz.smartautoclicker.core.display.haveRoundedCorner
import com.buzbuz.smartautoclicker.core.ui.R

import dagger.hilt.EntryPoints


class GestureRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val displayConfigManager: DisplayConfigManager by lazy {
        EntryPoints.get(context.applicationContext, DisplayEntryPoint::class.java)
            .displayMetrics()
    }

    private val viewStyle = context
        .obtainStyledAttributes(null, R.styleable.GestureRecordView, R.attr.gestureRecordStyle, 0)
        .use { ta -> ta.getGestureRecorderStyle()}

    private val recordStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = viewStyle.color
        strokeWidth = viewStyle.thicknessPx.toFloat() * 2
    }

    private val recordFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.color
    }

    private val borders: MutableList<Rect> = mutableListOf()
    private val corners: MutableList<Triple<RectF, Float, Float>> = mutableListOf()

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
        borders.clear()
        corners.clear()

        val displayConfig: DisplayConfig = displayConfigManager.displayConfig
        if (displayConfig.haveRoundedCorner()) {
            borders.addRoundedDisplayBorderLines(displayConfig)
            corners.addRoundedCorners(displayConfig)
        } else {
            borders.addDisplayRectangleBorder(displayConfig)
        }

        super.invalidate()
    }

    private fun MutableList<Rect>.addDisplayRectangleBorder(displayConfig: DisplayConfig) {
        add(Rect(0, 0, displayConfig.sizePx.x, displayConfig.sizePx.y))
    }

    private fun MutableList<Rect>.addRoundedDisplayBorderLines(displayConfig: DisplayConfig) {
        // Left
        add(Rect(
            0,
            displayConfig.roundedCorners[Corner.TOP_LEFT]?.centerPx?.y ?: 0,
            viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.centerPx?.y ?: displayConfig.sizePx.y,
        ))
        // Top
        add(Rect(
            displayConfig.roundedCorners[Corner.TOP_LEFT]?.centerPx?.x ?: 0,
            0,
            displayConfig.roundedCorners[Corner.TOP_RIGHT]?.centerPx?.x ?: displayConfig.sizePx.x,
            viewStyle.thicknessPx,
        ))
        // Right
        add(Rect(
            displayConfig.sizePx.x - viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.TOP_RIGHT]?.centerPx?.y ?: 0,
            displayConfig.sizePx.x,
            displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.centerPx?.y ?: displayConfig.sizePx.y,
        ))
        // Bottom
        add(Rect(
            displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.centerPx?.x ?: 0,
            displayConfig.sizePx.y - viewStyle.thicknessPx,
            displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.centerPx?.x ?: displayConfig.sizePx.y,
            displayConfig.sizePx.y,
        ))
    }

    private fun MutableList<Triple<RectF, Float, Float>>.addRoundedCorners(displayConfig: DisplayConfig) {
        val width = displayConfig.sizePx.x.toFloat()
        val height = displayConfig.sizePx.y.toFloat()

        displayConfig.roundedCorners[Corner.TOP_LEFT]?.let { topLeftCorner ->
            add(Triple(
                RectF(0f, 0f, topLeftCorner.getCornerOffsetLeft(), topLeftCorner.getCornerOffsetTop()),
                180f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.TOP_RIGHT]?.let { topRightCorner ->
            add(Triple(
                RectF(width - topRightCorner.getCornerOffsetRight(width), 0f,
                    width, topRightCorner.getCornerOffsetTop()),
                270f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]?.let { bottomRightCorner ->
            add(Triple(
                RectF(width - bottomRightCorner.getCornerOffsetRight(width),
                    height - bottomRightCorner.getCornerOffsetBottom(height), width, height),
                0f, 90f,
            ))
        }
        displayConfig.roundedCorners[Corner.BOTTOM_LEFT]?.let { bottomLeftCorner ->
            add(Triple(
                RectF(0f, height - bottomLeftCorner.getCornerOffsetBottom(height),
                    bottomLeftCorner.getCornerOffsetLeft(), height),
                90f, 90f,
            ))
        }
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureRecorder.processEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Square screen use case
        if (corners.isEmpty() && borders.size == 1) {
            canvas.drawRect(borders[0], recordStrokePaint)
            return
        }

        borders.forEach { border ->
            canvas.drawRect(border, recordFillPaint)
        }
        corners.forEach { (area, startAngle, sweepAngle) ->
            canvas.drawArc(area, startAngle, sweepAngle, false, recordStrokePaint)
        }
    }

    private fun DisplayRoundedCorner.getCornerOffsetLeft(): Float =
        centerPx.x * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetTop(): Float =
        centerPx.y * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetRight(width: Float): Float =
        (width - centerPx.x) * 2f + viewStyle.thicknessPx * 4f
    private fun DisplayRoundedCorner.getCornerOffsetBottom(height: Float): Float =
        (height - centerPx.y) * 2f + viewStyle.thicknessPx * 4f
}

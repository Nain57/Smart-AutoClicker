/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

import com.buzbuz.smartautoclicker.baseui.ScreenMetrics
import com.buzbuz.smartautoclicker.extensions.scale
import com.buzbuz.smartautoclicker.extensions.translate
import com.buzbuz.smartautoclicker.ui.R

import kotlin.math.max
import kotlin.math.min

/**
 * Displays a bitmap and handles moving/zooming on it.
 *
 * @param context the Android context.
 * @param styledAttrs the styled attributes of the [ConditionSelectorView]
 * @param screenMetrics provides information about current display.
 * @param viewInvalidator calls invalidate on the view hosting this component.
 */
internal class Capture(
    context: Context,
    styledAttrs: TypedArray,
    screenMetrics: ScreenMetrics,
    viewInvalidator: () -> Unit,
): SelectorViewComponent(screenMetrics, viewInvalidator) {

    /** Listener for the [gestureDetector] handling the move gesture. */
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            translateCapture(-distanceX, -distanceY)
            return true
        }
    }
    /** Gesture detector for the moving of the capture. */
    private val gestureDetector = GestureDetector(context, gestureListener)

    /** Listener for the [scaleGestureDetector] handling the pinch scaling gesture. */
    private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        /** Focus position of the gesture to be used as scaling pivot. */
        private val scaleFocus = PointF()

        override fun onScale(scaleGestureDetector: ScaleGestureDetector?): Boolean {
            scaleGestureDetector?: return false

            scaleFocus.apply {
                x = scaleGestureDetector.focusX
                y = scaleGestureDetector.focusY
            }
            setZoomLevel(zoomLevel * scaleGestureDetector.scaleFactor, scaleFocus)

            return true
        }
    }
    /** Gesture detector for the scaling of the capture. */
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

    /** The minimum zoom value. */
    private val zoomMin: Float = styledAttrs.getFloat(
        R.styleable.ConditionSelectorView_minimumZoomValue,
        DEFAULT_ZOOM_MINIMUM
    )
    /** The maximum zoom value. */
    private val zoomMax: Float = styledAttrs.getFloat(
        R.styleable.ConditionSelectorView_maximumZoomValue,
        DEFAULT_ZOOM_MAXIMUM
    )
    /** The current zoom level*/
    var zoomLevel = 1f
        private set

    /** The current area where the capture is displayed. It can be bigger than the screen when zoomed. */
    val captureArea = RectF(0f, 0f, maxArea.width(), maxArea.height())
    /** The drawable for the screen capture. */
    var screenCapture: BitmapDrawable? = null
    /** Listener upon the [captureArea] changes. */
    var onCapturePositionChanged: ((RectF) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = gestureDetector.onTouchEvent(event)
        handled = handled or scaleGestureDetector.onTouchEvent(event)
        return handled
    }

    /**
     * Translate the capture.
     *
     * @param translateX the horizontal value for the translation.
     * @param translateY the vertical value for the translation.
     */
    private fun translateCapture(translateX: Float, translateY: Float) {
        // Verify if the translation isn't moving the capture too far away and correct the value to avoid to "lost" the
        // capture.
        var inboundsTranslateX = translateX
        var inboundsTranslateY = translateY

        val horizontalMargin = maxArea.width() * 0.2f
        val verticalMargin = maxArea.height() * 0.2f

        if (translateX > 0 && captureArea.left + translateX > maxArea.right - horizontalMargin) {
            inboundsTranslateX = min(0f, translateX)
        } else if (translateX < 0 && captureArea.right + translateX < maxArea.left + horizontalMargin) {
            inboundsTranslateX = max(0f, translateX)
        }
        if (translateY > 0 && captureArea.top + translateY > maxArea.bottom - verticalMargin) {
            inboundsTranslateY= min(0f, translateY)
        } else if (translateY < 0 && captureArea.bottom - translateY < maxArea.top + verticalMargin) {
            inboundsTranslateY = max(0f, translateY)
        }

        // Translate safely
        captureArea.translate(inboundsTranslateX, inboundsTranslateY)

        onCapturePositionChanged?.invoke(captureArea)
        invalidate()
    }

    /**
     * Set the zoom level for the capture.
     *
     * @param newLevel the new zoom level.
     * @param zoomPivot the pivot for the scaling of the capture. Default value is the center of the capture.
     */
    fun setZoomLevel(newLevel: Float, zoomPivot: PointF = PointF(captureArea.centerX(), captureArea.centerY())) {
        val newZoom = newLevel.coerceIn(zoomMin, zoomMax)
        if (zoomLevel == newZoom) {
            return
        }

        val pivot = if (newZoom < 1) {
            PointF(maxArea.centerX(), maxArea.centerY())
        } else {
            zoomPivot
        }
        captureArea.scale(newZoom / zoomLevel, pivot)
        zoomLevel = newZoom

        onCapturePositionChanged?.invoke(captureArea)
        invalidate()
    }

    override fun onViewSizeChanged(w: Int, h: Int) {
        super.onViewSizeChanged(w, h)
        if (screenCapture == null) {
            captureArea.apply {
                left = 0f
                top = 0f
                right = maxArea.width()
                bottom = maxArea.height()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        screenCapture?.apply {
            canvas.drawColor(Color.BLACK)
            setBounds(
                captureArea.left.toInt(),
                captureArea.top.toInt(),
                captureArea.right.toInt(),
                captureArea.bottom.toInt()
            )
            draw(canvas)
        }
    }

    override fun onReset() {
        screenCapture = null
        setZoomLevel(1f)
        captureArea.apply {
            left = 0f
            top = 0f
            right = maxArea.width()
            bottom = maxArea.height()
        }

        onCapturePositionChanged?.invoke(captureArea)
    }
}

/** The default minimum zoom value. */
private const val DEFAULT_ZOOM_MINIMUM = 0.8f
/** The default maximum zoom value. */
private const val DEFAULT_ZOOM_MAXIMUM = 3f
/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.GestureDetector
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent

import androidx.annotation.ColorInt
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import com.buzbuz.smartautoclicker.core.base.extensions.translate

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.GestureType
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.MoveSelector
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeBottom
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeLeft
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeRight
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ResizeTop
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ZoomCapture

import kotlin.math.max
import kotlin.math.min

/**
 * Displays a rectangle selector and handles moving/resizing on it.
 *
 * @param context the Android Context.
 * @param selectorStyle the style for this component.
 * @param viewInvalidator calls invalidate on the view hosting this component.
 */
internal class SelectorComponent(
    context: Context,
    selectorStyle: SelectorComponentStyle,
    viewInvalidator: () -> Unit,
): ViewComponent(selectorStyle, viewInvalidator) {

    /** Listener for the [gestureDetector] handling the move/resize gesture. */
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return onNewDownEvent(e.x, e.y)
        }

        // SDK 33 defines MotionEvents as NonNull, but sometimes they are null and this can leads on a crash on
        // some devices. Force the two parameters as nullable and remove compiler warnings.
        @Suppress("NOTHING_TO_OVERRIDE", "ACCIDENTAL_OVERRIDE")
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (currentGesture == null) return false
            onTranslateSelector(-distanceX, -distanceY)
            return true
        }
    }
    /** Gesture detector for the move/resize of the capture. */
    private val gestureDetector = GestureDetector(context, gestureListener)
    /** The type of gesture currently executed by the user. */
    var currentGesture: GestureType? = null
        private set

    /** Default size of the selector area. */
    private val selectorDefaultSize = selectorStyle.selectorDefaultSize
    /** The size of the selector handle. */
    private val handleSize = selectorStyle.handleSize
    /** The size of the handle within the view. */
    private val innerHandleSize = handleSize / INNER_HANDLE_RATIO
    /** Difference between the center of the selector and its inner content. */
    private var selectorAreaOffset: Int = selectorStyle.selectorAreaOffset
    /** The radius of the corner for the selector. */
    private val cornerRadius = selectorStyle.cornerRadius

    /** Paint drawing the selector. */
    private val selectorPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = selectorStyle.selectorThickness
        color = selectorStyle.selectorColor
        alpha = 0
    }
    /** Paint for the background of the selector. */
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = selectorStyle.selectorBackgroundColor
    }
    /** The transparency of the background color of the selector. */
    private val selectorBackgroundAlpha: Int = backgroundPaint.color.shr(24)
    /** The path for drawing the selector transparent background and content. */
    private val selectorDrawingPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }
    /** The minimum size of the selector. Size is relative to the [maxArea]. */
    private val selectorMinimumSize = PointF()
    /** The area where the selector should be drawn. */
    private val selectorArea = RectF()
    /** Area within the selector that represents the zone to be capture to creates a event condition. */
    val selectedArea = RectF()

    /** The area requested via [setDefaultSelectionArea]. */
    private var defaultSelectionArea: RectF? = null
    /** The minimal size allowed for the selector requested via [setDefaultSelectionArea]. */
    private var defaultMinimumArea: RectF? = null

    /** Listener upon the position of the selector. */
    var onSelectorPositionChanged: ((Rect) -> Unit)? = null

    /** Transparency of the background. */
    var backgroundAlpha: Int = selectorBackgroundAlpha
        set(value) {
            field = value
            backgroundPaint.alpha = value
            invalidate()
        }
    /** Transparency of the selector. */
    var selectorAlpha: Int = 255
        set(value) {
            field = value
            selectorPaint.alpha = value
            invalidate()
        }

    /**
     * Set the default area selected.
     * @param area the selected area.
     * @param minimumArea the minimal size of the area selectable.
     * @return true if it wasn't defined, false if it already was.
     */
    fun setDefaultSelectionArea(area: Rect, minimumArea: Rect): Boolean {
        val result = defaultSelectionArea == null
        defaultSelectionArea = area.toRectF()
        defaultMinimumArea = minimumArea.toRectF()
        resetSelectorPosition(notify = false)
        invalidate()
        return result
    }

    /**
     * Get the part of the capture that is currently selected within the selector.
     *
     * @param captureArea the current area for the capture.
     * @param zoomLevel the current zoom level on the capture.
     *
     * @return the area of the selector relative to the screen size.
     */
    fun getSelectionArea(captureArea: RectF, zoomLevel: Float): Rect {
        return RectF(selectedArea).run {
            intersect(captureArea)

            val invertedZoomLevel = 1 / zoomLevel

            left = (left - captureArea.left) * invertedZoomLevel
            top = (top - captureArea.top) * invertedZoomLevel
            right = (right - captureArea.left) * invertedZoomLevel
            bottom = (bottom - captureArea.top) * invertedZoomLevel

            intersect(maxArea)

            toRect()
        }
    }

    override fun onViewSizeChanged(w: Int, h: Int) {
        super.onViewSizeChanged(w, h)
        resetSelectorPosition()
    }

    /** Reset the selector to its original position. */
    private fun resetSelectorPosition(notify: Boolean = true) {
        val requestedArea = defaultSelectionArea
        selectorArea.apply {
            if (requestedArea != null) {
                left = requestedArea.left - selectorAreaOffset
                top = requestedArea.top - selectorAreaOffset
                right = requestedArea.right + selectorAreaOffset
                bottom = requestedArea.bottom + selectorAreaOffset
            } else {
                left = maxArea.centerX() - selectorDefaultSize.x
                top = maxArea.centerY() - selectorDefaultSize.y
                right = maxArea.centerX() + selectorDefaultSize.x
                bottom = maxArea.centerY() + selectorDefaultSize.y
            }
        }

        val minimumArea = defaultMinimumArea
        selectorMinimumSize.apply {
            if (minimumArea != null) {
                x = minimumArea.width()
                y = minimumArea.height()
            } else {
                x = maxArea.width() * SELECTOR_MINIMUM_WIDTH_RATIO
                y = maxArea.height() * SELECTOR_MINIMUM_HEIGHT_RATIO
            }
        }

        verifyBounds()
        if (notify) onSelectorPositionChanged?.invoke(selectorArea.toRect())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = gestureDetector.onTouchEvent(event)
        if (!consumed && currentGesture != null) {
            consumed = true
        }

        if  (event.action == ACTION_UP) {
            currentGesture = null
            consumed = true
        }

        return consumed
    }

    private fun onNewDownEvent(eventX: Float, eventY: Float): Boolean {
        currentGesture = when {
            ResizeLeft.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                ResizeLeft
            ResizeTop.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                ResizeTop
            ResizeRight.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                ResizeRight
            ResizeBottom.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                ResizeBottom
            MoveSelector.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                MoveSelector
            else -> null
        }

        return currentGesture != null
    }

    /** The result of the move gesture. Kept here to avoid instantiation at each touch event. */
    private val moveResult = RectF()

    /**
     * Translate the selector.
     *
     * @param translateX the horizontal value for the translation.
     * @param translateY the vertical value for the translation.
     */
    private fun onTranslateSelector(translateX: Float, translateY: Float) {
        currentGesture?.let {
            when (it) {
                ResizeLeft -> selectorArea.left = min(
                    selectorArea.left + translateX,
                    selectorArea.right - selectorMinimumSize.x
                )
                ResizeTop -> selectorArea.top = min(
                    selectorArea.top + translateY,
                    selectorArea.bottom - selectorMinimumSize.y
                )
                ResizeRight -> selectorArea.right = max(
                    selectorArea.right + translateX,
                    selectorArea.left + selectorMinimumSize.x
                )
                ResizeBottom -> selectorArea.bottom = max(
                    selectorArea.bottom + translateY,
                    selectorArea.top + selectorMinimumSize.y
                )
                MoveSelector -> {
                    moveResult.set(selectorArea)
                    moveResult.translate(translateX, translateY)

                    if (maxArea.contains(moveResult)) {
                        selectorArea.set(moveResult)
                    }
                }
                ZoomCapture -> return
            }

            verifyBounds()
            onSelectorPositionChanged?.invoke(selectorArea.toRect())
            invalidate()
        }
    }

    /** Verify the correctness of the selector bounds. */
    private fun verifyBounds() {
        selectorArea.intersect(maxArea)
        selectedArea.apply {
            left = selectorArea.left + selectorAreaOffset
            top = selectorArea.top + selectorAreaOffset
            right = selectorArea.right - selectorAreaOffset
            bottom = selectorArea.bottom - selectorAreaOffset
        }
    }

    override fun invalidate() {
        selectorDrawingPath.apply {
            reset()

            moveTo(maxArea.left, maxArea.top)
            lineTo(maxArea.right, maxArea.top)
            lineTo(maxArea.right, maxArea.bottom)
            lineTo(maxArea.left, maxArea.bottom)
            close()

            moveTo(selectedArea.left, selectedArea.top)
            lineTo(selectedArea.right, selectedArea.top)
            lineTo(selectedArea.right, selectedArea.bottom)
            lineTo(selectedArea.left, selectedArea.bottom)
            close()
        }

        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(selectorDrawingPath, backgroundPaint)
        canvas.drawRoundRect(selectorArea, cornerRadius, cornerRadius, selectorPaint)
    }

    override fun onReset() {
        resetSelectorPosition()
        backgroundAlpha = selectorBackgroundAlpha
        selectorAlpha = 255
    }
}

/**
 * Style for [SelectorComponent].
 *
 * @param displayMetrics metrics for the device display.
 * @param selectorDefaultSize default size of the selector area in pixels.
 * @param handleSize the size of the selector handle in pixels.
 * @param selectorAreaOffset difference between the center of the selector and its inner content in pixels.
 * @param cornerRadius the radius of the corner for the selector in pixels.
 * @param selectorThickness the thickness of the selector borders, in pixels.
 * @param selectorColor the color of the selector borders.
 * @param selectorBackgroundColor the color of the selector background.
 */
internal class SelectorComponentStyle(
    displayMetrics: DisplayMetrics,
    val selectorDefaultSize: PointF,
    val handleSize: Float,
    val selectorAreaOffset: Int,
    val cornerRadius: Float,
    val selectorThickness: Float,
    @ColorInt val selectorColor: Int,
    @ColorInt val selectorBackgroundColor: Int,
) : ViewStyle(displayMetrics)

/** The ratio of the maximum width to be considered as the minimum width. */
private const val SELECTOR_MINIMUM_WIDTH_RATIO = 0.10f
/** The ratio of the maximum height to be considered as the minimum height. */
private const val SELECTOR_MINIMUM_HEIGHT_RATIO = 0.05f
/** Ratio between the handle and the inner handle */
private const val INNER_HANDLE_RATIO = 3f
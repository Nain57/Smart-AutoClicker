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
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition.selector

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.GestureDetector
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.extensions.translate
import com.buzbuz.smartautoclicker.ui.R

import kotlin.math.max
import kotlin.math.min

class Selector(
    context: Context,
    styledAttrs: TypedArray,
    private val screenMetrics: ScreenMetrics,
    private val viewInvalidator: () -> Unit,
) {

    private companion object {
        /** Ratio between the handle and the inner handle */
        private const val INNER_HANDLE_RATIO = 3f
    }

    /** */
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return onNewDownEvent(e.x, e.y)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (currentGesture == null) return false
            onTranslateSelector(-distanceX, -distanceY)
            return true
        }
    }

    /** */
    private val gestureDetector = GestureDetector(context, gestureListener)
    /** */
    var currentGesture: GestureType? = null
        private set

    /** The maximum size of the selector. */
    private val maxArea: RectF = RectF().apply {
        val screenSize = screenMetrics.getScreenSize()
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }
    /** Default size of the selector area. */
    private val selectorDefaultSize = PointF(
        styledAttrs.getDimensionPixelSize(
            R.styleable.ConditionSelectorView_defaultWidth,
            100
        ).toFloat() / 2f,
        styledAttrs.getDimensionPixelSize(
            R.styleable.ConditionSelectorView_defaultHeight,
            100
        ).toFloat() / 2f
    )
    /** */
    private val handleSize = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_resizeHandleSize,
        10
    ).toFloat()
    /** The size of the handle within the view. */
    private val innerHandleSize = handleSize / INNER_HANDLE_RATIO
    /** Difference between the center of the selector and its inner content. */
    private var selectorAreaOffset: Int = kotlin.math.ceil(
        styledAttrs.getDimensionPixelSize(
            R.styleable.ConditionSelectorView_thickness,
            4
        ).toFloat() / 2
    ).toInt()

    /** The radius of the corner for the selector. */
    val cornerRadius = styledAttrs.getDimensionPixelSize(
        R.styleable.ConditionSelectorView_cornerRadius,
        2
    ).toFloat()
    /** Area within the selector that represents the zone to be capture to creates a click condition. */
    val selectedArea = RectF()
    /** The area where the selector should be drawn. */
    val selectorArea = RectF()

    /** */
    var onSelectorPositionChanged: ((Rect) -> Unit)? = null

    fun onViewSizeChanged(w: Int, h: Int) {
        val screenSize = screenMetrics.getScreenSize()
        maxArea.apply {
            right = screenSize.x.toFloat()
            bottom = screenSize.y.toFloat()
        }

        selectorToDefaultPosition()
    }

    private fun selectorToDefaultPosition() {
        selectorArea.apply {
            left = maxArea.centerX() - selectorDefaultSize.x
            top = maxArea.centerY() - selectorDefaultSize.y
            right = maxArea.centerX() + selectorDefaultSize.x
            bottom = maxArea.centerY() + selectorDefaultSize.y
        }

        verifyBounds()
        onSelectorPositionChanged?.invoke(selectorArea.toRect())
        viewInvalidator.invoke()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
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
            Move.getGestureArea(selectedArea, handleSize, innerHandleSize).contains(eventX, eventY) ->
                Move
            else -> null
        }

        return currentGesture != null
    }

    private fun onTranslateSelector(translateX: Float, translateY: Float) {
        currentGesture?.let {
            when (it) {
                ResizeLeft -> selectorArea.left = min(selectorArea.left + translateX, selectorArea.right)
                ResizeTop -> selectorArea.top = min(selectorArea.top + translateY, selectorArea.bottom)
                ResizeRight -> selectorArea.right = max(selectorArea.right + translateX, selectorArea.left)
                ResizeBottom -> selectorArea.bottom = max(selectorArea.bottom + translateY, selectorArea.top)
                Move -> selectorArea.translate(translateX, translateY)
            }

            verifyBounds()
            onSelectorPositionChanged?.invoke(selectorArea.toRect())
            viewInvalidator.invoke()
        }
    }

    private fun verifyBounds() {
        selectorArea.intersect(maxArea)
        selectedArea.apply {
            left = selectorArea.left + selectorAreaOffset
            top = selectorArea.top + selectorAreaOffset
            right = selectorArea.right - selectorAreaOffset
            bottom = selectorArea.bottom - selectorAreaOffset
        }
    }

    /**
     * Get the part of the capture that is currently selected within the selector.
     *
     * @param captureArea
     * @param zoomLevel
     *
     * @return the area of the selector relative to the screen size.
     */
    fun getSelectionArea(captureArea: RectF, zoomLevel: Float): Rect {
        return RectF(this.selectedArea).run {
            val invertedZoomLevel = 1 / zoomLevel

            left = (left - captureArea.left) * invertedZoomLevel
            top = (top - captureArea.top) * invertedZoomLevel
            right = (right - captureArea.left) * invertedZoomLevel
            bottom = (bottom - captureArea.top) * invertedZoomLevel

            toRect()
        }
    }
}

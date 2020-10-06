/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.overlays.condition

import android.content.Context
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.extensions.bottomOffsetContains
import com.buzbuz.smartautoclicker.extensions.leftOffsetContains
import com.buzbuz.smartautoclicker.extensions.rightOffsetContains
import com.buzbuz.smartautoclicker.extensions.topOffsetContains

import kotlin.math.max
import kotlin.math.min

/**
 * [ScaleGestureDetector] implementation detecting the gestures for the overlay view.
 *
 * This class allows to calculate the values for the different resize/move/scale gestures to be applied on the selector
 * drawn within an overlay view.
 *
 * @param context the Android Context for the overlay menu.
 * @param view the overlay view the will be resized.
 * @param onScaleListener listener called when a scale gesture (pinch) have been detected.
 * @param onMoveListener listener called when a move gesture (drag and drop) have been detected.
 * @param onResizeListener listener called when a resize gesture (long press and move at border) have been detected.
 */
class ResizeGestureDetector(
    context: Context,
    private val view: View,
    private val onScaleListener: (Float) -> Unit,
    private val onMoveListener: (Float, Float) -> Unit,
    private val onResizeListener: (RectF, Int) -> Unit
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    companion object {

        @IntDef(RESIZE_LEFT, RESIZE_TOP, RESIZE_RIGHT, RESIZE_BOTTOM)
        @Retention(AnnotationRetention.SOURCE)
        annotation class GestureType
        /** The current touch events are for resizing the selector from the left size of the square. */
        const val RESIZE_LEFT = 1
        /** The current touch events are for resizing the selector from the top size of the square. */
        const val RESIZE_TOP = 2
        /** The current touch events are for resizing the selector from the right size of the square. */
        const val RESIZE_RIGHT = 3
        /** The current touch events are for resizing the selector from the bottom size of the square. */
        const val RESIZE_BOTTOM = 4
        /** The current touch events are for moving the selector. */
        const val MOVE = 5

        /** Identifier for no pointer. */
        private const val NO_POINTER_ID = -1
    }

    /** The Android scale gesture detector handling the touch events to detect the pinch for scaling. */
    private val scaleDetector = ScaleGestureDetector(context, this)

    /** The touch event pointer identifier when a move is detected. */
    private var movePointerId = NO_POINTER_ID
    /** The initial position of the view when the move gesture starts being detected. */
    private var moveInitialPosition = 0f to 0f
    /** The initial position of the touch event when the move gesture starts being detected. */
    private var moveInitialEventPosition = 0f to 0f
    /** The touch event pointer identifier when a resize is detected. */
    private var resizePointerId = NO_POINTER_ID
    /** The direction of the view resize. */
    @GestureType
    private var resizeType: Int? = null

    /** The size of the handle around the resizeable view. */
    var resizeHandleSize = 10f

    /**
     * Handles a new touch event on the resizable selector.
     *
     * @param event the new touch event.
     * @param selectorArea the current area of the selector to apply the gesture on in the view.
     */
    fun onTouchEvent(event: MotionEvent, selectorArea: RectF) : Boolean {
        if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_DOWN && onSinglePointerDown(event, selectorArea)) {
            return true
        }

        when (resizeType) {
            MOVE -> {
                val movePointerIndex = event.findPointerIndex(movePointerId)
                if (event.actionIndex == movePointerIndex) {
                    onMoveEvent(event, movePointerIndex)
                    return true
                }
            }
            else -> {
                val resizePointerIndex = event.findPointerIndex(resizePointerId)
                if (event.actionIndex == resizePointerIndex) {
                    onResizeEvent(event, resizePointerIndex, selectorArea)
                    return true
                }
            }
        }

        return scaleDetector.onTouchEvent(event)
    }

    /**
     * Handles a new touch event containing only one pointer (i.e, one finger on the view).
     * Depending on the event position on the selector, this will either initialize a move or a resize gesture.
     *
     * @param event the touch event containing the single pointer information.
     * @param selectorArea the current area of the selector in the view.
     */
    private fun onSinglePointerDown(event: MotionEvent, selectorArea: RectF): Boolean {
        if (selectorArea.contains(event.x, event.y)) {
            movePointerId = event.getPointerId(0)
            moveInitialPosition = selectorArea.left to selectorArea.top
            moveInitialEventPosition = event.rawX to event.rawY
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            resizeType = MOVE
            return true
        }

        resizeType = when {
            selectorArea.leftOffsetContains(resizeHandleSize, event.x, event.y) -> RESIZE_LEFT
            selectorArea.topOffsetContains(resizeHandleSize, event.x, event.y) -> RESIZE_TOP
            selectorArea.rightOffsetContains(resizeHandleSize, event.x, event.y) -> RESIZE_RIGHT
            selectorArea.bottomOffsetContains(resizeHandleSize, event.x, event.y) -> RESIZE_BOTTOM
            else -> null
        }
        if (resizeType != null) {
            resizePointerId = event.getPointerId(0)
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            return true
        }

        return false
    }

    /**
     * Handles a new touch event once the move gesture have been started.
     *
     * @param event the new touch event.
     * @param pointerIndex the index of the pointer in the touch event that has initiated the move gesture.
     */
    private fun onMoveEvent(event: MotionEvent, pointerIndex: Int) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                onMoveListener(
                    moveInitialPosition.first + event.getX(pointerIndex) - moveInitialEventPosition.first,
                    moveInitialPosition.second + event.getY(pointerIndex) - moveInitialEventPosition.second
                )
            }
            MotionEvent.ACTION_UP -> {
                movePointerId = NO_POINTER_ID
                moveInitialPosition = 0f to 0f
                moveInitialEventPosition = 0f to 0f
            }
        }
    }

    /**
     * Handles a new touch event once the resize gesture have been started.
     *
     * @param event the new touch event.
     * @param pointerIndex the index of the pointer in the touch event that has initiated the resize gesture.
     * @param selectorArea the current area of the selector to be resized in the view.
     */
    private fun onResizeEvent(event: MotionEvent, pointerIndex: Int, selectorArea: RectF) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                onResizeListener(RectF(
                    if (resizeType!! == RESIZE_LEFT) min(event.getX(pointerIndex), selectorArea.right) else selectorArea.left,
                    if (resizeType!! == RESIZE_TOP) min(event.getY(pointerIndex), selectorArea.bottom) else selectorArea.top,
                    if (resizeType!! == RESIZE_RIGHT) max(event.getX(pointerIndex), selectorArea.left) else selectorArea.right,
                    if (resizeType!! == RESIZE_BOTTOM) max(event.getY(pointerIndex), selectorArea.top) else selectorArea.bottom
                ), resizeType!!)
            }
            MotionEvent.ACTION_UP -> {
                resizePointerId = NO_POINTER_ID
                resizeType = null
            }
        }
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        onScaleListener.invoke(detector.scaleFactor)
        return true
    }
}
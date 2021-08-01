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
package com.buzbuz.smartautoclicker.baseui.gestures

import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

import androidx.annotation.IntDef

/** Indicate a type of gesture. */
@IntDef(MOVE, SCALE, RESIZE_LEFT, RESIZE_TOP, RESIZE_RIGHT, RESIZE_BOTTOM)
@Retention(AnnotationRetention.SOURCE)
annotation class GestureType
/** The current touch events are for moving the view. */
const val MOVE = 1
/** The current touch events are for scaling the view. */
const val SCALE = 2
/** The current touch events are for resizing the view from the left size of the square. */
const val RESIZE_LEFT = 3
/** The current touch events are for resizing the view from the top size of the square. */
const val RESIZE_TOP = 4
/** The current touch events are for resizing the view from the right size of the square. */
const val RESIZE_RIGHT = 5
/** The current touch events are for resizing the view from the bottom size of the square. */
const val RESIZE_BOTTOM = 6

/**
 * Base class for a gesture to apply to a view.
 *
 * In order to work correctly, the gesture must catch all [MotionEvent] provided by the [View] via the method
 * [View.onTouchEvent], and provide it to the Gesture using [onTouchEvent]. This method will then call the relevant
 * abstract methods.
 *
 * @param view the view to perform the gesture on.
 * @param handleSize the minimum size of the area where the user can interact with a gesture.
 * @param vibrate true to vibrate when the gesture is triggered, false to do nothing.
 * @param ignorePointers true to ignore the pointer that initiated the gesture, false to use it. Default value is false.
 */
abstract class Gesture(
    private val view: View,
    protected val handleSize: Float,
    private val vibrate: Boolean,
    private val ignorePointers: Boolean = false,
) {

    companion object {
        /** Identifier for no touch pointer. */
        const val NO_POINTER_ID = -1
        /** Ratio between the handle and the inner handle */
        const val INNER_HANDLE_RATIO = 3
    }

    /** The size of the handle within the view. */
    protected val innerHandleSize = handleSize / INNER_HANDLE_RATIO
    /**
     * The unique identifier of the pointer that initiated the gesture.
     * Using this value, the index of the pointer in another [MotionEvent] can be found using
     * [MotionEvent.findPointerIndex]. Will not be set if [ignorePointers] is true.
     */
    protected var firstPointerDownId = NO_POINTER_ID
        private set
    /**
     * The current index of the pointer that initiated the gesture in the current [MotionEvent].
     * Will be set during each calls to [onEvent] and [onGesturePointerUp], unless [ignorePointers] is
     * true.
     */
    protected var currentPointerDownIndex = NO_POINTER_ID
        private set

    /** The type of gesture. */
    @GestureType abstract val gestureType: Int

    /**
     * Handles a new touch event.
     * This method is the entry point of a motion event in order to be handled by the gesture. Depending on the event
     * content, the relevant abstract method will be called.
     *
     * @param event the touch event containing the single pointer information.
     * @param viewArea the current area of view.
     *
     * @return true if the event has been consumed, false if not.
     */
    fun onTouchEvent(event: MotionEvent, viewArea: RectF): Boolean  {
        // A new down event, check if its for this gesture
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!ignorePointers && event.pointerCount > 1) {
                return false
            }

            val handled = onDownEvent(event, viewArea)

            // The event is for this gesture, vibrate and keep the pointer, if necessary
            if (handled) {
                if (!ignorePointers ) {
                    firstPointerDownId = event.getPointerId(0)
                }

                if (vibrate) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
            return handled
        }

        // Not our business ? leave early
        if (!ignorePointers) {
            if (firstPointerDownId == NO_POINTER_ID) {
                return false
            }

            currentPointerDownIndex = event.findPointerIndex(firstPointerDownId)
            if (event.actionIndex != currentPointerDownIndex) {
                return false
            }
        }

        // Not a up ? Let the gesture implementation handle the event
        if (event.action != MotionEvent.ACTION_UP) {
            return onEvent(event, viewArea)
        }

        // That's a up ! Inform the implementation and end the gesture
        val upResult = onGesturePointerUp(event, viewArea)
        firstPointerDownId = NO_POINTER_ID
        currentPointerDownIndex = NO_POINTER_ID
        return upResult
    }

    /**
     * Handles a new touch event with the action [MotionEvent.ACTION_DOWN].
     *
     * @param event the touch event containing the single pointer information.
     * @param viewArea the current area of the view.
     *
     * @return true if event initiate the gesture, false if not.
     */
    protected abstract fun onDownEvent(event: MotionEvent, viewArea: RectF): Boolean

    /**
     * Handles a new touch event with any actions except [MotionEvent.ACTION_DOWN] and [MotionEvent.ACTION_UP].
     * If [ignorePointers] is false, this method will not be called for events that did not start the gesture.
     *
     * @param event the new touch event.
     * @param viewArea the current area of the view.
     *
     * @return true if this event has been handled, false if not.
     */
    protected abstract fun onEvent(event: MotionEvent, viewArea: RectF): Boolean

    /**
     * Handles a new touch event with the action [MotionEvent.ACTION_UP].
     * If [ignorePointers] is false, this method will not be called for events that did not start the gesture.
     *
     * @param event the new touch event.
     * @param viewArea the current area of the view.
     *
     * @return true if this event has been handled, false if not.
     */
    protected abstract fun onGesturePointerUp(event: MotionEvent, viewArea: RectF): Boolean
}

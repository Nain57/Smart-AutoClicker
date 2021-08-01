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
import android.view.MotionEvent
import android.view.View

/**
 * Gesture that moves the view center with the [MotionEvent] coordinates.
 *
 * @param view the view to perform the gesture on.
 * @param handleSize the minimum size of the area where the user can interact with a gesture.
 * @param vibrate true to vibrate when the gesture is triggered, false to do nothing.
 * @param moveListener the object to notify upon move gesture results.
 * Move listener parameters are:
 * * the x position of the center of the view after the move
 * * the y position of the center of the view after the move
 */
class MoveGesture(
    view: View,
    handleSize: Float,
    vibrate: Boolean,
    private val moveListener: (Float, Float) -> Unit
) : Gesture(view, handleSize, vibrate) {

    /** The initial position of the view when the move gesture starts being detected. */
    private var moveInitialPosition = 0f to 0f
    /** The initial position of the touch event when the move gesture starts being detected. */
    private var moveInitialEventPosition = 0f to 0f

    override val gestureType = MOVE

    override fun onDownEvent(event: MotionEvent, viewArea: RectF): Boolean {
        // When big enough, consider the part close to the view border as border handles
        val moveArea = RectF(viewArea)
        if (viewArea.height() > handleSize && viewArea.width() > handleSize) {
            moveArea.inset(innerHandleSize, innerHandleSize)
        }

        if (!moveArea.contains(event.x, event.y)) {
            return false
        }

        moveInitialPosition = viewArea.centerX() to viewArea.centerY()
        moveInitialEventPosition = event.rawX to event.rawY
        return true
    }

    override fun onEvent(event: MotionEvent, viewArea: RectF): Boolean {
        if (event.pointerCount != 1) {
            return false
        }

        moveListener(
            moveInitialPosition.first + event.getX(currentPointerDownIndex) - moveInitialEventPosition.first,
            moveInitialPosition.second + event.getY(currentPointerDownIndex) - moveInitialEventPosition.second
        )
        return true
    }

    override fun onGesturePointerUp(event: MotionEvent, viewArea: RectF): Boolean {
        moveInitialPosition = 0f to 0f
        moveInitialEventPosition = 0f to 0f
        return true
    }
}
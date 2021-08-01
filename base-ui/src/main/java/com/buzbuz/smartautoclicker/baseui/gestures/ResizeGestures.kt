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
import androidx.annotation.VisibleForTesting
import kotlin.math.max
import kotlin.math.min

/**
 * Base gesture for all resize gestures.
 * A resize gesture is when the user touch the border of the view and drag and drop it to resize the view alongside
 * the user finger.
 *
 * @param view the view to perform the gesture on.
 * @param handleSize the minimum size of the area where the user can interact with a gesture.
 * @param vibrate true to vibrate when the gesture is triggered, false to do nothing.
 * @param onResizeListener the object to notify upon resize gesture results.
 * Move listener parameters are:
 * * the new view bounds after the gesture
 * * the type of the gesture that did the resize
 */
abstract class ResizeGesture(
    view: View,
    handleSize: Float,
    vibrate: Boolean,
    private val onResizeListener: (RectF, Int) -> Unit
) : Gesture(view, handleSize, vibrate) {

    final override fun onDownEvent(event: MotionEvent, viewArea: RectF): Boolean {
        return getHandleArea(viewArea, viewArea.height() > handleSize && viewArea.width() > handleSize)
            .contains(event.x, event.y)
    }

    final override fun onEvent(event: MotionEvent, viewArea: RectF): Boolean {
        if (event.action != MotionEvent.ACTION_MOVE) {
            return false
        }
        onResizeListener(getNewSize(event, viewArea), gestureType)
        return true
    }

    final override fun onGesturePointerUp(event: MotionEvent, viewArea: RectF): Boolean = true

    /**
     * Get the size of the handle for this gesture given the current view area.
     *
     * @param viewArea the current view area.
     * @param enoughInnerSpace true if there is enough space to make the handle overlap the view content, or false if
     *                         the view is too small for that.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal abstract fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) : RectF

    /**
     * Get the new size of the view according to the new move motion event.
     *
     * @param event the [MotionEvent.ACTION_MOVE] motion event.
     * @param viewArea the current view bounds.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal abstract fun getNewSize(event: MotionEvent, viewArea: RectF): RectF
}

/** Resize gesture for the left side of the view with a rectangle handle on the left side as well. */
class ResizeLeftGesture(view: View, handleSize: Float, vibrate: Boolean, onResizeListener: (RectF, Int) -> Unit)
    : ResizeGesture(view, handleSize, vibrate, onResizeListener) {

    override val gestureType = RESIZE_LEFT

    override fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) =
        RectF(
            viewArea.left - handleSize,
            viewArea.top,
            if(enoughInnerSpace) viewArea.left + innerHandleSize else viewArea.left,
            viewArea.bottom
        )

    override fun getNewSize(event: MotionEvent, viewArea: RectF) =
        RectF(
            min(event.getX(currentPointerDownIndex), viewArea.right),
            viewArea.top,
            viewArea.right,
            viewArea.bottom
        )
}

/** Resize gesture for the top side of the view with a rectangle handle on the top side as well. */
class ResizeTopGesture(view: View, handleSize: Float, vibrate: Boolean, onResizeListener: (RectF, Int) -> Unit)
    : ResizeGesture(view, handleSize, vibrate, onResizeListener) {

    override val gestureType = RESIZE_TOP

    override fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) =
        RectF(
            viewArea.left,
            viewArea.top  - handleSize,
            viewArea.right,
            if (enoughInnerSpace) viewArea.top + innerHandleSize else viewArea.top
        )

    override fun getNewSize(event: MotionEvent, viewArea: RectF) =
        RectF(
            viewArea.left,
            min(event.getY(currentPointerDownIndex), viewArea.bottom),
            viewArea.right,
            viewArea.bottom
        )
}

/** Resize gesture for the right side of the view with a rectangle handle on the right side as well. */
class ResizeRightGesture(view: View, handleSize: Float, vibrate: Boolean, onResizeListener: (RectF, Int) -> Unit)
    : ResizeGesture(view, handleSize, vibrate, onResizeListener) {

    override val gestureType = RESIZE_RIGHT

    override fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) =
        RectF(
            if(enoughInnerSpace) viewArea.right - innerHandleSize else viewArea.right,
            viewArea.top,
            viewArea.right + handleSize,
            viewArea.bottom
        )

    override fun getNewSize(event: MotionEvent, viewArea: RectF) =
        RectF(
            viewArea.left,
            viewArea.top,
            max(event.getX(currentPointerDownIndex), viewArea.left),
            viewArea.bottom
        )
}

/** Resize gesture for the bottom side of the view with a rectangle handle on the bottom side as well. */
class ResizeBottomGesture(view: View, handleSize: Float, vibrate: Boolean, onResizeListener: (RectF, Int) -> Unit)
    : ResizeGesture(view, handleSize, vibrate, onResizeListener) {

    override val gestureType = RESIZE_BOTTOM

    override fun getHandleArea(viewArea: RectF, enoughInnerSpace: Boolean) =
        RectF(
            viewArea.left,
            if (enoughInnerSpace) viewArea.bottom - innerHandleSize else viewArea.bottom,
            viewArea.right,
            viewArea.bottom + handleSize
        )

    override fun getNewSize(event: MotionEvent, viewArea: RectF) =
        RectF(
            viewArea.left,
            viewArea.top,
            viewArea.right,
            max(event.getY(currentPointerDownIndex), viewArea.top)
        )
}
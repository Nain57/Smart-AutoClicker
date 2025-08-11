
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.graphics.Point
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

internal class OverlayMenuMoveTouchEventHandler(
    private val onMenuMoved: (Point) -> Unit,
) {

    /** The initial position of the overlay menu when pressing the move menu item. */
    private var moveInitialViewPosition: Point = Point(0, 0)
    /** The initial position of the touch event that as initiated the move of the overlay menu. */
    private var moveInitialTouchPosition: Point = Point(0, 0)

    fun onTouchEvent(viewToMove: View, event: MotionEvent): Boolean =
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewToMove.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onDownEvent(viewToMove, event)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                onMoveEvent(event)
                true
            }

            else -> false
        }

    private fun onDownEvent(viewToMove: View, event: MotionEvent) {
        val layoutParams = (viewToMove.layoutParams as WindowManager.LayoutParams)
        moveInitialViewPosition = Point(layoutParams.x, layoutParams.y)
        moveInitialTouchPosition = Point(event.rawX.toInt(), event.rawY.toInt())
    }

    private fun onMoveEvent(event: MotionEvent) {
        onMenuMoved(
            Point(
                moveInitialViewPosition.x + (event.rawX.toInt() - moveInitialTouchPosition.x),
                moveInitialViewPosition.y + (event.rawY.toInt() - moveInitialTouchPosition.y),
            )
        )
    }
}
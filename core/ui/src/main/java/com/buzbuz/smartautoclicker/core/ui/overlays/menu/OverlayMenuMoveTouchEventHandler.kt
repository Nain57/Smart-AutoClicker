/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu

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
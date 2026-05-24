/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.graphics.Point
import android.os.Build
import android.view.MotionEvent
import android.view.View

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.common.overlays.testutils.mockSimpleRawEvent

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config

/** Test the [OverlayMenuMoveTouchEventHandler] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayMenuMoveTouchEventHandlerTests {

    @Test
    fun onTouchEvent_onActionUp_notifiesMoveFinished() {
        var moveFinishedCount = 0
        val handler = newHandler(
            onMoveFinished = { moveFinishedCount++ },
        )

        handler.onTouchEvent(mock(View::class.java), mockSimpleRawEvent(MotionEvent.ACTION_UP, 0f, 0f))

        assertEquals(1, moveFinishedCount)
    }

    @Test
    fun onTouchEvent_onActionCancel_notifiesMoveFinished() {
        var moveFinishedCount = 0
        val handler = newHandler(
            onMoveFinished = { moveFinishedCount++ },
        )

        handler.onTouchEvent(mock(View::class.java), mockSimpleRawEvent(MotionEvent.ACTION_CANCEL, 0f, 0f))

        assertEquals(1, moveFinishedCount)
    }

    @Test
    fun onTouchEvent_onActionMove_usesLogicalMenuPosition() {
        var movedPosition: Point? = null
        val handler = newHandler(
            getCurrentMenuPosition = { Point(200, 100) },
            onMenuMoved = { movedPosition = it },
        )
        val movedView = mock(View::class.java)

        handler.onTouchEvent(movedView, mockSimpleRawEvent(MotionEvent.ACTION_DOWN, 20f, 20f))
        handler.onTouchEvent(movedView, mockSimpleRawEvent(MotionEvent.ACTION_MOVE, 50f, 40f))

        assertEquals(Point(230, 120), movedPosition)
    }

    private fun newHandler(
        onMenuMoved: (Point) -> Unit = {},
        getCurrentMenuPosition: () -> Point = { Point(0, 0) },
        onMoveStarted: () -> Unit = {},
        onMoveFinished: () -> Unit = {},
    ): OverlayMenuMoveTouchEventHandler =
        OverlayMenuMoveTouchEventHandler(
            onMenuMoved = onMenuMoved,
            getCurrentMenuPosition = getCurrentMenuPosition,
            onMoveStarted = onMoveStarted,
            onMoveFinished = onMoveFinished,
        )
}

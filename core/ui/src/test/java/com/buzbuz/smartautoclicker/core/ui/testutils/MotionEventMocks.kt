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
package com.buzbuz.smartautoclicker.core.ui.testutils

import android.view.MotionEvent

import org.mockito.Mockito
import org.mockito.Mockito.`when` as mockWhen

/** Creates a new mock for [MotionEvent]. */
private fun newMock(): MotionEvent = Mockito.mock(MotionEvent::class.java)

/**
 * Create a simple mock motion event with only the specified action and raw position.
 *
 * @param action the action. Must be one of the values declared in [MotionEvent].
 * @param rawXPos the x position for the touch event.
 * @param rawYPos the y position for the touch event.
 */
fun mockSimpleRawEvent(action: Int, rawXPos: Float, rawYPos: Float) = newMock().also {
    mockWhen(it.action).thenReturn(action)
    mockWhen(it.rawX).thenReturn(rawXPos)
    mockWhen(it.rawY).thenReturn(rawYPos)
}

/**
 * Create a complete mock motion event.
 *
 * @param action the action. Must be one of the values declared in [MotionEvent].
 * @param xPos the x position for the touch event.
 * @param yPos the y position for the touch event.
 * @param pointerId the id for this pointer.
 * @param pointerCount the count of pointers in the event.
 * @param pointerIndex the index of the pointer.
 * @param actionIndex the index of the pointer doing the action.
 */
fun mockEvent(action: Int, xPos: Float, yPos: Float, pointerId: Int, pointerCount: Int = 1, pointerIndex: Int = 0,
        actionIndex: Int = pointerIndex) = newMock().also {
    mockWhen(it.action).thenReturn(action)
    mockWhen(it.pointerCount).thenReturn(pointerCount)
    mockWhen(it.getPointerId(pointerIndex)).thenReturn(pointerId)
    mockWhen(it.findPointerIndex(pointerId)).thenReturn(pointerIndex)
    mockWhen(it.actionIndex).thenReturn(actionIndex)
    mockWhen(it.x).thenReturn(xPos)
    mockWhen(it.y).thenReturn(yPos)
    mockWhen(it.getX(pointerIndex)).thenReturn(xPos)
    mockWhen(it.getY(pointerIndex)).thenReturn(yPos)
    mockWhen(it.rawX).thenReturn(xPos)
    mockWhen(it.rawY).thenReturn(yPos)
}
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
package com.buzbuz.smartautoclicker.baseui.utils

import android.view.MotionEvent

import org.mockito.Mockito
import org.mockito.Mockito.`when` as mockWhen

/** Creates a new mock for [MotionEvent]. */
private fun newMock(): MotionEvent = Mockito.mock(MotionEvent::class.java)

/**
 * Create a mock touch event with only the specified action and raw position.
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
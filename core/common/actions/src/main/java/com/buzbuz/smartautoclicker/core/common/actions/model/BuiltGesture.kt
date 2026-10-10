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
package com.buzbuz.smartautoclicker.core.common.actions.model

import android.accessibilityservice.GestureDescription

/**
 * A gesture resolved into what has to be dispatched to the system.
 *
 * @param chains the gestures to dispatch, in order. The gestures of a same chain must be dispatched in one go.
 * @param heldPointer the pointer left down on the screen once the whole gesture is played, or null when the gesture
 * leaves nothing behind.
 */
class BuiltGesture internal constructor(
    val chains: List<List<GestureDescription>>,
    val heldPointer: HeldPointer?,
)
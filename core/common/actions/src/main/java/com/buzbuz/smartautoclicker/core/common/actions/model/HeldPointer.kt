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

import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder

/**
 * A pointer left down on the screen by a gesture holding its first finger.
 *
 * It is an opaque handle on what the system currently has down: only [AndroidGestureBuilder] can create one, and it
 * must be given back to [AndroidGestureBuilder.build] for every gesture executed while it is held. The system only
 * honours a continuation when the first step of the dispatched gesture holds exactly one stroke for each pointer
 * currently down, so a gesture ignoring the held pointer would have the hold cancelled.
 *
 * @param stroke the last stroke dispatched for the pointer, the one to continue to keep it down.
 * @param position the position at which the pointer rests on the screen. A continued stroke is refused unless it
 * starts exactly where the stroke it continues ended.
 * @param owner what the gesture holding the pointer was executed for, as given to [AndroidGestureBuilder.build]. The
 * gestures carrying the pointer along keep it, only a gesture taking the hold over replaces it: it always tells who
 * the pointer currently down belongs to.
 */
class HeldPointer internal constructor(
    internal val stroke: StrokeDescription,
    internal val position: Point,
    val owner: Any?,
)

/**
 * Index given to a [HeldPointer] carried along by a new gesture.
 *
 * It is above the maximum finger count of a gesture: a carried pointer is an extra finger, it never takes the place
 * of one of the fingers of the gesture being built. A gesture using all its fingers while a pointer is held therefore
 * puts six of them down at the same time.
 */
internal const val HELD_POINTER_FINGER_INDEX: Int = 5

/** Duration of the stroke lifting a held pointer off the screen. */
internal const val HELD_POINTER_RELEASE_DURATION_MS: Long = 1L
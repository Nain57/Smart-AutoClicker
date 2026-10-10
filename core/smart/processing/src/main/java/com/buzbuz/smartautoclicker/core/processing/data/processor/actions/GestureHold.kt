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
package com.buzbuz.smartautoclicker.core.processing.data.processor.actions

import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release.Hold
import com.buzbuz.smartautoclicker.core.domain.model.event.Event


/**
 * The owner of a pointer held on the screen: what it is held for, and until when.
 *
 * A new instance is given for each dispatched gesture, and becomes the owner of the held pointer only if that gesture
 * takes the hold over. The gestures carrying the pointer along keep the previous owner, so the hold keeps its type and
 * start time whatever is executed in between.
 *
 * @param event the event the gesture holding the pointer has been executed for.
 * @param hold the hold of the release of the first finger of that gesture.
 */
internal class GestureHold(
    val event: Event,
    val hold: Hold,
) {

    /** Time at which the gesture taking the hold over has been fully dispatched, in ms. Set once dispatched. */
    var startTimeMs: Long = 0L
        private set

    fun onHoldStarted(timeMs: Long) {
        startTimeMs = timeMs
    }

    /** @return the time remaining before the end of a [Hold.ForDuration], or null for any other hold. */
    fun getRemainingDurationMs(currentTimeMs: Long): Long? =
        (hold as? Hold.ForDuration)?.let { timedHold ->
            (startTimeMs + timedHold.durationMs - currentTimeMs).coerceAtLeast(0L)
        }

    override fun toString(): String = "GestureHold(event=${event.name}, hold=$hold, startTimeMs=$startTimeMs)"
}

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
package com.buzbuz.smartautoclicker.core.domain.model.action.gesture

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Move
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Press
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Wait


/**
 * A single finger of a [Gesture].
 *
 * A finger touches the screen once: its [motions] must start with a [Press] and end with a [Release], with any
 * number of [Move] and [Wait] in between. Pressing or releasing again in the middle of the sequence is not supported,
 * use another finger with a later [startTimeMs] instead.
 *
 * @param id the unique identifier for the finger.
 * @param startTimeMs the delay in milliseconds between the start of the gesture and the [Press] of this finger.
 * @param motions the ordered motions of this finger, from its press to its release.
 */
data class GestureFinger(
    override val id: Identifier,
    val startTimeMs: Long,
    val motions: List<GestureFingerMotion>,
): Identifiable, Completable {

    /**
     * @return true if the motions start with a [Press], end with a [Release], are all complete and the finger ends
     * before [Gesture.GESTURE_MAXIMUM_DURATION_MS].
     */
    override fun isComplete(): Boolean {
        // All motions should start with press and end with release
        if (motions.size < 2) return false
        if (motions.first() !is Press) return false
        if (motions.last() !is Release) return false

        if (getDurationMs() >= Gesture.GESTURE_MAXIMUM_DURATION_MS) return false
        motions.forEach { motion -> if (!motion.isComplete()) return false }

        return true
    }

    /**
     * @return the time in milliseconds, relative to the start of the gesture, at which this finger is released. A
     * finger with nothing but a press and a release is a tap and counts for 1ms.
     */
    fun getDurationMs() : Long =
        startTimeMs + (
            if (motions.size == 2 && motions.first() is Press && motions.last() is Release) 1L
            else motions.sumOf { motion ->
                when (motion) {
                    is Move -> motion.durationMs
                    is Wait -> motion.durationMs
                    is Press,
                    is Release -> 0L
                }
            }
        )
}

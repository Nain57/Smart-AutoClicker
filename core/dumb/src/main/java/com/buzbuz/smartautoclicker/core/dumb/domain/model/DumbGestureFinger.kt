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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point
import android.os.Build
import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion.Move
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion.Press
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion.Release
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion.Wait

/**
 * A single finger of a [DumbAction.DumbGesture].
 *
 * A finger touches the screen once: its [motions] must start with a [Press] and end with a [Release], with any
 * number of [Move] and [Wait] in between. Pressing or releasing again in the middle of the sequence is not supported,
 * use another finger with a later [startTimeMs] instead.
 *
 * @param id the unique identifier for the finger.
 * @param startTimeMs the delay in milliseconds between the start of the gesture and the [Press] of this finger.
 * @param motions the ordered motions of this finger, from its press to its release.
 */
data class DumbGestureFinger(
    override val id: Identifier,
    val startTimeMs: Long,
    val motions: List<DumbGestureFingerMotion>,
) : Identifiable {

    /**
     * @return true if the motions start with a [Press], end with a [Release], are all valid and the finger ends
     * before [DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS].
     */
    fun isValid(): Boolean {
        if (motions.size < 2) return false
        if (motions.first() !is Press) return false
        if (motions.last() !is Release) return false

        if (getDurationMs() >= DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS) return false
        motions.forEach { motion -> if (!motion.isValid()) return false }

        return true
    }

    /**
     * @return the time in milliseconds, relative to the start of the gesture, at which this finger is released. A
     * finger with nothing but a press and a release is a tap and counts for 1ms.
     */
    fun getDurationMs(): Long =
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

/**
 * A step in the timeline of a [DumbGestureFinger].
 *
 * Motions are executed one after the other, each one starting where the previous one ended. Only [Move] and [Wait]
 * take time; [Press] and [Release] are instantaneous.
 */
sealed interface DumbGestureFingerMotion : Identifiable {

    fun isValid(): Boolean

    /**
     * The finger touches the screen. Must be the first motion of a finger.
     *
     * @param id the unique identifier for the motion.
     * @param position the position where the finger lands on the screen.
     */
    data class Press(
        override val id: Identifier,
        override val position: Point,
    ) : DumbGestureFingerMotion, Positionable {

        override fun isValid(): Boolean = true
    }

    /**
     * The finger slides from its current position to a new one.
     *
     * @param id the unique identifier for the motion.
     * @param position the position where the finger stops moving.
     * @param durationMs the time in milliseconds the finger takes to reach [position].
     */
    data class Move(
        override val id: Identifier,
        override val position: Point,
        val durationMs: Long,
    ) : DumbGestureFingerMotion, Positionable {

        override fun isValid(): Boolean =
            durationMs in 1 until DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS
    }

    /**
     * The finger stays still on the screen at its current position.
     *
     * Holding a finger without moving it requires stroke continuation from the accessibility API, which is only
     * available from Android 8; on older versions, this motion is skipped at execution.
     *
     * @param id the unique identifier for the motion.
     * @param durationMs the time in milliseconds the finger stays still.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    data class Wait(
        override val id: Identifier,
        val durationMs: Long,
    ) : DumbGestureFingerMotion {

        override fun isValid(): Boolean =
            durationMs in 1 until DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS
    }

    /**
     * The finger leaves the screen at its current position. Must be the last motion of a finger.
     *
     * @param id the unique identifier for the motion.
     */
    data class Release(
        override val id: Identifier,
    ) : DumbGestureFingerMotion {

        override fun isValid(): Boolean = true
    }

    /** A motion that brings the finger to a position on the screen. */
    interface Positionable {
        /** The position of the finger at the end of the motion, in px. */
        val position: Point
    }
}

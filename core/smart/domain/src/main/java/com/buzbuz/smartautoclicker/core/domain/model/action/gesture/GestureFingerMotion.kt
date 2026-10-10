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

import android.os.Build
import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Positionable


/**
 * A step in the timeline of a [GestureFinger].
 *
 * Motions are executed one after the other, each one starting where the previous one ended. Only [Move] and [Wait]
 * take time; [Press] and [Release] are instantaneous.
 */
sealed interface GestureFingerMotion : Identifiable, Completable {

    /**
     * The finger touches the screen. Must be the first motion of a finger.
     *
     * @param id the unique identifier for the motion.
     * @param position the position where the finger lands on the screen.
     * @param offset the offset in px applied to [position].
     */
    data class Press(
        override val id: Identifier,
        override val position: Position,
        override val offset: Position,
    ): GestureFingerMotion, Positionable {

        override fun isComplete(): Boolean = true
    }

    /**
     * The finger slides from its current position to a new one.
     *
     * @param id the unique identifier for the motion.
     * @param position the position where the finger stops moving.
     * @param offset the offset in px applied to [position].
     * @param durationMs the time in milliseconds the finger takes to reach [position].
     */
    data class Move(
        override val id: Identifier,
        override val position: Position,
        override val offset: Position,
        val durationMs: Long,
    ): GestureFingerMotion, Positionable {

        override fun isComplete(): Boolean = durationMs in 1 until Gesture.GESTURE_MAXIMUM_DURATION_MS
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
    ): GestureFingerMotion {

        override fun isComplete(): Boolean = durationMs in 1 until Gesture.GESTURE_MAXIMUM_DURATION_MS
    }

    /**
     * The finger leaves the screen at its current position. Must be the last motion of a finger.
     *
     * Keeping the finger pressed once the gesture is over relies on stroke continuation from the accessibility API,
     * which is only available from Android 8, and only the first finger of a gesture can be held. In all other cases,
     * [hold] is ignored at execution and the finger is released right away.
     *
     * @param id the unique identifier for the motion.
     * @param hold how long the finger stays pressed on the screen once the gesture is over.
     */
    data class Release(
        override val id: Identifier,
        @RequiresApi(Build.VERSION_CODES.O) val hold: Hold = Hold.None,
    ): GestureFingerMotion {

        override fun isComplete(): Boolean = hold.isComplete()

        /** How long a released finger stays pressed on the screen once its gesture is over. */
        sealed interface Hold : Completable {

            /** The finger is lifted as soon as the gesture is over. */
            data object None : Hold {
                override fun isComplete(): Boolean = true
            }

            /** The finger stays pressed until the conditions of its event are no longer fulfilled. */
            data object UntilUnfulfilled : Hold {
                override fun isComplete(): Boolean = true
            }

            /** The finger stays pressed until all the actions of its event have been executed. */
            data object UntilActionsEnd : Hold {
                override fun isComplete(): Boolean = true
            }

            /**
             * The finger stays pressed for a custom time defined by the user.
             *
             * The hold starts once the gesture is over, so it is not part of the gesture duration and is not bound by
             * [Gesture.GESTURE_MAXIMUM_DURATION_MS]. It is bound by [MAXIMUM_DURATION_MS] instead.
             *
             * @param durationMs the time in milliseconds the finger stays pressed once the gesture is over.
             */
            data class ForDuration(val durationMs: Long) : Hold {
                override fun isComplete(): Boolean = durationMs in 1..MAXIMUM_DURATION_MS

                companion object {
                    /** The maximum hold duration, 24 hours. */
                    const val MAXIMUM_DURATION_MS: Long = 86_400_000L
                }
            }
        }
    }
}
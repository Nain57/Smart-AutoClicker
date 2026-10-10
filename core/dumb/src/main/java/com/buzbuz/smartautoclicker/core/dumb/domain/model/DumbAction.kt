/*
 * Copyright (C) 2024 Kevin Buzeau
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

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

sealed class DumbAction : Identifiable {

    /** The identifier of the dumb scenario for this dumb action. */
    abstract val scenarioId: Identifier
    /** The name of the dumb action. */
    abstract val name: String?

    abstract val priority: Int

    abstract fun isValid(): Boolean

    fun copyWithNewScenarioId(scenarioId: Identifier): DumbAction =
        when (this) {
            is DumbClick -> copy(scenarioId = scenarioId)
            is DumbPause -> copy(scenarioId = scenarioId)
            is DumbGesture -> copy(scenarioId = scenarioId)
        }

    data class DumbClick(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val position: Point,
        val pressDurationMs: Long,
    ) : DumbAction(), RepeatableWithDelay {

        override fun isValid(): Boolean =
            name.isNotEmpty() && pressDurationMs > 0 && isRepeatCountValid() && isRepeatDelayValid()
    }

    /**
     * A multitouch interaction described as a set of [fingers], each one following its own timeline of motions on
     * the screen. All fingers share the same time origin: the gesture starts at 0ms and each finger touches the
     * screen at its own [DumbGestureFinger.startTimeMs], allowing fingers to be sequenced (double tap) or overlapped
     * (pinch).
     *
     * @param fingers the fingers composing the gesture, between 1 and [GESTURE_MAXIMUM_FINGER_COUNT].
     */
    data class DumbGesture(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val fingers: List<DumbGestureFinger>,
    ) : DumbAction(), RepeatableWithDelay {

        companion object {
            /** The maximum number of fingers a gesture can contain. */
            const val GESTURE_MAXIMUM_FINGER_COUNT = 5
            /** The maximum total duration of a gesture in milliseconds, as allowed by the Android accessibility API. */
            val GESTURE_MAXIMUM_DURATION_MS: Long = GestureDescription.getMaxGestureDuration()
        }

        override fun isValid(): Boolean =
            name.isNotEmpty()
                    && fingers.isNotEmpty() && fingers.size <= GESTURE_MAXIMUM_FINGER_COUNT
                    && fingers.all { finger -> finger.isValid() }
                    && isRepeatCountValid() && isRepeatDelayValid()

        /** @return the total duration of the gesture in milliseconds, from its start to the release of the last finger. */
        fun getDurationMs(): Long =
            fingers.maxOfOrNull { finger -> finger.getDurationMs() } ?: 0L
    }

    data class DumbPause(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        val pauseDurationMs: Long,
    ) : DumbAction() {

        override fun isValid(): Boolean = name.isNotEmpty()
    }
}

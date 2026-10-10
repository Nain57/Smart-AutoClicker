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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.accessibilityservice.GestureDescription
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.areComplete
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger

/**
 * Gesture action.
 *
 * A gesture is a multitouch interaction described as a set of [fingers], each one following its own timeline of
 * motions on the screen. All fingers share the same time origin: the gesture starts at 0ms and each finger touches
 * the screen at its own [GestureFinger.startTimeMs], allowing fingers to be sequenced (double tap) or overlapped
 * (pinch).
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param priority the priority of the action in its event.
 * @param fingers the fingers composing the gesture, between 1 and [GESTURE_MAXIMUM_FINGER_COUNT].
 */
data class Gesture(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val fingers: List<GestureFinger>,
): Action() {

    companion object {
        /** The maximum number of fingers a gesture can contain. */
        const val GESTURE_MAXIMUM_FINGER_COUNT = 5
        /** The maximum total duration of a gesture in milliseconds, as allowed by the Android accessibility API. */
        val GESTURE_MAXIMUM_DURATION_MS = GestureDescription.getMaxGestureDuration()
    }

    /**
     * @return true if the action has a name, contains a valid number of fingers and all of its fingers are complete.
     */
    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false
        if (fingers.isEmpty() || fingers.size > GESTURE_MAXIMUM_FINGER_COUNT) return false

        return fingers.areComplete()
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + priority.hashCode() + fingers.hashCode()

    override fun deepCopy(): Gesture = copy(name = "" + name)

    /** @return the total duration of the gesture in milliseconds, from its start to the release of the last finger. */
    fun getDurationMs(): Long =
        fingers.maxOfOrNull { finger -> finger.getDurationMs() } ?: 0L
}
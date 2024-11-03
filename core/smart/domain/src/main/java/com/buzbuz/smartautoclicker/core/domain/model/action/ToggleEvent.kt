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
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle

/**
 * Toggle Event Action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param toggleAll true to toggle all events, false to control only via EventToggle.
 * @param toggleAllType the type of manipulation to apply for toggle all.
 */
data class ToggleEvent(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val toggleAll: Boolean = false,
    val toggleAllType: ToggleType? = null,
    val eventToggles: List<EventToggle> = emptyList(),
) : Action() {

    /**
     * Types of toggle of a [ToggleEvent].
     * Keep the same names as the db ones.
     */
    enum class ToggleType {
        /** Enable the event. Has no effect if the event is already enabled. */
        ENABLE,
        /** Disable the event. Has no effect if the event is already disabled. */
        DISABLE,
        /** Enable the event if it is disabled, disable it if it is enabled. */
        TOGGLE;

        fun toEntity(): EventToggleType = EventToggleType.valueOf(name)
    }

    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false

        return if (toggleAll) {
            toggleAllType != null
        } else {
            eventToggles.isNotEmpty() && eventToggles.find { !it.isComplete() } == null
        }
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + toggleAll.hashCode() + toggleAllType.hashCode() + eventToggles.hashCode()

    override fun deepCopy(): ToggleEvent = copy(name = "" + name)
}
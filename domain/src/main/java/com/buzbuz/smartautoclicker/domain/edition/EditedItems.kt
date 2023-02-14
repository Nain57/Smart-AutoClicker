/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.domain.edition

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario

/** Represents the scenario currently edited. */
data class EditedScenario internal constructor(
    val scenario: Scenario,
    val endConditions: List<EditedEndCondition>,
    val events: List<EditedEvent>,
)

/** Represents the end conditions of the scenario currently edited. */
data class EditedEndCondition internal constructor(
    val endCondition: EndCondition,
    val itemId: Int,
    val eventItemId: Int = INVALID_EDITED_ITEM_ID,
)

/** Represents the events of the scenario currently edited. */
data class EditedEvent internal constructor(
    val event: Event,
    val itemId: Int,
    val editedActions: List<EditedAction>,
)

/** Represents the actions of the event currently edited. */
data class EditedAction internal constructor(
    val action: Action,
    val itemId: Int,
    val toggleEventItemId: Int = INVALID_EDITED_ITEM_ID,
)

/**
 * Check scenario validity for database saving.
 * An edited scenario should have at least one event (all valid), and 0 or more end conditions (all valid)
 **
 * @return true if the edited scenario can be saved in database, false if not.
 */
internal fun EditedScenario.isValidForSave(): Boolean {
    if (events.isEmpty()) return false
    for (editedEvent in events) if (!editedEvent.isValidForSave(events)) return false

    for (editedEndCondition in endConditions) if (!editedEndCondition.isValidForSave(events)) return false

    return true
}

/**
 * Check event validity for database saving.
 * An edited event should shave a valid item id, at least one conditions (all valid), at least one action (all valid)
 * and a list of edited action, each of them referencing an event action.
 *
 * @param editedEvents the edited events for the scenario of this event.
 *
 * @return true if the edited event can be saved in database, false if not.
 */
private fun EditedEvent.isValidForSave(editedEvents: List<EditedEvent>): Boolean {
    if (itemId == INVALID_EDITED_ITEM_ID) return false

    if (event.conditions.isNullOrEmpty()) return false

    if (editedActions.isEmpty()) return false
    if (event.actions.isNullOrEmpty()) return false
    if (editedActions.size != event.actions.size) return false
    for (editedAction in editedActions) if (!editedAction.isValidForSave(editedEvents)) return false

    return true
}

/**
 * Check action validity for database saving.
 * An edited action should simply be complete, except for a ToggleEvent, where it should reference an existing event
 * within the same scenario.
 *
 * @param editedEvents the edited events for the scenario of this action.
 *
 * @return true if the edited action can be saved in database, false if not.
 */
private fun EditedAction.isValidForSave(editedEvents: List<EditedEvent>): Boolean = when (action) {
    is Action.Click,
    is Action.Swipe,
    is Action.Pause,
    is Action.Intent -> action.isComplete()

    is Action.ToggleEvent -> action.isComplete()
            && toggleEventItemId != INVALID_EDITED_ITEM_ID
            && editedEvents.find { editedEvent -> toggleEventItemId == editedEvent.itemId } != null
}


/**
 * Check end condition validity for database saving.
 * An edited end condition should have a valid edited item id and a valid corresponding event.
 *
 * @param editedEvents the edited events for the scenario of this end condition.
 *
 * @return true if the edited end condition can be saved in database, false if not.
 */
private fun EditedEndCondition.isValidForSave(editedEvents: List<EditedEvent>): Boolean =
    itemId != INVALID_EDITED_ITEM_ID && editedEvents.find { editedEvent -> eventItemId == editedEvent.itemId } != null

/** Invalid edited item id. */
const val INVALID_EDITED_ITEM_ID = -1
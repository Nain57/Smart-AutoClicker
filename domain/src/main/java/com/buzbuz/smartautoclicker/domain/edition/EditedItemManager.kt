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

import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.domain.model.scenario.Scenario

/** Handles the creation of edited items, allowing to uniquely identify an item during scenario edition. */
internal class EditedItemManager {

    /** The last generated item id for an Action. */
    private var lastGeneratedActionItemId: Int = 0
    /** The last generated item id for an End Condition. */
    private var lastGeneratedEndConditionItemId: Int = 0
    /** The last generated item id for an Event. */
    private var lastGeneratedEventItemId: Int = 0

    /** Creates a new configured scenario. */
    fun createConfiguredScenario(
        scenario: Scenario,
        endConditions: List<EndCondition>,
        events: List<Event>
    ): EditedScenario {

        val eventsIdToItemIdMap = mutableMapOf<Long, Int>()
        events.forEach { event -> eventsIdToItemIdMap[event.id] = ++lastGeneratedEventItemId }

        val editedEvents: List<EditedEvent> = events.map { event ->
            createNewConfiguredEvent(event, eventsIdToItemIdMap)
        }
        val editedEndConditions: List<EditedEndCondition> = endConditions.map { endCondition ->
            eventsIdToItemIdMap[endCondition.eventId]?.let { eventItemId ->
                createNewConfiguredEndCondition(endCondition, eventItemId)
            } ?: throw IllegalStateException("Event item id can't be found.")
        }

        return EditedScenario(
            scenario = scenario,
            events = editedEvents,
            endConditions = editedEndConditions,
        )
    }

    /** Creates a new configured End Condition. */
    fun createNewConfiguredEndCondition(
        endCondition: EndCondition,
        eventItemId: Int = INVALID_EDITED_ITEM_ID,
    ) = EditedEndCondition(
        endCondition = endCondition,
        itemId = ++lastGeneratedEndConditionItemId,
        eventItemId = eventItemId,
    )

    /** Creates a new configured Event. */
    fun createNewConfiguredEvent(event: Event) =
        EditedEvent(
            event = event,
            itemId = ++lastGeneratedEventItemId,
            editedActions = event.actions?.map { action ->
                createNewEditedAction(action)
            } ?: emptyList()
        )

    /** Creates a new configured Event with inner items mapped with an edited Event. */
    private fun createNewConfiguredEvent(event: Event, eventIdsMap: Map<Long, Int>) =
        EditedEvent(
            event = event,
            itemId = eventIdsMap[event.id]!!,
            editedActions = event.actions?.map { action ->
                createNewEditedAction(action, eventIdsMap)
            } ?: emptyList()
        )

    /** Creates a new configured Action. */
    fun createNewEditedAction(action: Action): EditedAction =
        EditedAction(
            action = action,
            itemId = ++lastGeneratedActionItemId,
        )

    /** Creates a new configured Action from another one. */
    fun createNewEditedActionCopy(action: Action, toggleEventItemId: Int): EditedAction =
        EditedAction(
            action = action.copyAndResetId(),
            itemId = ++lastGeneratedActionItemId,
            toggleEventItemId = toggleEventItemId,
        )

    /** Creates a new configured Action with inner items mapped with an edited Event. */
    private fun createNewEditedAction(action: Action, eventIdsMap: Map<Long, Int>): EditedAction =
        if (action is Action.ToggleEvent)
            EditedAction(
                action = action,
                itemId = ++lastGeneratedActionItemId,
                toggleEventItemId = eventIdsMap[action.toggleEventId]!!,
            )
        else createNewEditedAction(action)

    /** Reset the edition items ids. */
    fun resetEditionItemIds() {
        lastGeneratedEndConditionItemId = 0
        lastGeneratedEventItemId = 0
        lastGeneratedActionItemId = 0
    }
}
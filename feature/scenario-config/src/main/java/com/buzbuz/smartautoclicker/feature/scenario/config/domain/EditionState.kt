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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Suppress("UNCHECKED_CAST")
class EditionState internal constructor(private val editor: ScenarioEditor) {

    val scenarioCompleteState: Flow<EditedElementState<ScenarioEditionState>> =
        combine(
            editor.editedScenarioState,
            editor.eventsEditor.listState,
            editor.endConditionsEditor.listState,
        ) { scenario, events, endConditions ->

            if (scenario.value == null || events.value == null || endConditions.value == null)
                return@combine EditedElementState(value = null, hasChanged = false, canBeSaved = false)

            EditedElementState(
                value = ScenarioEditionState(scenario.value, events.value, endConditions.value),
                hasChanged = scenario.hasChanged || events.hasChanged || endConditions.hasChanged,
                canBeSaved = scenario.canBeSaved && events.canBeSaved && endConditions.canBeSaved
            )
        }
    val scenarioState: Flow<EditedElementState<Scenario>> =
        editor.editedScenarioState
    fun getScenario(): Scenario? = editor.editedScenario.value

    val eventsState: Flow<EditedElementState<List<Event>>> =
        editor.eventsEditor.listState
    val editedEventState: Flow<EditedElementState<Event>> =
        editor.eventsEditor.editedItemState
    fun getEditedEvent(): Event? = editor.eventsEditor.editedItem.value

    val editedEventConditionsState: Flow<EditedElementState<List<Condition>>> =
        editor.eventsEditor.conditionsEditor.listState
    val editedConditionState: Flow<EditedElementState<Condition>> =
        editor.eventsEditor.conditionsEditor.editedItemState
    fun getEditedCondition(): Condition? = editor.eventsEditor.conditionsEditor.editedItem.value

    val editedEventActionsState: Flow<EditedElementState<List<Action>>> =
        editor.eventsEditor.actionsEditor.listState
    val editedActionState: Flow<EditedElementState<Action>> =
        editor.eventsEditor.actionsEditor.editedItemState
    fun <T : Action> getEditedAction(): T? =
        editor.eventsEditor.actionsEditor.editedItem.value?.let { it as T }

    val editedActionIntentExtrasState: Flow<EditedElementState<List<IntentExtra<out Any>>>> =
        editor.eventsEditor.actionsEditor.intentExtraEditor.listState
    val editedIntentExtraState: Flow<EditedElementState<IntentExtra<out Any>>> =
        editor.eventsEditor.actionsEditor.intentExtraEditor.editedItemState
    val eventsAvailableForToggleEventAction: Flow<List<Event>> =
        combine(eventsState, editedEventState) { scenarioEvents, editedEvent ->
            buildList {
                val isEditedEventInList = scenarioEvents.value
                    ?.let { find { scenarioEvent -> scenarioEvent.id == editedEvent.value?.id } != null }
                    ?: false

                if (!isEditedEventInList && editedEvent.value != null) add(editedEvent.value)
                scenarioEvents.value?.let { addAll(it) }
            }
        }
    fun getEditedIntentExtra(): IntentExtra<out Any>? =
        editor.eventsEditor.actionsEditor.intentExtraEditor.editedItem.value

    val endConditionsState: Flow<EditedElementState<List<EndCondition>>> =
        editor.endConditionsEditor.listState
    val editedEndConditionState: Flow<EditedElementState<EndCondition>> =
        editor.endConditionsEditor.editedItemState
    val eventsAvailableForNewEndCondition: Flow<List<Event>> =
        combine(eventsState, endConditionsState) { events, endConditions,  ->
            if (endConditions.value.isNullOrEmpty()) events.value ?: emptyList()
            else events.value?.filter { event ->
                endConditions.value.find { endCondition ->
                    endCondition.eventId == event.id
                } == null
            } ?: emptyList()
        }
    fun getEditedEndCondition(): EndCondition? = editor.endConditionsEditor.editedItem.value

    /** Check if the edited Event is referenced by an EndCondition in the edited scenario. */
    fun isEditedEventReferencedByEndCondition(): Boolean {
        val event = editor.eventsEditor.editedItem.value ?: return false
        val endConditions = editor.endConditionsEditor.editedList.value ?: return false

        return endConditions.find { it.eventId == event.id } != null
    }

    /** Check if the edited Event is referenced by an Action in the edited scenario. */
    fun isEditedEventReferencedByAction(): Boolean {
        val event = editor.eventsEditor.editedItem.value ?: return false
        val scenarioEvents = editor.eventsEditor.editedList.value ?: return false

        return scenarioEvents.find { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@find false

            scenarioEvent.actions.find { action ->
                action is Action.ToggleEvent && action.toggleEventId == event.id
            } != null
        } != null
    }
}
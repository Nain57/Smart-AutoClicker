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

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.ScenarioEditionState
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.doesNotContainAction
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.isClickOnCondition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
class EditionState internal constructor(context: Context, private val editor: ScenarioEditor) {

    /** The repository providing access to the database. */
    private val repository: Repository = Repository.getRepository(context)

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

    val eventsState: Flow<EditedListState<Event>> =
        editor.eventsEditor.listState
    val editedEventState: Flow<EditedElementState<Event>> =
        editor.eventsEditor.editedItemState
    fun getEditedEvent(): Event? = editor.eventsEditor.editedItem.value

    val editedEventConditionsState: Flow<EditedListState<Condition>> =
        editor.eventsEditor.conditionsEditor.listState
    val editedConditionState: Flow<EditedElementState<Condition>> =
        editor.eventsEditor.conditionsEditor.editedItemState
    fun getEditedCondition(): Condition? = editor.eventsEditor.conditionsEditor.editedItem.value

    val editedEventActionsState: Flow<EditedListState<Action>> =
        editor.eventsEditor.actionsEditor.listState
    val editedActionState: Flow<EditedElementState<Action>> =
        editor.eventsEditor.actionsEditor.editedItemState
    fun <T : Action> getEditedAction(): T? =
        editor.eventsEditor.actionsEditor.editedItem.value?.let { it as T }

    val editedActionIntentExtrasState: Flow<EditedListState<IntentExtra<out Any>>> =
        editor.eventsEditor.actionsEditor.intentExtraEditor.listState
    val editedIntentExtraState: Flow<EditedElementState<IntentExtra<out Any>>> =
        editor.eventsEditor.actionsEditor.intentExtraEditor.editedItemState
    val eventsAvailableForToggleEventAction: Flow<List<Event>> =
        combine(eventsState, editedEventState) { scenarioEvents, editedEvent ->
            if (editedEvent.value == null || scenarioEvents.value == null) return@combine emptyList()

            buildList {
                val availableEvents = scenarioEvents.value
                    .filter { event -> event.id != editedEvent.value.id }

                add(editedEvent.value)
                addAll(availableEvents)
            }
        }
    fun getEditedIntentExtra(): IntentExtra<out Any>? =
        editor.eventsEditor.actionsEditor.intentExtraEditor.editedItem.value

    val endConditionsState: Flow<EditedListState<EndCondition>> =
        editor.endConditionsEditor.listState
    val editedEndConditionState: Flow<EditedElementState<EndCondition>> =
        editor.endConditionsEditor.editedItemState
    val eventsAvailableForNewEndCondition: Flow<List<Event>> =
        combine(eventsState, endConditionsState) { events, endConditions ->
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

    /**
     * Check if the edited Condition is referenced by a Click Action in the edited event.
     * If the edited event is set to OR, do not consider the reference as true.
     */
    fun isEditedConditionReferencedByClick(): Boolean {
        val event = editor.eventsEditor.editedItem.value ?: return false
        if (event.conditionOperator == OR) return false

        val condition = editor.eventsEditor.conditionsEditor.editedItem.value ?: return false
        val actions = editor.eventsEditor.actionsEditor.editedList.value ?: return false

        return actions.find { action ->
            action is Action.Click && action.clickOnConditionId == condition.id
        } != null
    }

    val editedScenarioOtherActionsForCopy: Flow<List<Action>> =
        editor.eventsEditor.editedList
            .map { events ->
                events ?: return@map emptyList()
                val editedEvent = getEditedEvent() ?: return@map emptyList()

                buildList {
                    events
                        .filter { item -> item.id != editedEvent.id }
                        .forEach { event -> addAll(event.actions.filter { !it.isClickOnCondition() }) }
                }
            }

    val allOtherScenarioActionsForCopy: Flow<List<Action>> =
        combine(editor.eventsEditor.editedItem, editedScenarioOtherActionsForCopy, repository.getAllActions()) { editedEvt, scenarioOthers, allOthers ->
            allOthers.filter { item ->
                !item.isClickOnCondition()
                        && item !is Action.ToggleEvent
                        && editedEvt?.actions?.doesNotContainAction(item) ?: true
                        && scenarioOthers.doesNotContainAction(item)
            }
        }
}
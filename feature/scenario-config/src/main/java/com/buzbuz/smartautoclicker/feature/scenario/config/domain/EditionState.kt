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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
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
            editor.imageEventsEditor.listState,
        ) { scenario, events ->

            if (scenario.value == null || events.value == null)
                return@combine EditedElementState(value = null, hasChanged = false, canBeSaved = false)

            EditedElementState(
                value = ScenarioEditionState(scenario.value, events.value),
                hasChanged = scenario.hasChanged || events.hasChanged,
                canBeSaved = scenario.canBeSaved && events.canBeSaved
            )
        }
    val scenarioState: Flow<EditedElementState<Scenario>> =
        editor.editedScenarioState
    fun getScenario(): Scenario? = editor.editedScenario.value

    val editedImageEventsState: Flow<EditedListState<ImageEvent>> =
        editor.imageEventsEditor.listState.map { listState ->
            listState.copy(value = listState.value?.sortedBy { it.priority } ?: emptyList())
        }
    val editedImageEventState: Flow<EditedElementState<ImageEvent>> =
        editor.imageEventsEditor.editedItemState
    val editedTriggerEventsState: Flow<EditedListState<TriggerEvent>> =
        editor.triggerEventsEditor.listState
    val editedTriggerImageEventState: Flow<EditedElementState<TriggerEvent>> =
        editor.triggerEventsEditor.editedItemState
    val allEditedEvents : Flow<List<Event>> =
        combine(editor.imageEventsEditor.allEditedItems, editor.triggerEventsEditor.allEditedItems) { imageEvent, triggerEvents ->
            buildList {
                addAll(imageEvent)
                addAll(triggerEvents)
            }
        }
    fun getEditedEvent(): Event? =
        editor.imageEventsEditor.editedItem.value ?: editor.triggerEventsEditor.editedItem.value
    fun getEditedImageEvent(): ImageEvent? =
        editor.imageEventsEditor.editedItem.value

    fun getAllEditedEvents(): List<Event> =
        buildList {
            addAll(editor.imageEventsEditor.getAllEditedItems())
            addAll(editor.triggerEventsEditor.getAllEditedItems())
        }
    fun getAllEditedEventsFlow(): Flow<List<Event>> =
        combine(editor.imageEventsEditor.allEditedItems, editor.triggerEventsEditor.allEditedItems) { imageEvents, triggerEvents ->
            buildList {
                addAll(imageEvents)
                addAll(triggerEvents)
            }
        }

    val editedEventConditionsState: Flow<EditedListState<ImageCondition>> =
        editor.imageEventsEditor.conditionsEditor.listState
    val editedConditionState: Flow<EditedElementState<ImageCondition>> =
        editor.imageEventsEditor.conditionsEditor.editedItemState
    fun getEditedCondition(): ImageCondition? = editor.imageEventsEditor.conditionsEditor.editedItem.value

    val editedEventActionsState: Flow<EditedListState<Action>> =
        editor.imageEventsEditor.actionsEditor.listState
    val editedActionState: Flow<EditedElementState<Action>> =
        editor.imageEventsEditor.actionsEditor.editedItemState
    fun <T : Action> getEditedAction(): T? =
        editor.imageEventsEditor.actionsEditor.editedItem.value?.let { it as T }

    val editedActionIntentExtrasState: Flow<EditedListState<IntentExtra<out Any>>> =
        editor.imageEventsEditor.actionsEditor.intentExtraEditor.listState
    val editedIntentExtraState: Flow<EditedElementState<IntentExtra<out Any>>> =
        editor.imageEventsEditor.actionsEditor.intentExtraEditor.editedItemState

    val editedActionEventTogglesState: Flow<EditedListState<EventToggle>> =
        editor.imageEventsEditor.actionsEditor.eventToggleEditor.listState

    fun getEditedEventToggles(): List<EventToggle>? =
        editor.imageEventsEditor.actionsEditor.editedItem.value?.let { action ->
            if (action is Action.ToggleEvent) action.eventToggles
            else null
        }

    val eventsAvailableForToggleEventAction: Flow<List<ImageEvent>> =
        combine(editedImageEventsState, editedImageEventState) { scenarioEvents, editedEvent ->
            if (editedEvent.value == null || scenarioEvents.value == null) return@combine emptyList()

            buildList {
                val availableEvents = scenarioEvents.value
                    .filter { event -> event.id != editedEvent.value.id }

                add(editedEvent.value)
                addAll(availableEvents)
            }
        }
    fun getEditedIntentExtra(): IntentExtra<out Any>? =
        editor.imageEventsEditor.actionsEditor.intentExtraEditor.editedItem.value

    /** Check if the edited Event is referenced by an Action in the edited scenario. */
    fun isEditedEventReferencedByAction(): Boolean {
        val event = editor.imageEventsEditor.editedItem.value ?: return false
        val scenarioEvents = editor.imageEventsEditor.editedList.value ?: return false

        return scenarioEvents.find { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@find false

            scenarioEvent.actions.find { action ->
                action is Action.ToggleEvent && !action.toggleAll && action.eventToggles.find { it.targetEventId == event.id } != null
            } != null
        } != null
    }

    /**
     * Check if the edited Condition is referenced by a Click Action in the edited event.
     * If the edited event is set to OR, do not consider the reference as true.
     */
    fun isEditedConditionReferencedByClick(): Boolean {
        val event = editor.imageEventsEditor.editedItem.value ?: return false
        if (event.conditionOperator == OR) return false

        val condition = editor.imageEventsEditor.conditionsEditor.editedItem.value ?: return false
        val actions = editor.imageEventsEditor.actionsEditor.editedList.value ?: return false

        return actions.find { action ->
            action is Action.Click && action.clickOnConditionId == condition.id
        } != null
    }

    val editedScenarioOtherActionsForCopy: Flow<List<Action>> =
        editor.imageEventsEditor.editedList
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
        combine(editor.imageEventsEditor.editedItem, editedScenarioOtherActionsForCopy, repository.getAllActions()) { editedEvt, scenarioOthers, allOthers ->
            allOthers.filter { item ->
                !item.isClickOnCondition()
                        && item !is Action.ToggleEvent
                        && editedEvt?.actions?.doesNotContainAction(item) ?: true
                        && scenarioOthers.doesNotContainAction(item)
            }
        }
}
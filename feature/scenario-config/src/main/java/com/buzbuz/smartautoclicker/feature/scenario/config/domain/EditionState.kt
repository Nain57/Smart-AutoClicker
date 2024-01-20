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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.EventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.ImageEventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.data.events.TriggerEventsEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.EditedScenarioState
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.IEditionState
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.doesNotContainAction
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.isClickOnCondition

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UNCHECKED_CAST")
internal class EditionState internal constructor(
    context: Context,
    private val editor: ScenarioEditor,
) : IEditionState {

    /** The repository providing access to the database. */
    private val repository: Repository = Repository.getRepository(context)

    override val scenarioCompleteState: Flow<EditedElementState<EditedScenarioState>> =
        combine(
            editor.editedScenarioState,
            editor.editedImageEventListState,
            editor.editedTriggerEventListState,
        ) { scenario, imageEvents, triggerEvents ->

            if (scenario.value == null || imageEvents.value == null || triggerEvents.value == null)
                return@combine EditedElementState(value = null, hasChanged = false, canBeSaved = false)

            EditedElementState(
                value = EditedScenarioState(scenario.value, imageEvents.value, triggerEvents.value),
                hasChanged = scenario.hasChanged || imageEvents.hasChanged || triggerEvents.hasChanged,
                canBeSaved = scenario.canBeSaved && imageEvents.canBeSaved && triggerEvents.canBeSaved,
            )
        }

    override val scenarioState: Flow<EditedElementState<Scenario>> =
        editor.editedScenarioState

    override val editedImageEventsState: Flow<EditedListState<ImageEvent>> =
        editor.editedImageEventListState.map { listState ->
            listState.copy(value = listState.value?.sortedBy { it.priority } ?: emptyList())
        }

    override val editedTriggerEventsState: Flow<EditedListState<TriggerEvent>> =
        editor.editedTriggerEventListState

    override val editedImageEventState: Flow<EditedElementState<ImageEvent>> =
        editor.editedImageEventState

    override val editedTriggerEventState: Flow<EditedElementState<TriggerEvent>> =
        editor.editedTriggerEventState

    override val allEditedEvents : Flow<List<Event>> =
        editor.allEditedEvents

    override val editedEventState: Flow<EditedElementState<Event>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.editedItemState ?: emptyFlow()
        }

    override val editedEventConditionsState: Flow<EditedListState<Condition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.conditionsEditor?.listState ?: emptyFlow()
        }

    override val editedEventImageConditionsState: Flow<EditedListState<ImageCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            val editor = (eventEditor as EventsEditor<*, *>)
            if (editor is ImageEventsEditor)
                editor.conditionsEditor.listState
            else emptyFlow()
        }

    override val editedImageConditionState: Flow<EditedElementState<ImageCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            val editor = (eventEditor as EventsEditor<*, *>)
            if (editor is ImageEventsEditor)
                editor.conditionsEditor.editedItemState
            else emptyFlow()
        }

    override val editedEventTriggerConditionsState: Flow<EditedListState<TriggerCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            val editor = (eventEditor as EventsEditor<*, *>)
            if (editor is TriggerEventsEditor)
                editor.conditionsEditor.listState
            else emptyFlow()
        }

    override val editedTriggerConditionState: Flow<EditedElementState<TriggerCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            val editor = (eventEditor as EventsEditor<*, *>)
            if (editor is TriggerEventsEditor)
                editor.conditionsEditor.editedItemState
            else emptyFlow()
        }

    override val editedEventActionsState: Flow<EditedListState<Action>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.actionsEditor?.listState ?: emptyFlow()
        }

    override val editedActionState: Flow<EditedElementState<Action>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.actionsEditor?.editedItemState ?: emptyFlow()
        }

    override val editedActionIntentExtrasState: Flow<EditedListState<IntentExtra<out Any>>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.actionsEditor?.intentExtraEditor?.listState  ?: emptyFlow()
        }

    override val editedIntentExtraState: Flow<EditedElementState<IntentExtra<out Any>>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.actionsEditor?.intentExtraEditor?.editedItemState  ?: emptyFlow()
        }

    override val editedActionEventTogglesState: Flow<EditedListState<EventToggle>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor?.actionsEditor?.eventToggleEditor?.listState  ?: emptyFlow()
        }

    override val eventsAvailableForToggleEventAction: Flow<List<Event>> =
        combine(allEditedEvents, editedEventState) { scenarioEvents, editedEvent ->
            if (editedEvent.value == null || scenarioEvents.isEmpty()) return@combine emptyList()

            buildList {
                val availableEvents = scenarioEvents
                    .filter { event -> event.id != editedEvent.value.id }

                add(editedEvent.value)
                addAll(availableEvents)
            }
        }

    override val actionsAvailableForCopyFromEditedScenario: Flow<List<Action>> =
        editor.allEditedEvents
            .map { events ->
                val editedEvent = getEditedEvent<Event>() ?: return@map emptyList()

                buildList {
                    events
                        .filter { item -> item.id != editedEvent.id }
                        .forEach { event -> addAll(event.actions.filter { !it.isClickOnCondition() }) }
                }
            }

    override val actionsAvailableForCopyFromOtherScenario: Flow<List<Action>> =
        combine(
            editor.editedEvent,
            actionsAvailableForCopyFromEditedScenario,
            repository.getAllActions(),
        ) { editedEvt, scenarioOthers, allOthers ->
            allOthers.filter { item ->
                !item.isClickOnCondition()
                        && item !is Action.ToggleEvent
                        && editedEvt?.actions?.doesNotContainAction(item) ?: true
                        && scenarioOthers.doesNotContainAction(item)
            }
        }

    override fun getScenario(): Scenario? =
        editor.editedScenario.value

    override fun getAllEditedEvents(): List<Event> =
        editor.getAllEditedEvents()

    override fun <T : Event> getEditedEvent(): T? =
        editor.currentEventEditor.value?.editedItem?.value as? T

    override fun <T : Condition> getEditedCondition(): T? =
        editor.currentEventEditor.value?.conditionsEditor?.editedItem?.value as T?

    override fun <T : Action> getEditedAction(): T? =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value as T?

    override fun getEditedIntentExtra(): IntentExtra<out Any>? =
        editor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.editedItem?.value

    override fun getEditedActionEventToggles(): List<EventToggle>? =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value?.let { action ->
            if (action is Action.ToggleEvent) action.eventToggles
            else null
        }

    override fun isEventIdValidInEditedScenario(eventId: Identifier): Boolean =
        getAllEditedEvents().find { eventId == it.id } != null

    override fun isEditedEventReferencedByAction(): Boolean {
        val event = getEditedEvent<Event>() ?: return false
        val scenarioEvents = getAllEditedEvents()

        return scenarioEvents.find { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@find false

            scenarioEvent.actions.find { action ->
                action is Action.ToggleEvent && !action.toggleAll && action.eventToggles.find { it.targetEventId == event.id } != null
            } != null
        } != null
    }

    override fun isEditedConditionReferencedByClick(): Boolean {
        val event = getEditedEvent<Event>() ?: return false
        if (event.conditionOperator == OR) return false

        val condition = getEditedCondition<Condition>() ?: return false
        val actions = editor.currentEventEditor.value?.actionsEditor?.editedList?.value ?: return false

        return actions.find { action ->
            action is Action.Click && action.clickOnConditionId == condition.id
        } != null
    }
}
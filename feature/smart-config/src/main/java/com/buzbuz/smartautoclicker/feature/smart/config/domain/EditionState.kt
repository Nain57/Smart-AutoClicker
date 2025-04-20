/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.sortedByPriority
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.data.events.EventsEditor
import com.buzbuz.smartautoclicker.feature.smart.config.data.events.ScreenEventsEditor
import com.buzbuz.smartautoclicker.feature.smart.config.data.events.TriggerEventsEditor
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedElementState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedScenarioState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class EditionState internal constructor(
    repository: IRepository,
    private val editor: com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor,
) : IEditionState {

    override val scenarioCompleteState: Flow<EditedElementState<EditedScenarioState>> =
        combine(
            editor.editedScenarioState,
            editor.editedScreenEventListState,
            editor.editedTriggerEventListState,
        ) { scenario, imageEvents, triggerEvents ->

            if (scenario.value == null || imageEvents.value == null || triggerEvents.value == null)
                return@combine EditedElementState(value = null, hasChanged = false, canBeSaved = false)

            EditedElementState(
                value = EditedScenarioState(scenario.value, imageEvents.value, triggerEvents.value),
                hasChanged = scenario.hasChanged || imageEvents.hasChanged || triggerEvents.hasChanged,
                canBeSaved = scenario.canBeSaved && imageEvents.canBeSaved && triggerEvents.canBeSaved
                        && (imageEvents.value.isNotEmpty() || triggerEvents.value.isNotEmpty()),
            )
        }

    override val scenarioState: Flow<EditedElementState<Scenario>> =
        editor.editedScenarioState

    override val editedScreenEventsState: Flow<EditedListState<ScreenEvent>> =
        editor.editedScreenEventListState.map { listState ->
            listState.copy(value = listState.value?.sortedByPriority()?.toList() ?: emptyList())
        }

    override val editedTriggerEventsState: Flow<EditedListState<TriggerEvent>> =
        editor.editedTriggerEventListState

    override val editedScreenEventState: Flow<EditedElementState<ScreenEvent>> =
        editor.editedScreenEventState

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

    override val editedEventScreenConditionsState: Flow<EditedListState<ScreenCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor ?: return@flatMapLatest emptyFlow()
            val editor = (eventEditor as EventsEditor<*, *>)

            if (editor is ScreenEventsEditor)
                editor.conditionsEditor.listState
            else emptyFlow()
        }

    override val editedScreenConditionState: Flow<EditedElementState<ScreenCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor ?: return@flatMapLatest emptyFlow()
            val editor = (eventEditor as EventsEditor<*, *>)

            if (editor is ScreenEventsEditor)
                editor.conditionsEditor.editedItemState
            else emptyFlow()
        }

    override val editedEventTriggerConditionsState: Flow<EditedListState<TriggerCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor ?: return@flatMapLatest emptyFlow()
            val editor = (eventEditor as EventsEditor<*, *>)

            if (editor is TriggerEventsEditor)
                editor.conditionsEditor.listState
            else emptyFlow()
        }

    override val editedTriggerConditionState: Flow<EditedElementState<TriggerCondition>> =
        editor.currentEventEditor.flatMapLatest { eventEditor ->
            eventEditor ?: return@flatMapLatest emptyFlow()
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

    override val screenEventsForCopy: Flow<List<ScreenEvent>> =
        combine(editedScreenEventsState, repository.allScreenEvents) { allEditedEvents, dbEvents ->
            buildList {
                val scenarioEvents = allEditedEvents.value?.getEditedImageEventsForCopy() ?: emptyList()
                addAll(scenarioEvents)
                addAll(dbEvents.filterForImageEventCopy(scenarioEvents))
            }
        }

    override val triggerEventsForCopy: Flow<List<TriggerEvent>> =
        combine(editedTriggerEventsState, repository.allTriggerEvents) { allEditedEvents, dbEvents ->
            buildList {
                val scenarioEvents = allEditedEvents.value?.getEditedTriggerEventsForCopy() ?: emptyList()
                addAll(scenarioEvents)
                addAll(dbEvents.filterForTriggerEventCopy(scenarioEvents))
            }
        }

    override val conditionsForCopy: Flow<List<Condition>> =
        combine(editor.editedEvent, allEditedEvents, repository.allConditions) { editedEvent, allEditedEvents, dbConditions ->
            if (editedEvent == null) return@combine emptyList<Condition>()
            buildList {
                val editedConditions = allEditedEvents.getEditedConditionsForCopy(editedEvent)
                addAll(editedConditions)
                addAll(dbConditions.filterConditionsForCopy(editedEvent, editedConditions))
            }.distinctBy { item -> item.hashCodeNoIds() }
        }

    override val actionsForCopy: Flow<List<Action>> =
        combine(editor.editedEvent, allEditedEvents, repository.allActions) { editedEvent, allEditedEvents, dbActions ->
            buildList {
                editedEvent ?: return@buildList
                val editedActions = allEditedEvents.getEditedActionsForCopy(editedEvent)
                addAll(editedActions)
                addAll(dbActions.filterActionsForCopy(editedEvent, editedActions))
            }.distinctBy { item -> item.hashCodeNoIds() }
        }

    override val canCopyImageEvents: Flow<Boolean> =
        screenEventsForCopy.map { it.isNotEmpty() }

    override val canCopyTriggerEvents: Flow<Boolean> =
        triggerEventsForCopy.map { it.isNotEmpty() }

    override val canCopyConditions: Flow<Boolean> =
        conditionsForCopy.map { it.isNotEmpty() }

    override val canCopyActions: Flow<Boolean> =
        actionsForCopy.map { it.isNotEmpty() }

    override fun getScenario(): Scenario? =
        editor.editedScenario.value

    override fun getAllEditedEvents(): List<Event> =
        editor.getAllEditedEvents()

    @Suppress("UNCHECKED_CAST")

    override fun <T : Event> getEditedEvent(): T? =
        editor.currentEventEditor.value?.editedItem?.value as? T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Action> getEditedEventActions(): List<T>? =
        editor.currentEventEditor.value?.actionsEditor?.editedList?.value as List<T>?

    @Suppress("UNCHECKED_CAST")
    override fun <T : Condition> getEditedEventConditions(): List<T>? =
        editor.currentEventEditor.value?.conditionsEditor?.editedList?.value as List<T>?

    @Suppress("UNCHECKED_CAST")

    override fun <T : Condition> getEditedCondition(): T? =
        editor.currentEventEditor.value?.conditionsEditor?.editedItem?.value as T?

    @Suppress("UNCHECKED_CAST")

    override fun <T : Action> getEditedAction(): T? =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value as T?

    override fun getEditedIntentExtra(): IntentExtra<out Any>? =
        editor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.editedItem?.value

    override fun getEditedActionEventToggles(): List<EventToggle>? =
        editor.currentEventEditor.value?.actionsEditor?.editedItem?.value?.let { action ->
            if (action is ToggleEvent) action.eventToggles
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
                action is ToggleEvent && !action.toggleAll && action.eventToggles.find { it.targetEventId == event.id } != null
            } != null
        } != null
    }

    override fun isEditedConditionReferencedByClick(): Boolean {
        val event = getEditedEvent<Event>() ?: return false
        if (event.conditionOperator == OR) return false

        val condition = getEditedCondition<Condition>() ?: return false
        val actions = editor.currentEventEditor.value?.actionsEditor?.editedList?.value ?: return false

        return actions.find { action ->
            action is Click && action.clickOnConditionId == condition.id
        } != null
    }
}
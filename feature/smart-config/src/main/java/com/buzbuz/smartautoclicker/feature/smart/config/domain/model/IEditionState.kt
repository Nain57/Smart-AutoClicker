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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.model

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.Flow

interface IEditionState {

    // Edited Scenario
    val scenarioCompleteState: Flow<EditedElementState<EditedScenarioState>>
    val scenarioState: Flow<EditedElementState<Scenario>>

    // Edited Scenario child items (ScreenEvents and TriggerEvents)
    val allEditedEvents: Flow<List<Event>>
    val editedScreenEventsState: Flow<EditedListState<ScreenEvent>>
    val editedTriggerEventsState: Flow<EditedListState<TriggerEvent>>

    // Edited Event
    val editedEventState: Flow<EditedElementState<Event>>
    val editedScreenEventState: Flow<EditedElementState<ScreenEvent>>
    val editedTriggerEventState: Flow<EditedElementState<TriggerEvent>>

    // Edited Event child conditions
    val editedEventConditionsState: Flow<EditedListState<Condition>>
    val editedEventScreenConditionsState: Flow<EditedListState<ScreenCondition>>
    val editedScreenConditionState: Flow<EditedElementState<ScreenCondition>>
    val editedEventTriggerConditionsState: Flow<EditedListState<TriggerCondition>>
    val editedTriggerConditionState: Flow<EditedElementState<TriggerCondition>>

    // Edited Event child Actions
    val editedEventActionsState: Flow<EditedListState<Action>>
    val editedActionState: Flow<EditedElementState<Action>>

    // Edited Actions child items (IntentExtra and EventToggles)
    val editedActionIntentExtrasState: Flow<EditedListState<IntentExtra<out Any>>>
    val editedIntentExtraState: Flow<EditedElementState<IntentExtra<out Any>>>
    val editedActionEventTogglesState: Flow<EditedListState<EventToggle>>

    // Copy possibility state depending on current edition state
    val canCopyImageEvents: Flow<Boolean>
    val canCopyTriggerEvents: Flow<Boolean>
    val canCopyConditions: Flow<Boolean>
    val canCopyActions: Flow<Boolean>

    // Possible items for copy depending on current edition state
    val screenEventsForCopy: Flow<List<ScreenEvent>>
    val triggerEventsForCopy: Flow<List<TriggerEvent>>
    val conditionsForCopy: Flow<List<Condition>>
    val actionsForCopy: Flow<List<Action>>

    // Edited items getters
    fun getScenario(): Scenario?
    fun getAllEditedEvents(): List<Event>
    fun <T : Event> getEditedEvent(): T?
    fun <T : Action> getEditedEventActions(): List<T>?
    fun <T : Condition> getEditedEventConditions(): List<T>?
    fun <T : Condition> getEditedCondition(): T?
    fun <T : Action> getEditedAction(): T?
    fun getEditedIntentExtra(): IntentExtra<out Any>?
    fun getEditedActionEventToggles(): List<EventToggle>?



    /** Check if this event id is set for one of the edited events. */
    fun isEventIdValidInEditedScenario(eventId: Identifier): Boolean

    /** Check if the edited Event is referenced by an Action in the edited scenario. */
    fun isEditedEventReferencedByAction(): Boolean

    /**
     * Check if the edited Condition is referenced by a Click Action in the edited event.
     * If the edited event is set to OR, do not consider the reference as true.
     */
    fun isEditedConditionReferencedByClick(): Boolean
}
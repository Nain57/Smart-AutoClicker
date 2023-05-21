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
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow

/** Repository handling the user edition of a scenario. */
class EditionRepository private constructor(context: Context) {

    companion object {

        /** Singleton preventing multiple instances of the EditionRepository at the same time. */
        @Volatile
        private var INSTANCE: EditionRepository? = null

        /**
         * Get the EditionRepository singleton, or instantiates it if it wasn't yet.
         * @param context the Android context.
         * @return the EditionRepository singleton.
         */
        fun getInstance(context: Context): EditionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EditionRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** The repository providing access to the database. */
    private val repository: Repository = Repository.getRepository(context)
    /** */
    private val scenarioEditor: ScenarioEditor = ScenarioEditor()

    /**
     * The scenario currently edited by the user.
     * Set as the database scenario when starting the edition with [startEdition], it will contains all modifications
     * made by the user.
     */
    val editedScenario: StateFlow<Scenario?> = scenarioEditor.editedValue
    /** The event list for scenario currently edited by the user. */
    val editedEvents: StateFlow<List<Event>?> = scenarioEditor.eventsEditor.editedValue
    /** The event of the event list currently edited. Defined with [startEventEdition]. */
    val editedEvent: StateFlow<Event?> = scenarioEditor.eventsEditor.eventEditor.editedValue
    /** The list of end conditions currently edited by the user. */
    val editedEndConditions: StateFlow<List<EndCondition>?> = scenarioEditor.endConditionsEditor.editedValue

    /** Tells if the scenario have changed since the edition start. */
    val isEditedScenarioContainsChanges: Flow<Boolean> = scenarioEditor.containsChange
    /** Tells if the event have changed since the edition start. */
    val isEditedEventContainsChanges: Flow<Boolean> = scenarioEditor.eventsEditor.eventEditor.containsChange

    /** Tells if the event list is not empty and contains only complete events. */
    val isEventListValid: Flow<Boolean> = scenarioEditor.eventsEditor.isEventListValid

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = scenarioEditor.editedValue.map { it == null }

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long) {
        scenarioEditor.startEdition(
            ScenarioEditor.Reference(
                scenario = repository.getScenario(scenarioId),
                events = repository.getEvents(scenarioId),
                endConditions = repository.getEndConditions(scenarioId),
            )
        )
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        repository.updateScenario(
            scenario = scenarioEditor.getEditedValueOrThrow(),
            events = scenarioEditor.eventsEditor.getEditedValueOrThrow(),
            endConditions = scenarioEditor.endConditionsEditor.getEditedValueOrThrow(),
        )
        scenarioEditor.finishEdition()
    }

    /** Cancel all changes made during the edition. */
    fun cancelEditions() {
        scenarioEditor.finishEdition()
    }

    /** Update the currently edited scenario. */
    fun updateEditedScenario(scenario: Scenario) {
        scenarioEditor.updateEditedValue(scenario)
    }

    /**
     * Update the priority of the events in the scenario.
     * @param newEvents the events, ordered by their new priorities.
     */
    fun updateEventsOrder(newEvents: List<Event>) {
        scenarioEditor.updateEventsOrder(newEvents)
    }

    /**
     * Create a new edited event.
     *
     */
    fun createNewEvent(context: Context, from: Event?) =
        scenarioEditor.createNewEvent(context, from)

    /** Set the event currently edited. */
    fun startEventEdition(event: Event) {
        scenarioEditor.setEditedEvent(event)
    }

    fun updateEditedEvent(event: Event) {
        scenarioEditor.updateEditedEvent(event)
    }

    fun createNewCondition(context: Context, area: Rect?, bitmap: Bitmap?, from: Condition?) =
        scenarioEditor.eventsEditor.eventEditor.createCondition(context, area, bitmap, from)

    fun upsertConditionToEditedEvent(condition: Condition) {
        scenarioEditor.eventsEditor.eventEditor.upsertCondition(condition)
    }

    /**
     * Remove a condition from the edited event.
     * @param condition the condition to be removed.
     */
    fun deleteConditionFromEditedEvent(condition: Condition) {
        scenarioEditor.eventsEditor.eventEditor.deleteCondition(condition)
    }

    fun createNewClick(context: Context, from: Action.Click?) =
        scenarioEditor.eventsEditor.eventEditor.createNewClick(context, from)

    fun createNewSwipe(context: Context, from: Action.Swipe?) =
        scenarioEditor.eventsEditor.eventEditor.createNewSwipe(context, from)

    fun createNewPause(context: Context, from: Action.Pause?) =
        scenarioEditor.eventsEditor.eventEditor.createNewPause(context, from)

    fun createNewIntent(context: Context, from: Action.Intent?) =
        scenarioEditor.eventsEditor.eventEditor.createNewIntent(context, from)

    fun createNewIntentExtra(actionId: Identifier) =
        scenarioEditor.eventsEditor.eventEditor.createNewIntentExtra(actionId)

    fun createNewToggleEvent(context: Context, from: Action.ToggleEvent?) =
        scenarioEditor.eventsEditor.eventEditor.createNewToggleEvent(context, from)

    /**
     * Add a new action to the edited event.
     * @param action the new action.
     */
    fun upsertActionToEditedEvent(action: Action) {
        scenarioEditor.eventsEditor.eventEditor.upsertAction(action)
    }

    /**
     * Remove an action from the edited event.
     * @param action the action to be removed.
     */
    fun removeActionFromEditedEvent(action: Action) {
        scenarioEditor.eventsEditor.eventEditor.deleteAction(action)
    }

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionsOrder(actions: List<Action>) {
        scenarioEditor.eventsEditor.eventEditor.updateActionsOrder(actions)
    }

    /** Check if the [editedEvent] is referenced by an end condition in the edited scenario. */
    fun isEditedEventUsedByEndCondition(): Boolean {
        val event = editedEvent.value ?: return false
        val endConditions = editedEndConditions.value ?: return false

        return endConditions.find { it.eventId == event.id } != null
    }

    /** Check if the [editedEvent] is referenced by an action in the edited scenario. */
    fun isEditedEventUsedByAction(): Boolean {
        val event = editedEvent.value ?: return false
        val scenarioEvents = editedEvents.value ?: return false

        return scenarioEvents.find { scenarioEvent ->
            if (scenarioEvent.id == event.id) return@find false

            scenarioEvent.actions.find { action ->
                action is Action.ToggleEvent && action.toggleEventId == event.id
            } != null
        } != null
    }

    /**
     * Add or update the edited event to the edited scenario.
     * If the event id is unset, it will be added. If not, updated.
     */
    fun commitEditedEvent() {
        scenarioEditor.commitEditedEvent()
    }

    fun deleteEditedEvent() {
        scenarioEditor.deleteEditedEvent()
    }

    fun discardEditedEvent() {
        scenarioEditor.discardEditedEvent()
    }

    fun createNewEndCondition(from: EndCondition?) =
        scenarioEditor.createNewEndCondition(from)

    /**
     * Insert/Update a new end condition to the edited event.
     * @param endCondition the new condition.
     */
    fun upsertEndCondition(endCondition: EndCondition) {
        scenarioEditor.upsertEndCondition(endCondition)
    }

    fun deleteEndCondition(endCondition: EndCondition) {
        scenarioEditor.deleteEndCondition(endCondition)
    }
}
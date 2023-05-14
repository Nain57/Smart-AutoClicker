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

import android.content.Context

import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.model.scenario.Scenario

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Repository handling the user edition of a scenario. */
@OptIn(ExperimentalCoroutinesApi::class)
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
    private val editedItemManager: EditedItemManager = EditedItemManager()

    /**
     * The unique identifier of the scenario currently used/configured.
     * Set with [setConfiguredScenario].
     */
    private val configuredScenarioId: MutableStateFlow<Long?> = MutableStateFlow(null)

    /** The current state of the configured scenario with its end conditions in the database. */
    private val dbScenarioWithEndConditions = configuredScenarioId
        .filterNotNull()
        .flatMapLatest { id -> repository.getScenarioWithEndConditionsFlow(id) }
    /** The current list of events of the configured scenario in the database. */
    private val dbEvents = configuredScenarioId
        .filterNotNull()
        .flatMapLatest { id -> repository.getCompleteEventListFlow(id) }

    /**
     * The scenario currently edited by the user.
     * Set as the dbScenario when starting the edition with [startEditions], it will contains all modifications
     * made by the user.
     */
    private val _editedScenario: MutableStateFlow<EditedScenario?> = MutableStateFlow(null)
    /** The scenario currently edited by the user. */
    val editedScenario: StateFlow<EditedScenario?> = _editedScenario
    /** The event list for scenario currently edited by the user. */
    val editedEvents: Flow<List<EditedEvent>> = _editedScenario.map { confScenario ->
        confScenario?.events ?: emptyList()
    }

    /**
     * The event currently configured by the user.
     * Set as the selected event via [startEventEdition], it will contains all modifications made by the user.
     */
    private val _editedEvent: MutableStateFlow<EditedEvent?> = MutableStateFlow(null)
    val editedEvent: StateFlow<EditedEvent?> = _editedEvent

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = _editedScenario.map { it == null }

    /** Set the scenario to be configured. */
    suspend fun setConfiguredScenario(scenarioId: Long) {
        configuredScenarioId.emit(scenarioId)
    }

    /** Start editing the configured scenario. */
    suspend fun startEditions() {
        val scenarioWithEndConditions = dbScenarioWithEndConditions.firstOrNull()
            ?: throw IllegalStateException("Can't start editions, there is no configured scenario set")
        val events = dbEvents.firstOrNull() ?: emptyList()

        editedItemManager.resetEditionItemIds()

        _editedScenario.emit(
            editedItemManager.createConfiguredScenario(
                scenario = scenarioWithEndConditions.first,
                endConditions = scenarioWithEndConditions.second,
                events = events,
            )
        )
    }

    /** Cancel all changes made during the edition. */
    suspend fun cancelEditions() {
        _editedScenario.emit(null)
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        _editedScenario.apply {
            value?.let { repository.updateScenario(it) }
            emit(null)
        }
    }

    /** Update the currently edited scenario. */
    fun updateEditedScenario(scenario: Scenario) {
        _editedScenario.value = _editedScenario.value?.copy(scenario = scenario)
    }

    /**
     * Update the priority of the events in the scenario.
     *
     * @param events the events, ordered by their new priorities. They must be in the current scenario and have a
     *               defined id.
     */
    fun updateEventsPriority(events: List<EditedEvent>) {
        updateEditedEventItems(events)
    }

    /** @return a new empty end condition. */
    fun createNewEndCondition(): EditedEndCondition =
        editedScenario.value?.let { confScenario ->
            editedItemManager.createNewConfiguredEndCondition(
                EndCondition(scenarioId = confScenario.scenario.id)
            )
        } ?: throw IllegalStateException("No scenario defined !")

    /**
     * Add a new end condition to the scenario.
     * @param confEndCondition the end condition to be added.
     */
    fun addEndCondition(confEndCondition: EditedEndCondition) {
        if (confEndCondition.endCondition.id != 0L) return

        editedScenario.value?.let { conf ->
            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply {
                    if (confEndCondition.itemId == INVALID_EDITED_ITEM_ID
                        || confEndCondition.eventItemId == INVALID_EDITED_ITEM_ID) {
                        throw IllegalStateException("Event item id can't be found.")
                    }

                    add(confEndCondition)
                }
            )
        }
    }

    /**
     * Update an end condition from the scenario.
     * @param confEndCondition the end condition to be updated.
     */
    fun updateEndCondition(confEndCondition: EditedEndCondition) {
        editedScenario.value?.let { conf ->
            val index = conf.endConditions.indexOfFirst { it.itemId == confEndCondition.itemId }
            if (index == -1) throw IllegalStateException("Can't update end condition, itemId not found.")

            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply { set(index, confEndCondition) }
            )
        }
    }

    /**
     * Delete a end condition from the scenario.
     * @param confEndCondition the end condition to be removed.
     */
    fun deleteEndCondition(confEndCondition: EditedEndCondition) {
        editedScenario.value?.let { conf ->
            val index = conf.endConditions.indexOfFirst { it.itemId == confEndCondition.itemId }
            if (index == -1) throw IllegalStateException("Can't delete end condition, itemId not found.")

            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply { removeAt(index) }
            )
        }
    }

    /** Create a new edited event. */
    fun createNewEvent(event: Event): EditedEvent =
        editedItemManager.createNewConfiguredEvent(event)

    /** Set the event currently edited. */
    fun startEventEdition(event: EditedEvent) {
        _editedEvent.value = event
    }

    /** Update the currently edited event. */
    fun updateEditedEvent(event: Event) {
        _editedEvent.value = _editedEvent.value?.copy(event = event)
    }

    /**
     * Add or update the edited event to the edited scenario.
     * If the event id is unset, it will be added. If not, updated.
     */
    fun commitEditedEventToEditedScenario() {
        val item = _editedEvent.value ?: return
        val items = (editedScenario.value?.events ?: emptyList()).toMutableList()

        items.indexOfFirst { other -> item.itemId == other.itemId }.let { itemIndex ->
            if (itemIndex == -1) items.add(item)
            else items[itemIndex] = item
        }

        updateEditedEventItems(items)
        _editedEvent.value = null
    }

    /** Check if the [editedEvent] is referenced by an end condition in the edited scenario. */
    fun isEditedEventUsedByEndCondition(): Boolean {
        val item = _editedEvent.value ?: return false
        val endConditions = editedScenario.value?.endConditions ?: return false

        return endConditions.find { it.eventItemId == item.itemId } != null
    }

    /** Check if the [editedEvent] is referenced by an action in the edited scenario. */
    fun isEditedEventUsedByAction(): Boolean {
        val item = _editedEvent.value ?: return false
        val events = editedScenario.value?.events ?: return false

        return events.find { event ->
            if (event.itemId == item.itemId) return@find false

            event.editedActions.find { action ->
                action.toggleEventItemId == item.itemId
            } != null
        } != null
    }

    /** Delete the edited event from the edited scenario. */
    fun deleteEditedEventFromEditedScenario() {
        val scenario = editedScenario.value ?: return
        val item = _editedEvent.value ?: return

        // Remove any end condition referencing the event to be deleted
        val correctedEndConditions = editedScenario.value?.endConditions?.toMutableList() ?: mutableListOf()
        editedScenario.value?.endConditions?.forEach { endCondition ->
            if (endCondition.itemId == item.itemId) correctedEndConditions.remove(endCondition)
        }

        // Remove any action referencing the event to be deleted
        val correctedEvents = editedScenario.value?.events?.toMutableList() ?: mutableListOf()
        for (evtIndex in correctedEvents.indices) {
            val correctedActions = correctedEvents[evtIndex].editedActions.toMutableList()
            correctedEvents[evtIndex].editedActions.forEach { editedAction ->
                if (editedAction.toggleEventItemId == item.itemId) {
                    correctedActions.remove(editedAction)
                }
            }
            correctedEvents[evtIndex] = correctedEvents[evtIndex].copy(
                event = correctedEvents[evtIndex].event.copy(actions = correctedActions.map { it.action }.toMutableList()),
                editedActions = correctedActions,
            )
        }

        // Remove event from list
        correctedEvents.indexOfFirst { other -> item.itemId == other.itemId }.let { itemIndex ->
            if (itemIndex == -1) return
            correctedEvents.removeAt(itemIndex)
        }

        _editedScenario.value = scenario.copy(
            scenario = scenario.scenario.copy(eventCount = correctedEvents.size),
            events = correctedEvents,
            endConditions = correctedEndConditions,
        )
        _editedEvent.value = null
    }

    /**
     * Add a new condition to the edited event.
     * @param condition the new condition.
     */
    fun addConditionToEditedEvent(condition: Condition) {
        editedEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions.add(condition)

            _editedEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
        }
    }

    /**
     * Update a condition in the edited event.
     * @param condition the updated condition.
     */
    fun updateConditionFromEditedEvent(condition: Condition, index: Int) {
        editedEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions[index] = condition

            _editedEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
        }
    }

    /**
     * Remove a condition from the edited event.
     * @param condition the condition to be removed.
     */
    fun removeConditionFromEditedEvent(condition: Condition) {
        editedEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            if (newConditions.remove(condition)) {
                _editedEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
            }
        }
    }

    /** Creates a new edited action. */
    fun createNewAction(action: Action): EditedAction =
        editedItemManager.createNewEditedAction(action)

    /** Creates a new edited action. */
    fun createNewActionCopy(action: Action, toggleEventItemId: Int): EditedAction =
        editedItemManager.createNewEditedActionCopy(action, toggleEventItemId)

    /**
     * Add a new action to the edited event.
     * @param editedAction the new action.
     */
    fun addActionToEditedEvent(editedAction: EditedAction) {
        editedEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions.add(editedAction.action)
            val newEditedActions = ArrayList(conf.editedActions)
            newEditedActions.add(editedAction)

            _editedEvent.value = conf.copy(
                event = conf.event.copy(actions = newActions),
                editedActions = newEditedActions,
            )
        }
    }

    /**
     * Update an action from the edited event.
     * @param editedAction the updated action.
     * @param index the index in the action list.
     */
    fun updateActionFromEditedEvent(editedAction: EditedAction, index: Int) {
        editedEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions[index] = editedAction.action
            val newEditedActions = ArrayList(conf.editedActions)
            newEditedActions[index] = editedAction

            _editedEvent.value = conf.copy(
                event = conf.event.copy(actions = newActions),
                editedActions = newEditedActions,
            )
        }
    }

    /**
     * Remove an action from the edited event.
     * @param editedAction the action to be removed.
     */
    fun removeActionFromEditedEvent(editedAction: EditedAction) {
        editedEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions.remove(editedAction.action)
            val newEditedActions = ArrayList(conf.editedActions)
            newEditedActions.remove(editedAction)

            _editedEvent.value = conf.copy(
                event = conf.event.copy(actions = newActions),
                editedActions = newEditedActions,
            )
        }
    }

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionOrder(actions: List<EditedAction>) {
        editedEvent.value?.let { conf ->
            _editedEvent.value = conf.copy(
                event = conf.event.copy(actions = actions.map { it.action }.toMutableList()),
                editedActions = actions.toMutableList(),
            )
        }
    }

    private fun updateEditedEventItems(newItems: List<EditedEvent>) {
        val currentConfiguredScenario = editedScenario.value ?: return

        _editedScenario.value = currentConfiguredScenario.copy(
            scenario = currentConfiguredScenario.scenario.copy(eventCount = newItems.size),
            events = newItems,
        )
    }
}
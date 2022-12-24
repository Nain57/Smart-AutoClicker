/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config

import android.content.Context

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
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
     * The state of the currently configured scenario in the database.
     * It will not changes until [saveEditions] is called.
     */
    private val dbScenario: Flow<ConfiguredScenario> = dbScenarioWithEndConditions
        .combine(dbEvents) { scenarioWithEndConditions, events ->
            ConfiguredScenario(
                scenario = scenarioWithEndConditions.first,
                endConditions = scenarioWithEndConditions.second.mapIndexed { index, endCondition ->
                    ConfiguredEndCondition(endCondition, index)
                },
                events = events.mapIndexed { index, event -> ConfiguredEvent(event, index) },
            )
        }

    /**
     * The scenario currently edited by the user.
     * Set as the [dbScenario] value when starting the edition with [startEditions], it will contains all modifications
     * made by the user.
     */
    private val _editedScenario: MutableStateFlow<ConfiguredScenario?> = MutableStateFlow(null)
    /** The scenario currently edited by the user. */
    val editedScenario: StateFlow<ConfiguredScenario?> = _editedScenario
    /** The event list for scenario currently edited by the user. */
    val editedEvents: Flow<List<ConfiguredEvent>> = _editedScenario.map { confScenario ->
        confScenario?.events ?: emptyList()
    }

    /**
     * The event currently configured by the user.
     * Set as the selected event via [startEventEdition], it will contains all modifications made by the user.
     */
    private val _configuredEvent: MutableStateFlow<ConfiguredEvent?> = MutableStateFlow(null)
    val configuredEvent: StateFlow<ConfiguredEvent?> = _configuredEvent

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = _editedScenario.map { it == null }

    /** Set the scenario to be configured. */
    suspend fun setConfiguredScenario(scenarioId: Long) {
        configuredScenarioId.emit(scenarioId)
    }

    /** Start editing the configured scenario. */
    suspend fun startEditions() {
        dbScenario.firstOrNull()?.let { dbScenario ->
            _editedScenario.emit(dbScenario)
        } ?: throw IllegalStateException("Can't start editions, there is no configured scenario set")
    }

    /** Cancel all changes made during the edition. */
    suspend fun cancelEditions() {
        _editedScenario.emit(null)
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        _editedScenario.value?.let { conf ->
            repository.updateScenario(conf.scenario)

            val eventItemIdMap = mutableMapOf<Int, Long>()
            val toBeRemoved = repository.getCompleteEventList(conf.scenario.id).toMutableList()
            conf.events.forEachIndexed { index, configuredEvent ->
                configuredEvent.event.priority = index

                if (configuredEvent.event.id == 0L) {
                    val eventId = repository.addEvent(configuredEvent.event)
                    eventItemIdMap[configuredEvent.itemId] = eventId
                } else {
                    repository.updateEvent(configuredEvent.event)
                    if (toBeRemoved.removeIf { it.id == configuredEvent.event.id }) {
                        eventItemIdMap[configuredEvent.itemId] = configuredEvent.event.id
                    }
                }
            }
            toBeRemoved.forEach { event -> repository.removeEvent(event) }

            repository.updateEndConditions(conf.scenario.id, conf.endConditions.mapNotNull { confEndCondition ->
                val eventId = eventItemIdMap[confEndCondition.eventItemId] ?: return@mapNotNull null
                confEndCondition.endCondition.copy(
                    scenarioId = conf.scenario.id,
                    eventId = eventId,
                )
            })
        } ?: throw IllegalStateException("Can't save editions, there is no configured scenario set")

        _editedScenario.emit(null)
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
    fun updateEventsPriority(events: List<ConfiguredEvent>) {
        updateEditedEventItems(events)
    }

    /** @return a new empty end condition. */
    fun createNewEndCondition(): ConfiguredEndCondition =
        editedScenario.value?.let { confScenario ->
            ConfiguredEndCondition(EndCondition(scenarioId = confScenario.scenario.id))
        } ?: throw IllegalStateException("No scenario defined !")

    /**
     * Add a new end condition to the scenario.
     * @param confEndCondition the end condition to be added.
     */
    fun addEndCondition(confEndCondition: ConfiguredEndCondition) {
        if (confEndCondition.endCondition.id != 0L) return

        editedScenario.value?.let { conf ->
            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply {
                    if (confEndCondition.itemId == INVALID_CONFIGURED_ITEM_ID) {
                        add(confEndCondition.copy(itemId = conf.endConditions.size))
                    }
                }
            )
        }
    }

    /**
     * Update an end condition from the scenario.
     * @param confEndCondition the end condition to be updated.
     */
    fun updateEndCondition(confEndCondition: ConfiguredEndCondition, index: Int) {
        editedScenario.value?.let { conf ->
            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply { set(index, confEndCondition) }
            )
        }
    }

    /**
     * Delete a end condition from the scenario.
     * @param confEndCondition the end condition to be removed.
     */
    fun deleteEndCondition(confEndCondition: ConfiguredEndCondition) {
        editedScenario.value?.let { conf ->
            _editedScenario.value = conf.copy(
                endConditions = conf.endConditions.toMutableList().apply { remove(confEndCondition) }
            )
        }
    }

    /** Set the event currently edited. */
    fun startEventEdition(event: ConfiguredEvent) {
        _configuredEvent.value = event
    }

    /** Update the currently edited event. */
    fun updateEditedEvent(event: Event) {
        _configuredEvent.value = _configuredEvent.value?.copy(event = event)
    }

    /**
     * Add or update the edited event to the edited scenario.
     * If the event id is unset, it will be added. If not, updated.
     */
    fun commitEditedEventToEditedScenario() {
        val item = _configuredEvent.value ?: return
        val items = (editedScenario.value?.events ?: emptyList()).toMutableList()

        if (item.itemId == INVALID_CONFIGURED_ITEM_ID) {
            items.add(item.copy(itemId = items.size))
        } else {
            val itemIndex = items.indexOfFirst { other -> item.itemId == other.itemId }
            if (itemIndex == -1) return

            items[itemIndex] = item
        }

        updateEditedEventItems(items)
        _configuredEvent.value = null
    }

    /** Delete the edited event from the edited scenario. */
    fun deleteEditedEventFromEditedScenario() {
        val item = _configuredEvent.value ?: return
        val items = (editedScenario.value?.events ?: emptyList()).toMutableList()

        val itemIndex = items.indexOfFirst { other -> item.itemId == other.itemId }
        if (itemIndex == -1) return
        items.removeAt(itemIndex)

        updateEditedEventItems(items)
        _configuredEvent.value = null
    }

    /**
     * Add a new condition to the edited event.
     * @param condition the new condition.
     */
    fun addConditionToEditedEvent(condition: Condition) {
        configuredEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions.add(condition)

            _configuredEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
        }
    }

    /**
     * Update a condition in the edited event.
     * @param condition the updated condition.
     */
    fun updateConditionFromEditedEvent(condition: Condition, index: Int) {
        configuredEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions[index] = condition

            _configuredEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
        }
    }

    /**
     * Remove a condition from the edited event.
     * @param condition the condition to be removed.
     */
    fun removeConditionFromEditedEvent(condition: Condition) {
        configuredEvent.value?.let { conf ->
            val newConditions = conf.event.conditions?.let { ArrayList(it) } ?: ArrayList()
            if (newConditions.remove(condition)) {
                _configuredEvent.value = conf.copy(event = conf.event.copy(conditions = newConditions))
            }
        }
    }

    /**
     * Add a new action to the edited event.
     * @param action the new action.
     */
    fun addActionToEditedEvent(action: Action) {
        configuredEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions.add(action)

            _configuredEvent.value = conf.copy(event = conf.event.copy(actions = newActions))
        }
    }

    /**
     * Update an action from the edited event.
     * @param action the updated action.
     */
    fun updateActionFromEditedEvent(action: Action, index: Int) {
        configuredEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions[index] = action

            _configuredEvent.value = conf.copy(event = conf.event.copy(actions = newActions))
        }
    }

    /**
     * Remove an action from the edited event.
     * @param action the action to be removed.
     */
    fun removeActionFromEditedEvent(action: Action) {
        configuredEvent.value?.let { conf ->
            val newActions = conf.event.actions?.let { ArrayList(it) } ?: ArrayList()
            if (newActions.remove(action)) {
                _configuredEvent.value = conf.copy(event = conf.event.copy(actions = newActions))
            }
        }
    }

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionOrder(actions: List<Action>) {
        configuredEvent.value?.let { conf ->
            _configuredEvent.value = conf.copy(
                event = conf.event.copy(actions = actions.toMutableList())
            )
        }
    }

    private fun updateEditedEventItems(newItems: List<ConfiguredEvent>) {
        val currentConfiguredScenario = editedScenario.value ?: return

        _editedScenario.value = currentConfiguredScenario.copy(
            scenario = currentConfiguredScenario.scenario.copy(eventCount = newItems.size),
            events = newItems,
        )
    }
}

/** Represents the scenario currently configured. */
data class ConfiguredScenario(
    val scenario: Scenario,
    val events: List<ConfiguredEvent>,
    val endConditions: List<ConfiguredEndCondition>,
)

/** Represents the events of the scenario currently configured. */
data class ConfiguredEvent(val event: Event, val itemId: Int = INVALID_CONFIGURED_ITEM_ID)

/** Represents the events of the scenario currently configured. */
data class ConfiguredEndCondition(
    val endCondition: EndCondition,
    val eventItemId: Int = INVALID_CONFIGURED_ITEM_ID,
    val itemId: Int = INVALID_CONFIGURED_ITEM_ID,
)

/** Invalid configured item id. The event item object is created but not yet in the list. */
const val INVALID_CONFIGURED_ITEM_ID = -1

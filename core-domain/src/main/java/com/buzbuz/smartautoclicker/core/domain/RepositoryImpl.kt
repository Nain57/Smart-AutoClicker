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
package com.buzbuz.smartautoclicker.core.domain

import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionWithEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.toAction
import com.buzbuz.smartautoclicker.core.domain.model.action.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.condition.toCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.toEndCondition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.event.toEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toScenario
import com.buzbuz.smartautoclicker.core.mapList

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.mapNotNull

// TODO: We should clean unused bitmaps, it can happens that some stays there
/**
 * Repository for the database and bitmap manager.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 *
 * @param database the database containing the list of scenario.
 * @param bitmapManager save and loads the bitmap for the conditions.
 */
internal class RepositoryImpl internal constructor(
    database: ClickDatabase,
    private val bitmapManager: BitmapManager,
): Repository {

    /** The Dao for accessing the database. */
    private val scenarioDao = database.scenarioDao()
    /** The Dao for accessing the database. */
    private val eventDao = database.eventDao()
    /** The Dao for accessing the conditions. */
    private val conditionsDao = database.conditionDao()
    /** The Dao for accessing the actions. */
    private val actionDao = database.actionDao()
    /** The Dao for accessing the scenario end conditions. */
    private val endConditionDao = database.endConditionDao()

    /** Updater for a list of conditions. */
    private val conditionsUpdater = DatabaseListUpdater<Condition, ConditionEntity>(
        itemPrimaryKeySupplier = { condition -> condition.id },
        entityPrimaryKeySupplier = { conditionEntity -> conditionEntity.id },
    )
    /** Updater for a list of actions. */
    private val actionsUpdater = DatabaseListUpdater<Action, CompleteActionEntity>(
        itemPrimaryKeySupplier = { action -> action.id },
        entityPrimaryKeySupplier = { completeActionEntity -> completeActionEntity.action.id },
    )
    /** Updater for a list of end conditions. */
    private val endConditionsUpdater = DatabaseListUpdater<EndCondition, EndConditionEntity>(
        itemPrimaryKeySupplier = { endCondition -> endCondition.id },
        entityPrimaryKeySupplier = { endConditionEntity -> endConditionEntity.id },
    )

    /** True when data are being inserted/edited, false if not. */
    private val isEditingData: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun getScenario(scenarioId: Long): Scenario =
        scenarioDao.getScenario(scenarioId).toScenario()

    override suspend fun getEvents(scenarioId: Long): List<Event> =
        eventDao.getCompleteEvents(scenarioId).map { it.toEvent() }

    override suspend fun getEndConditions(scenarioId: Long): List<EndCondition> =
        endConditionDao.getEndConditionsWithEvent(scenarioId).map { it.toEndCondition() }

    override suspend fun getCompleteEventList(scenarioId: Long): List<Event> =
        eventDao.getCompleteEvents(scenarioId).map { it.toEvent() }

    override val scenarios: Flow<List<Scenario>> =
        scenarioDao.getScenariosWithEvents()
            .filterWhenEditing()
            .mapList { it.toScenario() }

    override fun getScenarioWithEndConditionsFlow(scenarioId: Long): Flow<Pair<Scenario, List<EndCondition>>> =
        scenarioDao.getScenarioWithEndConditions(scenarioId)
            .filterWhenEditing()
            .mapNotNull { scenarioWithEndConditions ->
                scenarioWithEndConditions ?: return@mapNotNull null
                scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions.map { it.toEndCondition() }
            }

    override fun getCompleteEventListFlow(scenarioId: Long): Flow<List<Event>> =
        eventDao.getCompleteEventsFlow(scenarioId)
            .filterWhenEditing()
            .mapList { it.toEvent() }

    override fun getAllEvents(): Flow<List<Event>> =
        eventDao.getAllEvents()
            .filterWhenEditing()
            .mapList { it.toEvent() }

    override fun getAllActions(): Flow<List<Action>> =
        actionDao.getAllActions()
            .filterWhenEditing()
            .mapList { it.toAction() }

    override fun getAllConditions(): Flow<List<Condition>> =
        conditionsDao.getAllConditions()
            .filterWhenEditing()
            .mapList { it.toCondition() }

    override suspend fun addScenario(scenario: Scenario): Long =
        scenarioDao.add(scenario.toEntity())

    override suspend fun deleteScenario(scenario: Scenario) {
        val removedConditionsPath = mutableListOf<String>()
        eventDao.getEventsIds(scenario.id.databaseId).forEach { eventId ->
            conditionsDao.getConditionsPath(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        scenarioDao.delete(scenario.toEntity())
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    override suspend fun addScenarioCopy(completeScenario: CompleteScenario) {
        val scenario = completeScenario.scenario.toScenario(asDomain = true)
        val scenarioDbId = scenarioDao.add(scenario.toEntity())

        /**
         * Get the entities as domain object to use the same insertion.
         * Update the scenario id with the database one.
         */
        val events = completeScenario.events.map { completeEventEntity ->
            completeEventEntity
                .toEvent(asDomain = true)
                .copy(scenarioId = Identifier(databaseId = scenarioDbId))
        }

        /** Same with the end conditions. */
        val endConditions = completeScenario.endConditions.mapNotNull { endConditionEntity ->
            val associatedEvent = completeScenario.events.find { it.event.id == endConditionEntity.eventId }
                ?: return@mapNotNull null

            EndConditionWithEvent(endConditionEntity, associatedEvent.event)
                .toEndCondition(asDomain = true)
                .copy(scenarioId = Identifier(databaseId = scenarioDbId))
        }

        updateScenario(scenario.copy(id = Identifier(databaseId = scenarioDbId)), events, endConditions)
    }

    override suspend fun updateScenario(scenario: Scenario, events: List<Event>, endConditions: List<EndCondition>) {
        isEditingData.value = true

        // Update scenario entity values
        scenarioDao.update(scenario.toEntity())

        // Keep track of new database id for events that weren't in the database yet.
        val evtDomainToDbIdMap = mutableMapOf<Long, Long>()
        // Keep track of the events to be removed.
        // First take all events from the database, and then remove the events found in the new event list
        val eventsToBeRemoved = eventDao.getEvents(scenario.id.databaseId).toMutableList()

        // Add/Update all events entities. Removals will be done at the end.
        events.forEachIndexed { index, eventInScenario ->
            eventInScenario.priority = index

            val entity = eventInScenario.toEntity()
            if (eventInScenario.id.isInDatabase()) {
                eventDao.updateEvent(entity)
                eventsToBeRemoved.removeIf { it.id == eventInScenario.id.databaseId }
            } else {
                eventInScenario.id.domainId?.let {
                    evtDomainToDbIdMap[it] = eventDao.addEvent(entity)
                }
            }
        }

        // Now that all events have a database id, process actions and conditions
        events.forEach { eventInScenario ->
            val eventDbId = evtDomainToDbIdMap.getDatabaseId(eventInScenario.id)
            updateConditions(eventDbId, eventInScenario.conditions)
            updateActions(eventDbId, eventInScenario.actions, evtDomainToDbIdMap)
        }

        // Update the scenario's end conditions
        updateEndConditions(scenario.id.databaseId, endConditions, evtDomainToDbIdMap)

        // Remove events that are not in the new scenario.
        // Cascade deletion will remove all linked actions and conditions.
        if (eventsToBeRemoved.isNotEmpty()) eventDao.deleteEvents(eventsToBeRemoved)

        isEditingData.value = false
    }

    private suspend fun updateConditions(eventDbId: Long, conditions: List<Condition>) {
        saveNewConditionsBitmap(conditions)

        conditionsUpdater.refreshUpdateValues(
            currentEntities = conditionsDao.getConditions(eventDbId),
            newItems = conditions,
            toEntity = { _, condition ->
                condition.toEntity().apply {
                    eventId = eventDbId
                }
            }
        )

        conditionsDao.syncConditions(
            conditionsUpdater.toBeAdded,
            conditionsUpdater.toBeUpdated,
            conditionsUpdater.toBeRemoved,
        )

        if (conditionsUpdater.toBeRemoved.isNotEmpty()) {
            clearRemovedConditionsBitmaps(conditionsUpdater.toBeRemoved.map { it.path })
        }
    }

    private suspend fun updateActions(eventDbId: Long, actions: List<Action>, evtItemIdToDbIdMap: Map<Long, Long>) {
        actionsUpdater.refreshUpdateValues(
            currentEntities = actionDao.getCompleteActions(eventDbId),
            newItems = actions,
            toEntity = { index, actionInEvent ->
                actionInEvent.toEntity().apply {
                    action.eventId = eventDbId
                    action.toggleEventId = evtItemIdToDbIdMap.getToggleEventDatabaseId(actionInEvent)
                    action.priority = index
                }
            }
        )

        actionDao.syncActions(
            actionsUpdater.toBeAdded,
            actionsUpdater.toBeUpdated,
            actionsUpdater.toBeRemoved,
        )
    }

    private suspend fun updateEndConditions(
        scenarioId: Long,
        endConditions: List<EndCondition>,
        evtItemIdToDbIdMap: Map<Long, Long>,
    ) {
        endConditionsUpdater.refreshUpdateValues(
            currentEntities = endConditionDao.getEndConditions(scenarioId),
            newItems = endConditions,
            toEntity = { _, endCondition ->
                endCondition.toEntity().apply {
                    eventId = evtItemIdToDbIdMap.getDatabaseId(endCondition.eventId)
                }
            }
        )

        endConditionDao.syncEndConditions(
            endConditionsUpdater.toBeAdded,
            endConditionsUpdater.toBeUpdated,
            endConditionsUpdater.toBeRemoved,
        )
    }

    private fun Map<Long, Long>.getDatabaseId(identifier: Identifier?): Long = when {
        identifier != null && identifier.domainId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> get(identifier.domainId) ?: throw IllegalStateException("Identifier is not found in map")
        else -> throw IllegalStateException("Database id can't be found")
    }

    private fun Map<Long, Long>.getToggleEventDatabaseId(action: Action): Long? =
        if (action is Action.ToggleEvent) {
            val toggleEventId = action.toggleEventId
                ?: throw IllegalArgumentException("Invalid toggle event insertion")
            getDatabaseId(toggleEventId)
        } else {
            null
        }

    override suspend fun getBitmap(path: String, width: Int, height: Int) =
        bitmapManager.loadBitmap(path, width, height)

    override fun cleanCache(): Unit =
        bitmapManager.releaseCache()

    /**
     * Save the new conditions bitmap on the application data folder.
     *
     * @param conditions the list of conditions to save their bitmap.
     */
    private suspend fun saveNewConditionsBitmap(conditions: List<Condition>) {
        conditions.forEach { condition ->
            if (condition.path == null) {
                if (condition.bitmap == null) {
                    throw IllegalArgumentException("Can't save invalid condition")
                }

                condition.path = bitmapManager.saveBitmap(condition.bitmap)
            }
        }
    }

    /**
     * Remove bitmaps from the application data folder.
     *
     * @param removedPath the list of path for the bitmaps to be removed.
     */
    private suspend fun clearRemovedConditionsBitmaps(removedPath: List<String>) {
        val deletedPaths = removedPath.filter { path ->
            conditionsDao.getValidPathCount(path) == 0
        }

        bitmapManager.deleteBitmaps(deletedPaths)
    }

    /**
     * Propagate the changes of a flow only when [isEditingData] is false.
     * This allows to prevent the clients to receives updates when a scenario is being updated.
     */
    private fun <T> Flow<T>.filterWhenEditing() = combineTransform(isEditingData) { value, isEditing ->
        if (!isEditing) emit(value)
    }
}
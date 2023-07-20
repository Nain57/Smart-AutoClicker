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

import android.util.Log
import androidx.room.withTransaction

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.bitmaps.TUTORIAL_CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.ScenarioDatabase
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ActionDao
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EndConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao
import com.buzbuz.smartautoclicker.core.database.dao.TutorialDao
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionWithEvent
import com.buzbuz.smartautoclicker.core.database.entity.TutorialSuccessEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.TutorialSuccessState
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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.lang.Exception

// TODO: We should clean unused bitmaps, it can happens that some stays there
/**
 * Repository for the database and bitmap manager.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 *
 * @param database the database containing the list of scenario.
 * @param bitmapManager save and loads the bitmap for the conditions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RepositoryImpl internal constructor(
    private val database: ClickDatabase,
    private val tutorialDatabase: TutorialDatabase,
    private val bitmapManager: BitmapManager,
): Repository {

    private var currentDatabase: MutableStateFlow<ScenarioDatabase> = MutableStateFlow(database)

    /** The Dao for accessing the database. */
    private fun scenarioDao(): ScenarioDao = currentDatabase.value.scenarioDao()
    /** The Dao for accessing the database. */
    private fun eventDao(): EventDao = database.eventDao()
    /** The Dao for accessing the conditions. */
    private fun conditionsDao(): ConditionDao = database.conditionDao()
    /** The Dao for accessing the actions. */
    private fun actionDao(): ActionDao = database.actionDao()
    /** The Dao for accessing the scenario end conditions. */
    private fun endConditionDao(): EndConditionDao = database.endConditionDao()

    private fun tutorialDao(): TutorialDao? {
        val db = currentDatabase
        return if (db is TutorialDatabase) db.tutorialDao() else null
    }

    private fun getBitmapFilePrefix(): String =
        if (currentDatabase.value == database) CONDITION_FILE_PREFIX
        else TUTORIAL_CONDITION_FILE_PREFIX

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

    override val scenarios: Flow<List<Scenario>> =
        currentDatabase
            .flatMapLatest { it.scenarioDao().getScenariosWithEvents() }
            .mapList { it.toScenario() }

    override val tutorialSuccessList: Flow<List<TutorialSuccessState>> =
        currentDatabase
            .flatMapLatest {
                if (it is TutorialDatabase) tutorialDatabase.tutorialDao().getTutorialSuccessList()
                else flowOf(emptyList())
            }
            .map { list -> list.map { TutorialSuccessState(it.scenarioId) } }

    override suspend fun getScenario(scenarioId: Long): Scenario? =
        scenarioDao().getScenario(scenarioId)?.toScenario()

    override suspend fun getEvents(scenarioId: Long): List<Event> =
        eventDao().getCompleteEvents(scenarioId).map { it.toEvent() }

    override suspend fun getEndConditions(scenarioId: Long): List<EndCondition> =
        endConditionDao().getEndConditionsWithEvent(scenarioId).map { it.toEndCondition() }

    override suspend fun getCompleteEventList(scenarioId: Long): List<Event> =
        eventDao().getCompleteEvents(scenarioId).map { it.toEvent() }

    override fun getScenarioWithEndConditionsFlow(scenarioId: Long): Flow<Pair<Scenario, List<EndCondition>>> =
        scenarioDao().getScenarioWithEndConditions(scenarioId)
            .mapNotNull { scenarioWithEndConditions ->
                scenarioWithEndConditions ?: return@mapNotNull null
                scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions.map { it.toEndCondition() }
            }

    override fun getCompleteEventListFlow(scenarioId: Long): Flow<List<Event>> =
        eventDao().getCompleteEventsFlow(scenarioId)
            .mapList { it.toEvent() }

    override fun getAllEvents(): Flow<List<Event>> =
        eventDao().getAllEvents()
            .mapList { it.toEvent() }

    override fun getAllActions(): Flow<List<Action>> =
        actionDao().getAllActions()
            .mapList { it.toAction() }

    override fun getAllConditions(): Flow<List<Condition>> =
        conditionsDao().getAllConditions()
            .mapList { it.toCondition() }

    override suspend fun addScenario(scenario: Scenario): Long =
        scenarioDao().add(scenario.toEntity())

    override suspend fun deleteScenario(scenarioId: Identifier) {
        val removedConditionsPath = mutableListOf<String>()
        eventDao().getEventsIds(scenarioId.databaseId).forEach { eventId ->
            conditionsDao().getConditionsPath(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        scenarioDao().delete(scenarioId.databaseId)
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    override suspend fun addScenarioCopy(completeScenario: CompleteScenario): Boolean {
        return try {
            database.withTransaction {
                val scenario = completeScenario.scenario.toScenario(asDomain = true)
                val scenarioDbId = scenarioDao().add(scenario.toEntity())

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

                updateScenarioContent(scenarioDbId, events, endConditions)
            }

            true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            false
        }
    }

    override suspend fun updateScenario(scenario: Scenario, events: List<Event>, endConditions: List<EndCondition>): Boolean {
        return try {
            database.withTransaction {
                // Update scenario entity values
                scenarioDao().update(scenario.toEntity())
                // Update scenario content
                updateScenarioContent(scenario.id.databaseId, events, endConditions)
            }

            true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while updating scenario", ex)
            false
        }
    }

    private suspend fun updateScenarioContent(scenarioDbId: Long, events: List<Event>, endConditions: List<EndCondition>) {
        // Check arguments
        if (events.find { !it.isComplete() } != null)
            throw IllegalArgumentException("Can't update scenario content, one of the event is not complete")
        if (endConditions.find { !it.isComplete() } != null)
            throw IllegalArgumentException("Can't update scenario content, one of the end condition is not complete")

        // Keep track of new database id for events that weren't in the database yet.
        val evtDomainToDbIdMap = mutableMapOf<Long, Long>()
        // Keep track of the events to be removed.
        // First take all events from the database, and then remove the events found in the new event list
        val eventsToBeRemoved = eventDao().getEvents(scenarioDbId).toMutableList()

        // Add/Update all events entities. Removals will be done at the end.
        events.forEachIndexed { index, eventInScenario ->
            eventInScenario.priority = index

            val entity = eventInScenario.toEntity()
            if (eventInScenario.id.isInDatabase()) {
                eventDao().updateEvent(entity)
                eventsToBeRemoved.removeIf { it.id == eventInScenario.id.databaseId }
            } else {
                eventInScenario.id.domainId?.let {
                    evtDomainToDbIdMap[it] = eventDao().addEvent(entity)
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
        updateEndConditions(scenarioDbId, endConditions, evtDomainToDbIdMap)

        // Remove events that are not in the new scenario.
        // Cascade deletion will remove all linked actions and conditions.
        if (eventsToBeRemoved.isNotEmpty()) eventDao().deleteEvents(eventsToBeRemoved)
    }

    private suspend fun updateConditions(eventDbId: Long, conditions: List<Condition>) {
        val prefix = getBitmapFilePrefix()

        conditionsUpdater.refreshUpdateValues(
            currentEntities = conditionsDao().getConditions(eventDbId),
            newItems = conditions,
            toEntity = { _, condition ->
                val path = if (condition.path.isNullOrEmpty()) {
                    condition.bitmap?.let { bitmapManager.saveBitmap(it, prefix) }
                        ?: throw IllegalArgumentException("Can't insert condition, bitmap and path are both null.")
                } else {
                    condition.path
                }

                condition.copy(
                    eventId = Identifier(databaseId = eventDbId),
                    path = path,
                ).toEntity()
            }
        )

        conditionsDao().syncConditions(
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
            currentEntities = actionDao().getCompleteActions(eventDbId),
            newItems = actions,
            toEntity = { index, actionInEvent ->
                actionInEvent.toEntity().apply {
                    action.eventId = eventDbId
                    action.toggleEventId = evtItemIdToDbIdMap.getToggleEventDatabaseId(actionInEvent)
                    action.priority = index
                }
            }
        )

        actionDao().syncActions(
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
            currentEntities = endConditionDao().getEndConditions(scenarioId),
            newItems = endConditions,
            toEntity = { _, endCondition ->
                endCondition.copy(
                    eventId = Identifier(databaseId = evtItemIdToDbIdMap.getDatabaseId(endCondition.eventId)),
                ).toEntity()
            }
        )

        endConditionDao().syncEndConditions(
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
     * Remove bitmaps from the application data folder.
     *
     * @param removedPath the list of path for the bitmaps to be removed.
     */
    private suspend fun clearRemovedConditionsBitmaps(removedPath: List<String>) {
        val deletedPaths = removedPath.filter { path ->
            conditionsDao().getValidPathCount(path) == 0
        }

        bitmapManager.deleteBitmaps(deletedPaths)
    }

    override fun startTutorialMode() {
        currentDatabase.value = tutorialDatabase
    }

    override fun stopTutorialMode() {
        currentDatabase.value = database
    }

    override suspend fun getTutorialScenarioDatabaseId(index: Int): Identifier? =
        tutorialDao()?.getTutorialScenarioId(index)?.let {
            Identifier(databaseId = it)
        }

    override suspend fun setTutorialSuccess(index: Int, scenarioId: Identifier) {
        tutorialDao()?.upsert(
            TutorialSuccessEntity(
                tutorialIndex = index,
                scenarioId = scenarioId.databaseId,
            )
        )
    }
}

/** Tag for logs. */
private const val TAG = "RepositoryImpl"
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
package com.buzbuz.smartautoclicker.core.domain.data

import android.util.Log
import androidx.room.withTransaction
import com.buzbuz.smartautoclicker.core.base.DatabaseListUpdater

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.bitmaps.CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.bitmaps.TUTORIAL_CONDITION_FILE_PREFIX
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.ScenarioDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ActionDao
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionWithEvent
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEndConditions
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

import java.lang.Exception

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScenarioDataSource(
    defaultDatabase: ScenarioDatabase,
    private val bitmapManager: BitmapManager,
) {

    /** The database currently in use. */
    var currentDatabase: MutableStateFlow<ScenarioDatabase> = MutableStateFlow(defaultDatabase)

    /** The Dao for accessing the scenario. */
    private val scenarioDaoFlow: Flow<ScenarioDao> = currentDatabase.map { it.scenarioDao() }
    /** The Dao for accessing the events. */
    private val eventDaoFlow: Flow<EventDao> = currentDatabase.map { it.eventDao() }
    /** The Dao for accessing the conditions. */
    private val conditionsDaoFlow: Flow<ConditionDao> = currentDatabase.map { it.conditionDao() }
    /** The Dao for accessing the actions. */
    private val actionDaoFlow: Flow<ActionDao> = currentDatabase.map { it.actionDao() }

    /** State of scenario during an update, to keep track of ids mapping. */
    private val scenarioUpdateState = ScenarioUpdateState()
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

    val scenarios: Flow<List<ScenarioWithEvents>> =
        scenarioDaoFlow.flatMapLatest { it.getScenariosWithEvents() }

    suspend fun getScenario(scenarioId: Long): ScenarioEntity? =
        currentDatabase.value.scenarioDao().getScenario(scenarioId)

    suspend fun getEvents(scenarioId: Long): List<CompleteEventEntity> =
        currentDatabase.value.eventDao().getCompleteEvents(scenarioId)

    suspend fun getEndConditionsWithEvent(scenarioId: Long): List<EndConditionWithEvent> =
        currentDatabase.value.endConditionDao().getEndConditionsWithEvent(scenarioId)

    fun getScenarioWithEndConditionsFlow(scenarioId: Long): Flow<ScenarioWithEndConditions?> =
        scenarioDaoFlow.flatMapLatest { it.getScenarioWithEndConditions(scenarioId) }

    fun getCompleteEventListFlow(scenarioId: Long): Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getCompleteEventsFlow(scenarioId) }

    fun getAllEvents(): Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getAllEvents() }

    fun getAllActions(): Flow<List<CompleteActionEntity>> =
        actionDaoFlow.flatMapLatest { it.getAllActions() }

    fun getAllConditions(): Flow<List<ConditionEntity>> =
        conditionsDaoFlow.flatMapLatest { it.getAllConditions() }

    suspend fun addScenario(scenario: Scenario): Long {
        Log.d(TAG, "Add scenario to the database: ${scenario.id}")
        return currentDatabase.value.scenarioDao().add(scenario.toEntity())
    }

    suspend fun deleteScenario(scenarioId: Identifier) {
        Log.d(TAG, "Delete scenario from the database: $scenarioId")

        val removedConditionsPath = mutableListOf<String>()
        currentDatabase.value.eventDao().getEventsIds(scenarioId.databaseId).forEach { eventId ->
            currentDatabase.value.conditionDao().getConditionsPath(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        currentDatabase.value.scenarioDao().delete(scenarioId.databaseId)
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    suspend fun addScenarioCopy(completeScenario: CompleteScenario): Long? {
        Log.d(TAG, "Add scenario copy to the database: ${completeScenario.scenario.id}")

        return try {
            currentDatabase.value.withTransaction {
                val scenario = completeScenario.scenario.toScenario(asDomain = true)
                val scenarioDbId = currentDatabase.value.scenarioDao().add(scenario.toEntity())

                /*
                 * Get the entities as domain object to use the same insertion.
                 * Update the scenario id with the database one.
                 */
                val events = completeScenario.events.map { completeEventEntity ->
                    completeEventEntity
                        .toEvent(asDomain = true)
                        .copy(scenarioId = Identifier(databaseId = scenarioDbId))
                }.sortedBy { it.priority }

                /* Same with the end conditions. */
                val endConditions = completeScenario.endConditions.mapNotNull { endConditionEntity ->
                    val associatedEvent = completeScenario.events.find { it.event.id == endConditionEntity.eventId }
                        ?: return@mapNotNull null

                    EndConditionWithEvent(endConditionEntity, associatedEvent.event)
                        .toEndCondition(asDomain = true)
                        .copy(scenarioId = Identifier(databaseId = scenarioDbId))
                }

                updateScenarioContent(scenarioDbId, events, endConditions)

                scenarioDbId
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun updateScenario(scenario: Scenario, events: List<Event>, endConditions: List<EndCondition>): Boolean {
        Log.d(TAG, "Update scenario in the database: ${scenario.id}")

        return try {
            currentDatabase.value.withTransaction {
                // Update scenario entity values
                currentDatabase.value.scenarioDao().update(scenario.toEntity())
                // Update scenario content
                updateScenarioContent(scenario.id.databaseId, events, endConditions)
            }

            true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while updating scenario\n* Scenario=$scenario\n* Events=$events\n* endCondition=$endConditions", ex)
            false
        }
    }

    private suspend fun updateScenarioContent(scenarioDbId: Long, events: List<Event>, endConditions: List<EndCondition>) {
        Log.d(TAG, "Update scenario $scenarioDbId content in the database")

        // Check arguments
        if (events.find { !it.isComplete() } != null)
            throw IllegalArgumentException("Can't update scenario content, one of the event is not complete")
        if (endConditions.find { !it.isComplete() } != null)
            throw IllegalArgumentException("Can't update scenario content, one of the end condition is not complete")

        // Init update state.
        scenarioUpdateState.initUpdateState(
            oldScenarioEvents = currentDatabase.value.eventDao().getEvents(scenarioDbId)
        )

        // Add/Update all events entities. Removals will be done at the end.
        updateEvents(events)

        // Now that all events have a database id, process actions and conditions
        events.forEach { eventInScenario ->
            val eventDbId = scenarioUpdateState.getEventDbId(eventInScenario.id)

            updateConditions(eventDbId, eventInScenario.conditions)
            updateActions(eventDbId, eventInScenario.actions)
        }

        // Update the scenario's end conditions
        updateEndConditions(scenarioDbId, endConditions)

        // Remove events that are not in the new scenario.
        // Cascade deletion will remove all linked actions and conditions.
        val evtToBeRemoved = scenarioUpdateState.getEventToBeRemoved()
        if (evtToBeRemoved.isNotEmpty()) {
            currentDatabase.value.eventDao().deleteEvents(evtToBeRemoved)
            clearRemovedEventsBitmaps(evtToBeRemoved)
        }

        conditionsUpdater.clear()
        actionsUpdater.clear()
        endConditionsUpdater.clear()
    }

    /** Add/Update all events entities. Removals will be done at the end. */
    private suspend fun updateEvents(events: List<Event>) {
        events.forEachIndexed { index, eventInScenario ->
            eventInScenario.priority = index

            val entity = eventInScenario.toEntity()
            if (eventInScenario.id.isInDatabase()) {
                currentDatabase.value.eventDao().updateEvent(entity)
                scenarioUpdateState.setEventAsKept(eventInScenario.id.databaseId)
            } else {
                eventInScenario.id.domainId?.let {
                    scenarioUpdateState.addEventIdMapping(
                        domainId = it,
                        dbId = currentDatabase.value.eventDao().addEvent(entity),
                    )
                }
            }
        }
    }

    private suspend fun updateConditions(eventDbId: Long, conditions: List<Condition>) {
        Log.d(TAG, "Updating conditions in the database for event $eventDbId")
        conditionsUpdater.refreshUpdateValues(
            currentEntities = currentDatabase.value.conditionDao().getConditions(eventDbId),
            newItems = conditions,
            toEntity = { _, condition ->
                condition.copy(
                    eventId = Identifier(databaseId = eventDbId),
                    path = saveBitmapIfNeeded(condition),
                ).toEntity()
            }
        )
        Log.d(TAG, "Conditions updater: $conditionsUpdater")

        currentDatabase.value.conditionDao().apply {
            addConditions(conditionsUpdater.toBeAdded).forEachIndexed { index, conditionDbId ->
                conditionsUpdater.getItemFromEntity(conditionsUpdater.toBeAdded[index])?.id?.domainId?.let { conditionDomainId ->
                    scenarioUpdateState.addConditionIdMapping(
                        domainId = conditionDomainId,
                        dbId = conditionDbId,
                    )
                }
            }
            updateConditions(conditionsUpdater.toBeUpdated)
            deleteConditions(conditionsUpdater.toBeRemoved)
        }

        if (conditionsUpdater.toBeRemoved.isNotEmpty()) {
            clearRemovedConditionsBitmaps(conditionsUpdater.toBeRemoved.map { it.path })
        }
    }

    private suspend fun updateActions(eventDbId: Long, actions: List<Action>) {
        Log.d(TAG, "Updating actions in the database for event $eventDbId")
        actionsUpdater.refreshUpdateValues(
            currentEntities = currentDatabase.value.actionDao().getCompleteActions(eventDbId),
            newItems = actions,
            toEntity = { index, actionInEvent ->
                actionInEvent.toEntity().apply {
                    action.eventId = eventDbId
                    action.toggleEventId = scenarioUpdateState.getToggleEventDatabaseId(actionInEvent)
                    action.clickOnConditionId = scenarioUpdateState.getClickOnConditionDatabaseId(actionInEvent)
                    action.priority = index
                }
            }
        )
        Log.d(TAG, "Actions updater: $actionsUpdater")

        addCompleteActions(actionsUpdater.toBeAdded)
        updateCompleteActions(actionsUpdater.toBeUpdated)
        currentDatabase.value.actionDao()
            .deleteActions(actionsUpdater.toBeRemoved.map { it.action })
    }

    private suspend fun addCompleteActions(completeActions: List<CompleteActionEntity>) {
        completeActions.forEach { completeAction ->

            currentDatabase.value.actionDao().apply {
                val actionId = addAction(completeAction.action)

                completeAction.intentExtras.forEach { intentExtra ->
                    intentExtra.actionId = actionId
                    addIntentExtra(intentExtra)
                }
            }
        }
    }

    private suspend fun updateCompleteActions(completeActions: List<CompleteActionEntity>) {
        completeActions.forEach { completeAction ->
            currentDatabase.value.actionDao().apply {
                updateAction(completeAction.action)

                val extrasToBeRemoved = getIntentExtras(completeAction.action.id).toMutableList()
                completeAction.intentExtras.forEach { intentExtra ->
                    intentExtra.actionId = completeAction.action.id

                    if (intentExtra.id == 0L) {
                        addIntentExtra(intentExtra)
                    } else {
                        updateIntentExtra(intentExtra)
                        extrasToBeRemoved.removeIf { it.id == intentExtra.id }
                    }
                }
                if (extrasToBeRemoved.isNotEmpty()) deleteIntentExtras(extrasToBeRemoved)
            }
        }
    }

    private suspend fun updateEndConditions(scenarioId: Long, endConditions: List<EndCondition>) {
        endConditionsUpdater.refreshUpdateValues(
            currentEntities = currentDatabase.value.endConditionDao().getEndConditions(scenarioId),
            newItems = endConditions,
            toEntity = { _, endCondition ->
                endCondition.copy(
                    eventId = Identifier(databaseId = scenarioUpdateState.getEventDbId(endCondition.eventId)),
                ).toEntity()
            }
        )

        currentDatabase.value.endConditionDao().apply {
            addEndConditions(endConditionsUpdater.toBeAdded)
            updateEndConditions(endConditionsUpdater.toBeUpdated)
            deleteEndConditions(endConditionsUpdater.toBeRemoved)
        }
    }

    /**
     * Remove bitmaps from the application data folder.
     * @param removedEvents the list of events for the bitmaps to be removed.
     */
    private suspend fun clearRemovedEventsBitmaps(removedEvents: List<EventEntity>) {
        val deletedPaths = buildSet {
            removedEvents.forEach { event ->
                currentDatabase.value.conditionDao().getConditionsPath(event.id).forEach { path ->
                    add(path)
                }
            }
        }

        clearRemovedConditionsBitmaps(deletedPaths.toList())
    }

    /**
     * Remove bitmaps from the application data folder.
     * @param removedPath the list of path for the bitmaps to be removed.
     */
    private suspend fun clearRemovedConditionsBitmaps(removedPath: List<String>) {
        val deletedPaths = removedPath.filter { path ->
            currentDatabase.value.conditionDao().getValidPathCount(path) == 0
        }

        Log.d(TAG, "Removed conditions count: ${removedPath.size}; Unused bitmaps after removal: ${deletedPaths.size}")
        bitmapManager.deleteBitmaps(deletedPaths)
    }

    private suspend fun saveBitmapIfNeeded(condition: Condition): String =
        if (condition.path.isNullOrEmpty()) {
            condition.bitmap?.let { bitmapManager.saveBitmap(it, getBitmapFilePrefix()) }
                ?: throw IllegalArgumentException("Can't insert condition, bitmap and path are both null.")
        } else {
            condition.path
        }

    private fun getBitmapFilePrefix(): String =
        if (currentDatabase.value is ClickDatabase) CONDITION_FILE_PREFIX
        else TUTORIAL_CONDITION_FILE_PREFIX
}

/** Tag for logs. */
private const val TAG = "ScenarioDataSource"
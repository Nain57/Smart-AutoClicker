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
package com.buzbuz.smartautoclicker.core.domain.data

import android.util.Log
import androidx.room.withTransaction

import com.buzbuz.smartautoclicker.core.base.DatabaseListUpdater
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.areComplete
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.database.ScenarioDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ActionDao
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioStatsEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.condition.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

import java.lang.Exception

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScenarioDataSource(
    defaultDatabase: ScenarioDatabase,
    private val bitmapManager: BitmapRepository,
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

    val scenarios: Flow<List<ScenarioWithEvents>> =
        scenarioDaoFlow.flatMapLatest { it.getScenariosWithEvents() }

    val allTriggerEvents: Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getAllTriggerEventsFlow() }

    val allImageEvents: Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getAllImageEventsFlow() }

    suspend fun getScenario(scenarioId: Long): ScenarioWithEvents? =
        currentDatabase.value.scenarioDao().getScenario(scenarioId)

    suspend fun getCompleteScenario(scenarioId: Long): CompleteScenario? =
        currentDatabase.value.scenarioDao().getCompleteScenario(scenarioId)

    fun getScenarioFlow(scenarioId: Long): Flow<ScenarioWithEvents?> =
        scenarioDaoFlow.flatMapLatest { it.getScenarioFlow(scenarioId) }

    suspend fun getImageEvents(scenarioId: Long): List<CompleteEventEntity> =
        currentDatabase.value.eventDao().getCompleteImageEvents(scenarioId)

    fun getImageEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getCompleteImageEventsFlow(scenarioId) }

    suspend fun getTriggerEvents(scenarioId: Long): List<CompleteEventEntity> =
        currentDatabase.value.eventDao().getCompleteTriggerEvents(scenarioId)

    fun getTriggerEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>> =
        eventDaoFlow.flatMapLatest { it.getCompleteTriggerEventsFlow(scenarioId) }

    fun getAllConditions(): Flow<List<ConditionEntity>> =
        conditionsDaoFlow.flatMapLatest { it.getAllConditions() }

    fun getAllActions(): Flow<List<CompleteActionEntity>> =
        actionDaoFlow.flatMapLatest { it.getAllActions() }

    suspend fun addScenario(scenario: Scenario): Long {
        Log.d(TAG, "Add scenario to the database: ${scenario.id}")
        return currentDatabase.value.scenarioDao().add(scenario.toEntity())
    }

    suspend fun deleteScenario(scenarioId: Identifier) {
        Log.d(TAG, "Delete scenario from the database: $scenarioId")

        val removedConditionsPath = mutableListOf<String>()
        currentDatabase.value.eventDao().getEventsIds(scenarioId.databaseId).forEach { eventId ->
            currentDatabase.value.conditionDao().getConditionsPaths(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        currentDatabase.value.scenarioDao().delete(scenarioId.databaseId)
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    suspend fun addCompleteScenario(scenario: Scenario, events: List<Event>): Long? {
        Log.d(TAG, "Add scenario copy to the database: ${scenario.id}")

        // Check the events correctness
        if (!events.areComplete())
            throw IllegalArgumentException("Can't update scenario content, one of the event is not complete")

        return try {
            currentDatabase.value.withTransaction {
                // First insert the scenario to get its database id, and put it in all events
                val scenarioId = Identifier(
                    databaseId = currentDatabase.value.scenarioDao().add(scenario.toEntity())
                )

                updateEvents(
                    scenarioDbId = scenarioId.databaseId,
                    events = events,
                )

                scenarioId.databaseId
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun updateScenario(scenario: Scenario, events: List<Event>): Boolean {
        Log.d(TAG, "Update scenario in the database: ${scenario.id}")

        return try {
            currentDatabase.value.withTransaction {
                // Update scenario entity values
                currentDatabase.value.scenarioDao().update(scenario.toEntity())
                // Update scenario content
                updateEvents(
                    scenarioDbId = scenario.id.databaseId,
                    events = events,
                )
            }

            true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while updating scenario\n* Scenario=$scenario\n* Events=$events\n", ex)
            false
        }
    }

    suspend fun markAsUsed(scenarioDbId: Long) {
        currentDatabase.value.scenarioDao().let { scenarioDao ->
            val previousStats = scenarioDao.getScenarioStats(scenarioDbId)
            if (previousStats != null) {
                scenarioDao.updateScenarioStats(
                    previousStats.copy(
                        lastStartTimestampMs = System.currentTimeMillis(),
                        startCount = previousStats.startCount + 1,
                    )
                )
            } else {
                scenarioDao.addScenarioStats(
                    ScenarioStatsEntity(
                        id = DATABASE_ID_INSERTION,
                        scenarioId = scenarioDbId,
                        lastStartTimestampMs = System.currentTimeMillis(),
                        startCount = 1,
                    )
                )
            }
        }
    }

    private suspend fun updateEvents(scenarioDbId: Long, events: List<Event>) {
        scenarioUpdateState.initUpdateState()
        val updater = DatabaseListUpdater<Event, EventEntity>()

        Log.d(TAG, "Updating events in the database for scenario $scenarioDbId")
        updater.refreshUpdateValues(
            currentEntities = currentDatabase.value.eventDao().getEvents(scenarioDbId),
            newItems = events,
            mappingClosure = { event ->
                event.toEntity().apply {
                    scenarioId = scenarioDbId
                }
            }
        )
        Log.d(TAG, "Events updater: $updater")

        currentDatabase.value.eventDao().let { eventDao ->
            updater.executeUpdate(
                addList = eventDao::addEvents,
                updateList = eventDao::updateEvent,
                removeList = eventDao::deleteEvents,
                onSuccess = { addedMapping, added, updated, removed ->
                    addedMapping.forEach { (domainId, dbId) ->
                        scenarioUpdateState.addEventIdMapping(domainId, dbId)
                    }

                    updateEventsChildren(buildList {
                        addAll(added)
                        addAll(updated)
                    })

                    if (removed.isNotEmpty()) clearRemovedEventsBitmaps(removed)
                }
            )
        }
    }

    private suspend fun updateEventsChildren(events: List<Event>) {
        // Actions can reference a condition, do them all first
        events.forEach { event ->
            updateConditions(
                eventDbId = scenarioUpdateState.getEventDbId(event.id),
                newConditions = event.conditions,
            )
        }

        // Second iteration for actions
        events.forEach { event ->
            updateActions(
                eventDbId = scenarioUpdateState.getEventDbId(event.id),
                newActions = event.actions,
            )
        }
    }

    private suspend fun updateConditions(eventDbId: Long, newConditions: List<Condition>) {
        val updater = DatabaseListUpdater<Condition, ConditionEntity>()

        Log.d(TAG, "Updating conditions in the database for event $eventDbId")
        updater.refreshUpdateValues(
            currentEntities = currentDatabase.value.conditionDao().getConditions(eventDbId),
            newItems = newConditions,
            mappingClosure = { condition -> condition.copy(evtId = Identifier(databaseId = eventDbId)).toEntity() }
        )
        Log.d(TAG, "Conditions updater: $updater")

        currentDatabase.value.conditionDao().let { conditionDao ->
            updater.executeUpdate(
                addList = conditionDao::addConditions,
                updateList = conditionDao::updateConditions,
                removeList = conditionDao::deleteConditions,
                onSuccess = { addedMapping, _, _, removed ->
                    addedMapping.forEach { (domainId, dbId) ->
                        scenarioUpdateState.addConditionIdMapping(domainId, dbId)
                    }

                    if (removed.isNotEmpty()) clearRemovedConditionsBitmaps(removed.mapNotNull { it.path })
                }
            )
        }
    }

    private suspend fun updateActions(eventDbId: Long, newActions: List<Action>) {
        val currentCompleteActions = currentDatabase.value.actionDao().getCompleteActions(eventDbId)
        val currentActionsEntities = currentCompleteActions.map { it.action }
        val updater = DatabaseListUpdater<Action, ActionEntity>()

        Log.d(TAG, "Updating actions in the database for event $eventDbId")
        updater.refreshUpdateValues(
            currentEntities = currentActionsEntities,
            newItems = newActions,
            mappingClosure = { actionInEvent ->
                actionInEvent.toEntity().apply {
                    eventId = eventDbId
                    clickOnConditionId = scenarioUpdateState.getClickOnConditionDatabaseId(actionInEvent)
                }
            }
        )
        Log.d(TAG, "Actions updater: $updater")

        currentDatabase.value.actionDao().let { actionDao ->
            updater.executeUpdate(
                addList = actionDao::addActions,
                updateList = actionDao::updateActions,
                removeList = actionDao::deleteActions,
                onSuccess = { addedMapping, added, updated, _ ->
                    addedMapping.forEach { (domainId, dbId) ->
                        scenarioUpdateState.addActionIdMapping(domainId, dbId)
                    }

                    updateActionsChildren(buildList {
                        addAll(added)
                        addAll(updated)
                    })
                }
            )
        }
    }

    private suspend fun updateActionsChildren(actions: List<Action>) {
        actions.forEach { action ->
            when (action) {
                is Intent -> {
                    action.extras?.let { extras ->
                        updateIntentExtras(
                            actionDbId = scenarioUpdateState.getActionDbId(action.id),
                            newExtras = extras,
                        )
                    }
                }

                is ToggleEvent -> {
                    updateEventToggles(
                        actionDbId = scenarioUpdateState.getActionDbId(action.id),
                        newToggles = action.eventToggles,
                    )
                }

                else -> Unit
            }
        }
    }

    private suspend fun updateIntentExtras(actionDbId: Long, newExtras: List<IntentExtra<out Any>>) {
        val updater = DatabaseListUpdater<IntentExtra<out Any>, IntentExtraEntity>()

        updater.refreshUpdateValues(
            currentEntities = currentDatabase.value.actionDao().getIntentExtras(actionDbId),
            newItems = newExtras,
            mappingClosure = { item ->
                item.toEntity().apply {
                    actionId = actionDbId
                }
            }
        )
        Log.d(TAG, "IntentExtra updater $updater")

        currentDatabase.value.actionDao().let { actionDao ->
            updater.executeUpdate(
                addList = actionDao::addIntentExtras,
                updateList = actionDao::updateIntentExtras,
                removeList = actionDao::deleteIntentExtras,
            )
        }
    }

    private suspend fun updateEventToggles(actionDbId: Long, newToggles: List<EventToggle>) {
        val updater = DatabaseListUpdater<EventToggle, EventToggleEntity>()

        updater.refreshUpdateValues(
            currentEntities = currentDatabase.value.actionDao().getEventsToggles(actionDbId),
            newItems = newToggles,
            mappingClosure = { item ->
                item.toEntity().apply {
                    actionId = actionDbId
                    toggleEventId = scenarioUpdateState.getEventDbId(item.targetEventId)
                }
            }
        )
        Log.d(TAG, "EventToggle updater $updater")

        currentDatabase.value.actionDao().let { actionDao ->
            updater.executeUpdate(
                addList = actionDao::addEventToggles,
                updateList = actionDao::updateEventToggles,
                removeList = actionDao::deleteEventToggles,
            )
        }
    }

    /**
     * Remove bitmaps from the application data folder.
     * @param removedEvents the list of events for the bitmaps to be removed.
     */
    private suspend fun clearRemovedEventsBitmaps(removedEvents: List<EventEntity>) {
        val deletedPaths = buildSet {
            removedEvents.forEach { event ->
                currentDatabase.value.conditionDao().getConditionsPaths(event.id).forEach { path ->
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
    internal suspend fun clearRemovedConditionsBitmaps(removedPath: List<String>) {
        Log.d(TAG, "Clearing removed conditions bitmaps: $removedPath")
        val deletedPaths = removedPath.filter { path ->
            path.isNotEmpty() && currentDatabase.value.conditionDao().getValidPathCount(path) == 0
        }

        Log.d(TAG, "Removed conditions count: ${removedPath.size}; Unused bitmaps after removal: ${deletedPaths.size}")
        if (deletedPaths.isNotEmpty()) bitmapManager.deleteImageConditionBitmaps(deletedPaths)
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioDataSource"
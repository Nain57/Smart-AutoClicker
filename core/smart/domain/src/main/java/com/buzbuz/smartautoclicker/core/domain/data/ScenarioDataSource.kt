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
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CountersEntity
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
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.event.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toEntity
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ScenarioDataSource @Inject constructor(
    private val database: ClickDatabase,
) {

    /** State of scenario during an update, to keep track of ids mapping. */
    private val scenarioUpdateState = ScenarioUpdateState()

    fun scenarios(): Flow<List<ScenarioWithEvents>> =
        database.scenarioDao().getScenariosWithEvents()

    fun allTriggerEvents(): Flow<List<CompleteEventEntity>> =
        database.eventDao().getAllTriggerEventsFlow()

    fun allImageEvents(): Flow<List<CompleteEventEntity>> =
        database.eventDao().getAllScreenEventsFlow()

    fun screenEventsCount(): Flow<Int> =
        database.eventDao().getScreenEventsCount()

    fun triggerEventsCount(): Flow<Int> =
        database.eventDao().getTriggerEventsCount()

    fun screenConditionsCount(): Flow<Int> =
        database.conditionDao().getScreenConditionsCount()

    fun triggerConditionsCount(): Flow<Int> =
        database.conditionDao().getTriggerConditionsCount()

    fun actionsCount(): Flow<Int> =
        database.actionDao().getActionsCount()


    suspend fun getScenario(scenarioId: Long): ScenarioWithEvents? =
        database.scenarioDao().getScenario(scenarioId)

    suspend fun getCompleteScenario(scenarioId: Long): CompleteScenario? =
        database.scenarioDao().getCompleteScenario(scenarioId)

    fun getScenarioFlow(scenarioId: Long): Flow<ScenarioWithEvents?> =
        database.scenarioDao().getScenarioFlow(scenarioId)

    suspend fun getScreenEvents(scenarioId: Long): List<CompleteEventEntity> =
        database.eventDao().getCompleteScreenEvents(scenarioId)

    fun getScreenEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>> =
        database.eventDao().getCompleteScreenEventsFlow(scenarioId)

    suspend fun getTriggerEvents(scenarioId: Long): List<CompleteEventEntity> =
        database.eventDao().getCompleteTriggerEvents(scenarioId)

    fun getTriggerEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>> =
        database.eventDao().getCompleteTriggerEventsFlow(scenarioId)

    fun getAllConditions(): Flow<List<ConditionEntity>> =
        database.conditionDao().getAllConditions()

    fun getAllActions(): Flow<List<CompleteActionEntity>> =
        database.actionDao().getAllActions()

    fun getCountersFlow(scenarioId: Long): Flow<List<CountersEntity>> =
        database.countersDao().getScenarioCountersFlow(scenarioId)

    suspend fun getCounters(scenarioId: Long): List<CountersEntity> =
        database.countersDao().getScenarioCounters(scenarioId)

    suspend fun getImageConditionPathUsageCount(path: String): Int =
        database.conditionDao().getValidPathCount(path)

    suspend fun getConditionName(conditionId: Long): String? =
        database.conditionDao().getConditionName(conditionId)

    suspend fun getEventName(eventId: Long): String? =
        database.eventDao().getEventName(eventId)

    suspend fun addScenario(scenario: Scenario): Long {
        Log.d(TAG, "Add scenario to the database: ${scenario.id}")
        return database.scenarioDao().add(scenario.toEntity())
    }

    suspend fun deleteScenario(scenarioId: Identifier, onImageConditionsRemoved: suspend (List<String>) -> Unit) {
        Log.d(TAG, "Delete scenario from the database: $scenarioId")

        val removedConditionsPath = mutableListOf<String>()
        database.eventDao().getEventsIds(scenarioId.databaseId).forEach { eventId ->
            database.conditionDao().getConditionsPaths(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        database.scenarioDao().delete(scenarioId.databaseId)
        onImageConditionsRemoved(removedConditionsPath)
    }

    suspend fun addCompleteScenario(
        scenario: Scenario,
        events: List<Event>,
        counters: List<Counter>,
        onImageConditionsRemoved: suspend (List<String>) -> Unit,
    ): Long? {
        Log.d(TAG, "Add scenario copy to the database: ${scenario.id}")

        // Check the events correctness
        if (!events.areComplete())
            throw IllegalArgumentException("Can't update scenario content, one of the event is not complete")

        return try {
            var scenarioId: Identifier? = null
            val conditionsRemoved = mutableListOf<String>()
            database.withTransaction {
                // First insert the scenario to get its database id, and put it in all events
                scenarioId = Identifier(
                    databaseId = database.scenarioDao().add(scenario.toEntity())
                )

                updateEvents(
                    scenarioDbId = scenarioId.databaseId,
                    events = events,
                    onImageConditionsRemoved = { removed -> conditionsRemoved += removed },
                )

                updateCounters(
                    scenarioDbId = scenarioId.databaseId,
                    newCounters = counters,
                )
            }

            onImageConditionsRemoved(conditionsRemoved)
            scenarioId?.databaseId
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun updateScenario(
        scenario: Scenario,
        events: List<Event>,
        counters: List<Counter>,
        onImageConditionsRemoved: suspend (List<String>) -> Unit,
    ): Boolean {
        Log.d(TAG, "Update scenario in the database: ${scenario.id}")

        return try {
            val conditionsRemoved = mutableListOf<String>()
            database.withTransaction {
                // Update scenario entity values
                database.scenarioDao().update(scenario.toEntity())
                // Update scenario content
                updateEvents(
                    scenarioDbId = scenario.id.databaseId,
                    events = events,
                    onImageConditionsRemoved = { removed -> conditionsRemoved += removed },
                )
                // Update counters
                updateCounters(
                    scenarioDbId = scenario.id.databaseId,
                    newCounters = counters,
                )
            }

            onImageConditionsRemoved(conditionsRemoved)
            true
        } catch (ex: Exception) {
            Log.e(TAG, "Error while updating scenario\n* Scenario=$scenario\n* Events=$events\n", ex)
            false
        }
    }

    suspend fun markAsUsed(scenarioDbId: Long) {
        database.scenarioDao().let { scenarioDao ->
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

    suspend fun getLegacyImageConditions(): List<ConditionEntity> =
        database.conditionDao().getLegacyImageConditions()

    fun getLegacyImageConditionsFlow(): Flow<List<ConditionEntity>> = flow {
        emitAll(database.conditionDao().getLegacyImageConditionsFlow())
    }

    suspend fun updateLegacyImageCondition(condition: ConditionEntity, newPath: String) {
        database.conditionDao().updateCondition(condition.copy(path = newPath))
    }

    private suspend fun updateEvents(
        scenarioDbId: Long,
        events: List<Event>,
        onImageConditionsRemoved: suspend (List<String>) -> Unit,
    ) {
        scenarioUpdateState.initUpdateState()
        val updater = DatabaseListUpdater<Event, EventEntity>()

        Log.d(TAG, "Updating events in the database for scenario $scenarioDbId")
        updater.refreshUpdateValues(
            currentEntities = database.eventDao().getEvents(scenarioDbId),
            newItems = events,
            mappingClosure = { event ->
                event.toEntity().apply {
                    scenarioId = scenarioDbId
                }
            }
        )
        Log.d(TAG, "Events updater: $updater")

        database.eventDao().let { eventDao ->
            updater.executeUpdate(
                addList = eventDao::addEvents,
                updateList = eventDao::updateEvent,
                removeList = eventDao::deleteEvents,
                onSuccess = { addedMapping, added, updated, removed ->
                    addedMapping.forEach { (domainId, dbId) ->
                        scenarioUpdateState.addEventIdMapping(domainId, dbId)
                    }

                    updateEventsChildren(
                        events = buildList {
                            addAll(added)
                            addAll(updated)
                        },
                        onImageConditionsRemoved = onImageConditionsRemoved,
                    )

                    if (removed.isNotEmpty()) onImageConditionsRemoved(events.getRemovedConditionsPath(removed))
                }
            )
        }
    }

    private suspend fun updateEventsChildren(
        events: List<Event>,
        onImageConditionsRemoved: suspend (List<String>) -> Unit,
    ) {
        // Actions can reference a condition, do them all first
        events.forEach { event ->
            updateConditions(
                eventDbId = scenarioUpdateState.getEventDbId(event.id),
                newConditions = event.conditions,
                onImageConditionsRemoved = onImageConditionsRemoved,
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

    private suspend fun updateConditions(
        eventDbId: Long,
        newConditions: List<Condition>,
        onImageConditionsRemoved: suspend (List<String>) -> Unit,
    ) {
        val updater = DatabaseListUpdater<Condition, ConditionEntity>()

        Log.d(TAG, "Updating conditions in the database for event $eventDbId")
        updater.refreshUpdateValues(
            currentEntities = database.conditionDao().getConditions(eventDbId),
            newItems = newConditions,
            mappingClosure = { condition ->
                condition.copyWithNewId(evtId = Identifier(databaseId = eventDbId)).toEntity()
            }
        )
        Log.d(TAG, "Conditions updater: $updater")

        database.conditionDao().let { conditionDao ->
            updater.executeUpdate(
                addList = conditionDao::addConditions,
                updateList = conditionDao::updateConditions,
                removeList = conditionDao::deleteConditions,
                onSuccess = { addedMapping, _, _, removed ->
                    addedMapping.forEach { (domainId, dbId) ->
                        scenarioUpdateState.addConditionIdMapping(domainId, dbId)
                    }

                    if (removed.isNotEmpty()) onImageConditionsRemoved(removed.mapNotNull { it.path })
                }
            )
        }
    }

    private suspend fun updateActions(eventDbId: Long, newActions: List<Action>) {
        val currentCompleteActions = database.actionDao().getCompleteActions(eventDbId)
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

        database.actionDao().let { actionDao ->
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
            currentEntities = database.actionDao().getIntentExtras(actionDbId),
            newItems = newExtras,
            mappingClosure = { item ->
                item.toEntity().apply {
                    actionId = actionDbId
                }
            }
        )
        Log.d(TAG, "IntentExtra updater $updater")

        database.actionDao().let { actionDao ->
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
            currentEntities = database.actionDao().getEventsToggles(actionDbId),
            newItems = newToggles,
            mappingClosure = { item ->
                item.toEntity().apply {
                    actionId = actionDbId
                    toggleEventId = scenarioUpdateState.getEventDbId(item.targetEventId)
                }
            }
        )
        Log.d(TAG, "EventToggle updater $updater")

        database.actionDao().let { actionDao ->
            updater.executeUpdate(
                addList = actionDao::addEventToggles,
                updateList = actionDao::updateEventToggles,
                removeList = actionDao::deleteEventToggles,
            )
        }
    }

    private suspend fun updateCounters(scenarioDbId: Long, newCounters: List<Counter>) {
        val toBeRemoved = database.countersDao()
            .getScenarioCounters(scenarioDbId)
            .toMutableList()

        newCounters.forEach { counter ->
            // Discard all items added/updated from the to be removed list
            val oldIndex = toBeRemoved.indexOfFirst { oldCounter -> oldCounter.name == counter.counterName }
            if (oldIndex in toBeRemoved.indices) toBeRemoved.remove(toBeRemoved[oldIndex])

            database.countersDao().upsertCounter(
                counter.toEntity().copy(scenarioId = scenarioDbId)
            )
        }

        toBeRemoved.forEach { counter ->
            database.countersDao().deleteCounter(counter.name, scenarioDbId)
        }
    }

    private fun List<Event>.getRemovedConditionsPath(removedEntities: List<EventEntity>): List<String> =
        buildList {
            removedEntities.forEach { removedEntity ->
                // Find the deleted domain event, get its image conditions list and map to their path
                val removedEvent = this@getRemovedConditionsPath
                    .find { event -> event is ScreenEvent && event.id.databaseId == removedEntity.id }
                    ?.conditions?.filterIsInstance<ScreenCondition.Image>()
                    ?.map { condition -> condition.path }
                    ?: return@forEach

                addAll(removedEvent)
            }
        }
}

/** Tag for logs. */
private const val TAG = "ScenarioDataSource"
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
package com.buzbuz.smartautoclicker.domain

import android.graphics.Point
import android.net.Uri

import com.buzbuz.smartautoclicker.backup.BackupEngine.BackupProgress
import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.domain.edition.EditedEndCondition
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent
import com.buzbuz.smartautoclicker.domain.edition.EditedScenario
import com.buzbuz.smartautoclicker.domain.edition.INVALID_EDITED_ITEM_ID
import com.buzbuz.smartautoclicker.domain.edition.isValidForSave
import com.buzbuz.smartautoclicker.domain.model.action.toAction
import com.buzbuz.smartautoclicker.domain.model.action.toEntity
import com.buzbuz.smartautoclicker.domain.model.condition.toCondition
import com.buzbuz.smartautoclicker.domain.model.condition.toEntity
import com.buzbuz.smartautoclicker.domain.model.endcondition.toEndCondition
import com.buzbuz.smartautoclicker.domain.model.endcondition.toEntity
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.domain.model.event.toEntity
import com.buzbuz.smartautoclicker.domain.model.event.toEvent
import com.buzbuz.smartautoclicker.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.domain.model.scenario.toEntity
import com.buzbuz.smartautoclicker.domain.model.scenario.toScenario
import com.buzbuz.smartautoclicker.extensions.mapList

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

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
    private val backupEngine: com.buzbuz.smartautoclicker.backup.BackupEngine,
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
    private val actionsUpdater = DatabaseListUpdater<EditedAction, CompleteActionEntity>(
        itemPrimaryKeySupplier = { editedAction -> editedAction.action.id },
        entityPrimaryKeySupplier = { completeActionEntity -> completeActionEntity.action.id },
    )
    /** Updater for a list of end conditions. */
    private val endConditionsUpdater = DatabaseListUpdater<EditedEndCondition, EndConditionEntity>(
        itemPrimaryKeySupplier = { editedEndCondition -> editedEndCondition.endCondition.id },
        entityPrimaryKeySupplier = { endConditionEntity -> endConditionEntity.id },
    )

    /** True when data are being inserted/edited, false if not. */
    private val isEditingData: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val scenarios = scenarioDao.getScenariosWithEvents()
        .filterWhenEditing()
        .mapList { it.toScenario() }

    override suspend fun addScenario(scenario: Scenario): Long =
        scenarioDao.add(scenario.toEntity())

    override suspend fun deleteScenario(scenario: Scenario) {
        val removedConditionsPath = mutableListOf<String>()
        eventDao.getEventsIds(scenario.id).forEach { eventId ->
            conditionsDao.getConditionsPath(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
            }
        }

        scenarioDao.delete(scenario.toEntity())
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    override fun getScenarioWithEndConditionsFlow(scenarioId: Long) = scenarioDao.getScenarioWithEndConditions(scenarioId)
        .filterWhenEditing()
        .mapNotNull { scenarioWithEndConditions ->
            scenarioWithEndConditions ?: return@mapNotNull null
            scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions.map { it.toEndCondition() }
        }

    override suspend fun getCompleteEventList(scenarioId: Long): List<Event> =
        eventDao.getCompleteEvents(scenarioId).map { it.toEvent() }

    override fun getCompleteEventListFlow(scenarioId: Long): Flow<List<Event>> =
        eventDao.getCompleteEventsFlow(scenarioId)
            .filterWhenEditing()
            .mapList { it.toEvent() }

    override fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()
        .filterWhenEditing()
        .mapList { it.toEvent() }

    override fun getAllActions(): Flow<List<Action>> = actionDao.getAllActions()
        .filterWhenEditing()
        .mapList { it.toAction() }

    override fun getAllConditions(): Flow<List<Condition>> = conditionsDao.getAllConditions()
        .filterWhenEditing()
        .mapList { it.toCondition() }

    override suspend fun updateScenario(editedScenario: EditedScenario) {
        // Ensure correctness of the inserted scenario
        if (!editedScenario.isValidForSave())
            throw IllegalArgumentException("Can't save this scenario, it is invalid.")

        isEditingData.value = true

        // Update scenario entity values
        scenarioDao.update(editedScenario.scenario.toEntity())

        val eventItemIdMap = updateScenarioEvents(editedScenario.scenario.id, editedScenario.events)
        editedScenario.events.forEach { editedEvent ->
            updateEventConditions(eventItemIdMap.getEventId(editedEvent.itemId), editedEvent.event.conditions ?: emptyList())
            updateEventActions(editedEvent.itemId, editedEvent.editedActions, eventItemIdMap)
        }
        updateEndConditions(editedScenario.scenario.id, editedScenario.endConditions, eventItemIdMap)

        isEditingData.value = false
    }

    /**
     * Insert/update the events entity and keep the mapping between db ids and edition ids.
     * Keep also track of the events to be removed, and remove them all at once.
     */
    private suspend fun updateScenarioEvents(scenarioId: Long, events: List<EditedEvent>): Map<Int, Long> {
        val itemIdToDbIdMap = mutableMapOf<Int, Long>()

        val eventsToBeRemoved = getCompleteEventList(scenarioId).toMutableList()
        events.forEachIndexed { index, editedEvent ->
            editedEvent.event.priority = index

            if (editedEvent.event.id == 0L) {
                itemIdToDbIdMap[editedEvent.itemId] = eventDao.addEvent(editedEvent.event.toEntity())
            } else {
                eventDao.updateEvent(editedEvent.event.toEntity())
                itemIdToDbIdMap[editedEvent.itemId] = editedEvent.event.id
                eventsToBeRemoved.removeIf { it.id == editedEvent.event.id }
            }
        }

        if (eventsToBeRemoved.isNotEmpty()) eventDao.deleteEvents(eventsToBeRemoved.map { it.toEntity() })

        return itemIdToDbIdMap
    }

    private suspend fun updateEventConditions(evtId: Long, conditions: List<Condition>) {
        saveNewConditionsBitmap(conditions)

        conditionsUpdater.refreshUpdateValues(
            currentEntities = conditionsDao.getConditions(evtId),
            newItems = conditions,
            toEntity = { _, condition ->
                condition.toEntity().apply {
                    eventId = evtId
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

    private suspend fun updateEventActions(
        eventItemId: Int,
        actions: List<EditedAction>,
        evtItemIdToDbIdMap: Map<Int, Long>,
    ) {
        val evtId = evtItemIdToDbIdMap.getEventId(eventItemId)

        actionsUpdater.refreshUpdateValues(
            currentEntities = actionDao.getCompleteActions(evtId),
            newItems = actions,
            toEntity = { index, editedAction ->
                val toggleEvtId = if (editedAction.action is Action.ToggleEvent && editedAction.toggleEventItemId != INVALID_EDITED_ITEM_ID) {
                    evtItemIdToDbIdMap.getEventId(editedAction.toggleEventItemId)
                } else {
                    null
                }

                editedAction.action.toEntity().apply {
                    action.eventId = evtId
                    action.toggleEventId = toggleEvtId
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
        endConditions: List<EditedEndCondition>,
        evtItemIdToDbIdMap: Map<Int, Long>,
    ) {
        endConditionsUpdater.refreshUpdateValues(
            currentEntities = endConditionDao.getEndConditions(scenarioId),
            newItems = endConditions,
            toEntity = { _, editedEndCondition ->
                editedEndCondition.endCondition.toEntity().apply {
                    eventId = evtItemIdToDbIdMap.getEventId(editedEndCondition.eventItemId)
                }
            }
        )

        endConditionDao.syncEndConditions(
            endConditionsUpdater.toBeAdded,
            endConditionsUpdater.toBeUpdated,
            endConditionsUpdater.toBeRemoved,
        )
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

    override fun createScenarioBackup(zipFileUri: Uri, scenarios: List<Long>, screenSize: Point) = channelFlow  {
        launch {
            backupEngine.createBackup(
                zipFileUri,
                scenarios.mapNotNull {
                    scenarioDao.getCompleteScenario(it)
                },
                screenSize,
                BackupProgress(
                    onError = { send(Backup.Error) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onCompleted = { success, failureCount, compatWarning ->
                        send(Backup.Completed(success.size, failureCount, compatWarning))
                    }
                )
            )
        }
    }

    override fun restoreScenarioBackup(zipFileUri: Uri, screenSize: Point) = channelFlow {
        launch {
            backupEngine.loadBackup(
                zipFileUri,
                screenSize,
                BackupProgress(
                    onError = { send(Backup.Error) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onVerification = { send(Backup.Verification) },
                    onCompleted = { success, failureCount, compatWarning ->
                        insertRestoredScenarios(success)
                        send(Backup.Completed(success.size, failureCount, compatWarning))
                    }
                )
            )
        }
    }

    private suspend fun insertRestoredScenarios(completeScenarios: List<CompleteScenario>) {
        isEditingData.value = true

        completeScenarios.forEach { completeScenario ->
            // Insert scenario to get the new database id
            val scenarioId = scenarioDao.add(completeScenario.scenario.copy(id = 0))

            // Insert all events and map their old id to the new database id
            val eventIdMap = mutableMapOf<Long, Long>()
            completeScenario.events.forEach { completeEvent ->
                eventIdMap[completeEvent.event.id] =
                    eventDao.addEvent(completeEvent.event.copy(id = 0, scenarioId = scenarioId))
            }

            // Insert all conditions and actions with the new events ids
            completeScenario.events.forEach { completeEvent ->
                conditionsDao.addConditions(
                    completeEvent.conditions.map { it.copy(id = 0, eventId = eventIdMap[it.eventId]!!) }
                )

                completeEvent.actions.forEach { completeAction ->
                    val actionId = actionDao.addAction(
                        completeAction.action.copy(
                            id = 0,
                            eventId = eventIdMap[completeAction.action.eventId]!!,
                            toggleEventId = completeAction.action.toggleEventId?.let { toggleEvtId ->
                                eventIdMap[toggleEvtId]!!
                            }
                        )
                    )
                    completeAction.intentExtras.forEach { intentExtra ->
                        intentExtra.actionId = actionId
                        actionDao.addIntentExtra(intentExtra)
                    }
                }
            }

            // Insert all end conditions
            endConditionDao.addEndConditions(
                completeScenario.endConditions.map { endCondition ->
                    endCondition.copy(
                        id = 0,
                        scenarioId = scenarioId,
                        eventId = eventIdMap[endCondition.eventId]!!,
                    )
                }
            )
        }

        isEditingData.value = false
    }

    /**
     * Propagate the changes of a flow only when [isEditingData] is false.
     * This allows to prevent the clients to receives updates when a scenario is being updated.
     */
    private fun <T> Flow<T>.filterWhenEditing() = combineTransform(isEditingData) { value, isEditing ->
        if (!isEditing) emit(value)
    }
}

private fun Map<Int, Long>.getEventId(itemId: Int): Long =
    get(itemId) ?: throw IllegalStateException("Can't find event id for item id $itemId")

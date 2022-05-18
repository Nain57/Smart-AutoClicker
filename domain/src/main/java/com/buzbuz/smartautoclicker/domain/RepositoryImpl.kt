/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import android.graphics.Point
import android.net.Uri

import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario
import com.buzbuz.smartautoclicker.extensions.mapList

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
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
    /** The Dao for accessing the scenario end conditions. */
    private val endConditionDao = database.endConditionDao()

    override val scenarios = scenarioDao.getScenariosWithEvents().mapList { it.toScenario() }

    override suspend fun addScenario(scenario: Scenario): Long {
        return scenarioDao.add(scenario.toEntity())
    }

    override suspend fun updateScenario(scenario: Scenario) {
        scenarioDao.update(scenario.toEntity())
    }

    override suspend fun deleteScenario(scenario: Scenario) {
        val removedConditionsPath = mutableListOf<String>()
        eventDao.getEventsIds(scenario.id).forEach { eventId ->
            conditionsDao.getConditionsPath(eventId).forEach { path ->
                if (!removedConditionsPath.contains(path))  removedConditionsPath.add(path)
            }
        }

        scenarioDao.delete(scenario.toEntity())
        clearRemovedConditionsBitmaps(removedConditionsPath)
    }

    override fun getScenario(scenarioId: Long): Flow<Scenario> = scenarioDao.getScenarioWithEvents(scenarioId)
        .map { scenarioEntity ->
            scenarioEntity.toScenario()
        }

    override fun getScenarioWithEndConditionsFlow(scenarioId: Long) = scenarioDao.getScenarioWithEndConditionsFlow(scenarioId)
        .map { scenarioWithEndConditions ->
            scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions.map { it.toEndCondition() }
        }

    override suspend fun getScenarioWithEndConditions(scenarioId: Long): Pair<Scenario, List<EndCondition>> {
        val scenarioWithEndConditions = scenarioDao.getScenarioWithEndConditions(scenarioId)
        return scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions
            .map { it.toEndCondition() }
    }

    override suspend fun updateEndConditions(scenarioId: Long, endConditions: List<EndCondition>) {
        endConditionDao.updateEndConditions(scenarioId, endConditions.map { it.toEntity() })
    }

    override fun getEventCount(): Flow<Int> = eventDao.getEventsCount()
    override fun getActionsCount(): Flow<Int> = eventDao.getActionsCount()
    override fun getConditionsCount(): Flow<Int> = conditionsDao.getConditionsCount()

    override fun getEventList(scenarioId: Long): Flow<List<Event>> =
        eventDao.getEvents(scenarioId).mapList { it.toEvent() }

    override fun getCompleteEventList(scenarioId: Long): Flow<List<Event>> =
        eventDao.getCompleteEvents(scenarioId).mapList { it.toEvent() }

    override suspend fun getCompleteEvent(eventId: Long) = eventDao.getEvent(eventId).toEvent()

    override fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents().mapList { it.toEvent() }

    override fun getAllActions(): Flow<List<Action>> = eventDao.getAllActions().mapList { it.toAction() }

    override fun getAllConditions(): Flow<List<Condition>> = conditionsDao.getAllConditions().mapList { it.toCondition() }

    override suspend fun addEvent(event: Event): Boolean {
        event.conditions?.let {
            saveNewConditionsBitmap(it)
        }

        event.toCompleteEntity()?.let { entity ->
            eventDao.addCompleteEvent(entity)
            return true
        } ?: return false
    }

    override suspend fun updateEvent(event: Event) {
        event.conditions?.let {
            saveNewConditionsBitmap(it)
        }

        event.toCompleteEntity()?.let { eventEntity ->
            // Update database values
            val deletedConditions = eventDao.updateCompleteEvent(eventEntity)
            // Remove the conditions bitmap if unused
            clearRemovedConditionsBitmaps(deletedConditions.map { it.path })
        }
    }

    override suspend fun updateEventsPriority(events: List<Event>) {
        eventDao.updateEventList(events.map { it.toEntity() })
    }

    override suspend fun removeEvent(event: Event) {
        val removedConditions = conditionsDao.getConditionsPath(event.id)
        eventDao.deleteEvent(event.toEntity())
        clearRemovedConditionsBitmaps(removedConditions)
    }

    override suspend fun getBitmap(path: String, width: Int, height: Int) = bitmapManager.loadBitmap(path, width, height)

    override fun cleanCache() {
        bitmapManager.releaseCache()
    }

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
                scenarios.map {
                    scenarioDao.getCompleteScenario(it)
                },
                screenSize,
                com.buzbuz.smartautoclicker.backup.BackupEngine.BackupProgress(
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
                com.buzbuz.smartautoclicker.backup.BackupEngine.BackupProgress(
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
        completeScenarios.forEach { completeScenario ->
            val scenarioId = scenarioDao.add(completeScenario.scenario.copy(id = 0))

            val eventIdMap = mutableMapOf<Long, Long>()
            completeScenario.events.forEach { completeEvent ->
                val eventId = eventDao.addCompleteEvent(
                    completeEvent.copy(event = completeEvent.event.copy(id = 0, scenarioId = scenarioId))
                )
                eventIdMap[completeEvent.event.id] = eventId
            }

            endConditionDao.add(
                completeScenario.endConditions.map { endCondition ->
                    endCondition.copy(
                        id = 0,
                        scenarioId = scenarioId,
                        eventId = eventIdMap[endCondition.eventId]!!,
                    )
                }
            )
        }
    }
}

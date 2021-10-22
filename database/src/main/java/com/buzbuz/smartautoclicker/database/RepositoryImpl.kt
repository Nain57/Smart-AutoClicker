/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.database

import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.domain.*
import com.buzbuz.smartautoclicker.database.domain.toEvent
import com.buzbuz.smartautoclicker.database.domain.toScenario
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.dao.EntityListUpdater
import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.extensions.mapList

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    private val bitmapManager: BitmapManager
): Repository {

    /** The Dao for accessing the database. */
    private val scenarioDao = database.scenarioDao()
    /** The Dao for accessing the database. */
    private val eventDao = database.eventDao()
    /** The Dao for accessing the conditions. */
    private val conditionsDao = database.conditionDao()

    /** List updater for the list of actions. */
    private val actionsUpdater = EntityListUpdater<ActionEntity, Long>(
        defaultPrimaryKey = 0L,
        primaryKeySupplier = { action -> action.id },
    )
    /** List updater for the list of conditions. */
    private val conditionsUpdater = EntityListUpdater<ConditionEntity, Long>(
        defaultPrimaryKey = 0L,
        primaryKeySupplier = { condition -> condition.id },
    )

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
            eventDao.addEvent(entity)
            return true
        } ?: return false
    }

    override suspend fun updateEvent(event: Event) {
        event.conditions?.let {
            saveNewConditionsBitmap(it)
        }

        event.toCompleteEntity()?.let { eventEntity ->
            // Update action priorities
            eventEntity.actions.forEachIndexed { index, actionEntity ->
                actionEntity.priority = index
            }

            // Get current database values
            val oldActions = eventDao.getActions(eventEntity.event.id)
            val oldConditions = conditionsDao.getConditions(eventEntity.event.id)

            // Refresh the updaters
            actionsUpdater.refreshUpdateValues(oldActions, eventEntity.actions)
            conditionsUpdater.refreshUpdateValues(oldConditions, eventEntity.conditions)

            // Update database values
            eventDao.updateEvent(eventEntity.event, actionsUpdater, conditionsUpdater)

            // Remove the conditions bitmap if unused
            clearRemovedConditionsBitmaps(conditionsUpdater.toBeRemoved.map { it.path })
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
}

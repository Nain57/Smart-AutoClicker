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
package com.buzbuz.smartautoclicker.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EventEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the events in the database. */
@Dao
internal abstract class EventDao {

    /**
     * Get the list of events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events.
     */
    @Query("SELECT * FROM event_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract fun getEvents(scenarioId: Long): Flow<List<EventEntity>>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM event_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract fun getCompleteEvents(scenarioId: Long): Flow<List<CompleteEventEntity>>

    /**
     * Get the list of event identifier for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events id.
     */
    @Transaction
    @Query("SELECT id FROM event_table WHERE scenario_id=:scenarioId")
    abstract suspend fun getEventsIds(scenarioId: Long): List<Long>

    /**
     * Get the complete event.
     *
     * @param eventId the identifier of the event.
     * @return the flow on the event.
     */
    @Transaction
    @Query("SELECT * FROM event_table WHERE id=:eventId")
    abstract suspend fun getEvent(eventId: Long): CompleteEventEntity

    /**
     * Get all events from all scenarios.
     *
     * @return the list containing all events.
     */
    @Transaction
    @Query("SELECT * FROM event_table ORDER BY name")
    abstract fun getAllEvents(): Flow<List<CompleteEventEntity>>

    /**
     * Get all actions from all events.
     *
     * @return the list containing all actions.
     */
    @Transaction
    @Query("SELECT * FROM action_table ORDER BY name")
    abstract fun getAllActions(): Flow<List<ActionEntity>>

    /**
     * Update the provided events in the database.
     * Priorities will be updated.
     *
     * @param events the list events to update.
     */
    suspend fun updateEventList(events: List<EventEntity>) {
        events.forEachIndexed { index, event ->
            event.priority = index
        }

        updateEvents(events)
    }

    /**
     * Add an event to the database.
     * This will save in the database the event and its actions and conditions. This will set automatically the event id
     * fields in all substructure and ensure action priority correctness.
     *
     * @param event the complete event to be added.
     */
    @Transaction
    open suspend fun addEvent(event: CompleteEventEntity) {
        val eventId = addEvent(event.event)

        event.actions.forEachIndexed { index, action ->
            action.eventId = eventId
            action.priority = index
        }
        addActions(event.actions)

        event.conditions.forEach {
            it.eventId = eventId
        }
        addConditions(event.conditions)
    }

    /**
     * Update an event in the database.
     *
     * This will update all events values and all of it's conditions and actions. If new actions/conditions have been
     * added, they will be added to the database. If actions/conditions have been removed, they will be removed from the
     * database.
     *
     * @param event the event to update.
     * @param actionsUpdater contains the list update information for the actions.
     *                       [EntityListUpdater.refreshUpdateValues] must have been called first.
     * @param conditionsUpdater contains the list update information for the conditions.
     *                          [EntityListUpdater.refreshUpdateValues] must have been called first.
     */
    @Transaction
    open suspend fun updateEvent(
        event: EventEntity,
        actionsUpdater: EntityListUpdater<ActionEntity, Long>,
        conditionsUpdater: EntityListUpdater<ConditionEntity, Long>,
    ) {
        updateEvent(event)

        actionsUpdater.toBeAdded.forEach { action ->
            action.eventId = event.id
        }
        addActions(actionsUpdater.toBeAdded)
        updateActions(actionsUpdater.toBeUpdated)
        deleteActions(actionsUpdater.toBeRemoved)

        conditionsUpdater.toBeAdded.forEach { condition ->
            condition.eventId = event.id
        }
        addConditions(conditionsUpdater.toBeAdded)
        updateConditions(conditionsUpdater.toBeUpdated)
        deleteConditions(conditionsUpdater.toBeRemoved)
    }

    /**
     * Delete an event from the database.
     * Actions and conditions of this event will be deleted as well due to the CASCADE action on event deletion.
     *
     * @param event the conditions to be added.
     */
    @Delete
    abstract suspend fun deleteEvent(event: EventEntity)

    /**
     * Get the list actions for an event, ordered by priority.
     *
     * @param eventId the identifier of the event to get the actions from.
     */
    @Query("SELECT * FROM action_table WHERE eventId=:eventId ORDER BY priority")
    abstract suspend fun getActions(eventId: Long): List<ActionEntity>

    /**
     * Add an event to the database.
     * @param event the event to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addEvent(event: EventEntity): Long
    /**
     * Add a list of actions to the database.
     * @param actions the actions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addActions(actions: List<ActionEntity>)
    /**
     * Add a list of conditions to the database.
     * @param conditions the conditions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addConditions(conditions: List<ConditionEntity>)

    /**
     * Update an event in the database.
     * @param event the event to be updated.
     */
    @Update
    protected abstract suspend fun updateEvent(event: EventEntity)
    /**
     * Update a list of events in the database.
     * @param events the list of events to be updated.
     */
    @Update
    protected abstract suspend fun updateEvents(events: List<EventEntity>)
    /**
     * Update a list of actions in the database.
     * @param actions the list of actions to be updated.
     */
    @Update
    protected abstract suspend fun updateActions(actions: List<ActionEntity>)
    /**
     * Update a list of conditions in the database.
     * @param conditions the list of conditions to be updated.
     */
    @Update
    protected abstract suspend fun updateConditions(conditions: List<ConditionEntity>)

    /**
     * Delete a list of actions in the database.
     * @param actions the conditions to be removed.
     */
    @Delete
    protected abstract suspend fun deleteActions(actions: List<ActionEntity>)
    /**
     * Delete a list of conditions in the database.
     * @param conditions the conditions to be removed.
     */
    @Delete
    protected abstract suspend fun deleteConditions(conditions: List<ConditionEntity>)
}
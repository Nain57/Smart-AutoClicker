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
package com.buzbuz.smartautoclicker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.core.database.EVENT_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the events in the database. */
@Dao
abstract class EventDao {

    /**
     * Get the list of event identifier for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events id.
     */
    @Transaction
    @Query("SELECT id FROM $EVENT_TABLE WHERE scenario_id=:scenarioId")
    abstract suspend fun getEventsIds(scenarioId: Long): List<Long>

    /**
     * Get the list of events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events.
     */
    @Query("SELECT * FROM $EVENT_TABLE WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract suspend fun getEvents(scenarioId: Long): List<EventEntity>

    /**
     * Get the list of image events from all scenarios.
     * @return the flow on the list of events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE type='IMAGE_EVENT' ORDER BY name")
    abstract fun getAllImageEventsFlow(): Flow<List<CompleteEventEntity>>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE scenario_id=:scenarioId AND type='IMAGE_EVENT' ORDER BY priority")
    abstract suspend fun getCompleteImageEvents(scenarioId: Long): List<CompleteEventEntity>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE scenario_id=:scenarioId AND type='IMAGE_EVENT' ORDER BY priority")
    abstract fun getCompleteImageEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>>

    /**
     * Get the list of trigger events from all scenarios.
     * @return the flow on the list of events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE type='TRIGGER_EVENT' ORDER BY name")
    abstract fun getAllTriggerEventsFlow(): Flow<List<CompleteEventEntity>>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE scenario_id=:scenarioId AND type='TRIGGER_EVENT' ORDER BY name")
    abstract suspend fun getCompleteTriggerEvents(scenarioId: Long): List<CompleteEventEntity>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM $EVENT_TABLE WHERE scenario_id=:scenarioId AND type='TRIGGER_EVENT' ORDER BY name")
    abstract fun getCompleteTriggerEventsFlow(scenarioId: Long): Flow<List<CompleteEventEntity>>

    /**
     * Add a list of events to the database.
     * @param events the events to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addEvents(events: List<EventEntity>): List<Long>

    /**
     * Update a list of events in the database.
     * @param events the events to be updated.
     */
    @Update
    abstract suspend fun updateEvent(events: List<EventEntity>)

    /**
     * Delete an event list from the database.
     * Actions and conditions of this event will be deleted as well due to the CASCADE action on event deletion.
     *
     * @param events the events to be deleted
     */
    @Delete
    abstract suspend fun deleteEvents(events: List<EventEntity>)
}
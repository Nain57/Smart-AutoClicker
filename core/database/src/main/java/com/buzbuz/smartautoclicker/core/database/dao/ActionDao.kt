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
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE

import com.buzbuz.smartautoclicker.core.database.EVENT_TOGGLE_TABLE
import com.buzbuz.smartautoclicker.core.database.INTENT_EXTRA_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access the actions in the database. */
@Dao
abstract class ActionDao {

    /**
     * Get all actions from all events.
     *
     * @return the list containing all actions.
     */
    @Transaction
    @Query("SELECT * FROM $ACTION_TABLE ORDER BY name")
    abstract fun getAllActions(): Flow<List<CompleteActionEntity>>

    /**
     * Get the list of complete actions for an event, ordered by priority.
     *
     * @param eventId the identifier of the event to get the actions from.
     */
    @Transaction
    @Query("SELECT * FROM $ACTION_TABLE WHERE eventId=:eventId ORDER BY priority")
    abstract suspend fun getCompleteActions(eventId: Long): List<CompleteActionEntity>

    /**
     * Get the list of intent extras for a given action.
     *
     * @param actionId the identifier of the action to get the intent extras from.
     * @return the list of intent extras for the action.
     */
    @Query("SELECT * FROM $INTENT_EXTRA_TABLE WHERE action_id=:actionId ORDER BY id")
    abstract suspend fun getIntentExtras(actionId: Long): List<IntentExtraEntity>

    /**
     * Get the list of events toggles for a given action.
     *
     * @param actionId the identifier of the action to get the event toggles from.
     * @return the list of event toggles for the action.
     */
    @Query("SELECT * FROM $EVENT_TOGGLE_TABLE WHERE action_id=:actionId ORDER BY id")
    abstract suspend fun getEventsToggles(actionId: Long): List<EventToggleEntity>

    /**
     * Add a list of action to the database.
     * @param actions the actions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addActions(actions: List<ActionEntity>): List<Long>

    /**
     * Add a list of intent extras to the database.
     * @param intentExtras the intent extras to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addIntentExtras(intentExtras: List<IntentExtraEntity>): List<Long>

    /**
     * Add a list of event toggle to the database.
     * @param eventToggles the event toggles to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addEventToggles(eventToggles: List<EventToggleEntity>): List<Long>

    /**
     * Update a list of action to the database.
     * @param actions the actions to be updated.
     */
    @Update
    abstract suspend fun updateActions(actions: List<ActionEntity>)

    /**
     * Update a list of intent extra in the database.
     * @param extras the intent extras to be updated.
     */
    @Update
    abstract suspend fun updateIntentExtras(extras: List<IntentExtraEntity>)

    /**
     * Update a list of event toggle in the database.
     * @param eventToggles the event toggles to be updated.
     */
    @Update
    abstract suspend fun updateEventToggles(eventToggles: List<EventToggleEntity>)

    /**
     * Delete a list of actions in the database.
     * @param actions the actions to be removed.
     */
    @Delete
    abstract suspend fun deleteActions(actions: List<ActionEntity>)

    /**
     * Delete a list of intent extras in the database.
     * @param extras the intent extras to be removed.
     */
    @Delete
    abstract suspend fun deleteIntentExtras(extras: List<IntentExtraEntity>)

    /**
     * Delete a list of event toggle in the database.
     * @param eventToggles the event toggles to be removed.
     */
    @Delete
    abstract suspend fun deleteEventToggles(eventToggles: List<EventToggleEntity>)
}
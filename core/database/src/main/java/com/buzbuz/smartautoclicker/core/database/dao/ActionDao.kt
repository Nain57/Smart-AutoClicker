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
package com.buzbuz.smartautoclicker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
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
    @Query("SELECT * FROM action_table ORDER BY name")
    abstract fun getAllActions(): Flow<List<CompleteActionEntity>>

    /**
     * Get the list of complete actions for an event, ordered by priority.
     *
     * @param eventId the identifier of the event to get the actions from.
     */
    @Transaction
    @Query("SELECT * FROM action_table WHERE eventId=:eventId ORDER BY priority")
    abstract suspend fun getCompleteActions(eventId: Long): List<CompleteActionEntity>

    /**
     * Get the list of intent extras for a given action.
     *
     * @param actionId the identifier of the action to get the intent extras from.
     * @return the list of intent extras for the action.
     */
    @Query("SELECT * FROM intent_extra_table WHERE action_id=:actionId ORDER BY id")
    abstract suspend fun getIntentExtras(actionId: Long): List<IntentExtraEntity>

    /**
     * Add an action to the database.
     * @param action the action to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addAction(action: ActionEntity): Long

    /**
     * Add an intent extras to the database.
     * @param intentExtra the intent extra to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addIntentExtra(intentExtra: IntentExtraEntity)

    /**
     * Update an action in the database.
     * @param action the action to be updated.
     */
    @Update
    abstract suspend fun updateAction(action: ActionEntity)

    /**
     * Update an intent extra in the database.
     * @param extra the intent extra to be updated.
     */
    @Update
    abstract suspend fun updateIntentExtra(extra: IntentExtraEntity)

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
}
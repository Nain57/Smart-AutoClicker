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
package com.buzbuz.smartautoclicker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access the conditions in the database. */
@Dao
abstract class ConditionDao {

    /**
     * Get all conditions from all events.
     *
     * @return the list containing all conditions.
     */
    @Query("SELECT * FROM condition_table")
    abstract fun getAllConditions(): Flow<List<ConditionEntity>>

    /**
     * Get the list of conditions for a given event.
     *
     * @param eventId the identifier of the event to get the conditions from.
     * @return the list of conditions for the event.
     */
    @Query("SELECT * FROM condition_table WHERE eventId=:eventId ORDER BY id")
    abstract suspend fun getConditions(eventId: Long): List<ConditionEntity>

    /**
     * Get the list of conditions path for a given event.
     *
     * @param eventId the identifier of the event to get the conditions path from.
     * @return the list of path for the event.
     */
    @Query("SELECT path FROM condition_table WHERE eventId=:eventId")
    abstract suspend fun getConditionsPath(eventId: Long): List<String>

    /**
     * Get the number of times this path is used in the condition table.
     *
     * @param path the value to be searched in the path column.
     * @return the number of conditions using this path.
     */
    @Query("SELECT COUNT(path) FROM condition_table WHERE path=:path")
    abstract suspend fun getValidPathCount(path: String): Int

    /**
     * Add conditions to the database.
     * @param conditions the conditions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addConditions(conditions: List<ConditionEntity>): List<Long>

    /**
     * Update a condition in the database.
     * @param conditions the condition to be updated.
     */
    @Update
    abstract suspend fun updateConditions(conditions: List<ConditionEntity>)

    /**
     * Delete a list of conditions in the database.
     * @param conditions the conditions to be removed.
     */
    @Delete
    abstract suspend fun deleteConditions(conditions: List<ConditionEntity>)
}
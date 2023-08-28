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

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.core.database.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionWithEvent

/** Allows to access and edit the end conditions in the database. */
@Dao
abstract class EndConditionDao {

    /**
     * Get all end conditions for a given scenario.
     *
     * @param scenarioId the unique identifier of the scenario.
     */
    @Transaction
    @Query("SELECT * FROM end_condition_table WHERE scenario_id=:scenarioId")
    abstract suspend fun getEndConditions(scenarioId: Long): List<EndConditionEntity>

    /**
     * Get all end conditions and their associated event for a given scenario.
     *
     * @param scenarioId the unique identifier of the scenario.
     */
    @Transaction
    @Query("SELECT * FROM end_condition_table WHERE scenario_id=:scenarioId")
    abstract suspend fun getEndConditionsWithEvent(scenarioId: Long): List<EndConditionWithEvent>

    /**
     * Add a new end condition to the database.
     *
     * @param endConditionEntities the list of end condition to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addEndConditions(endConditionEntities: List<EndConditionEntity>)

    /**
     * Update the selected end condition.
     *
     * @param endConditionEntities the list of end condition to be updated.
     */
    @Update
    abstract suspend fun updateEndConditions(endConditionEntities: List<EndConditionEntity>)

    /**
     * Delete the provided end condition from the database.
     *
     * @param endConditionEntities the list of end condition to be deleted.
     */
    @Delete
    abstract suspend fun deleteEndConditions(endConditionEntities: List<EndConditionEntity>)
}
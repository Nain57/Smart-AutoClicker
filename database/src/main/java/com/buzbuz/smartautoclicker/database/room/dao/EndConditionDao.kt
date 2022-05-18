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
package com.buzbuz.smartautoclicker.database.room.dao

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity

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
    abstract fun getEndConditions(scenarioId: Long): List<EndConditionEntity>

    /**
     * Update an end condition in the database.
     * Execute the add, update and remove end conditions operations.
     *
     * @param scenarioId the unique identifier of the scenario for the updated end conditions.
     * @param endConditions the new end conditions for this scenario.
     */
    @Transaction
    open suspend fun updateEndConditions(scenarioId: Long, endConditions: List<EndConditionEntity>) {
        EndConditionsUpdater.let {
            it.refreshUpdateValues(getEndConditions(scenarioId), endConditions)
            add(it.toBeAdded)
            update(it.toBeUpdated)
            delete(it.toBeRemoved)
        }
    }

    /**
     * Add a new end condition to the database.
     *
     * @param endConditionEntities the list of end condition to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun add(endConditionEntities: List<EndConditionEntity>)

    /**
     * Update the selected end condition.
     *
     * @param endConditionEntities the list of end condition to be updated.
     */
    @Update
    @VisibleForTesting
    abstract suspend fun update(endConditionEntities: List<EndConditionEntity>)

    /**
     * Delete the provided end condition from the database.
     *
     * @param endConditionEntities the list of end condition to be deleted.
     */
    @Delete
    @VisibleForTesting
    abstract suspend fun delete(endConditionEntities: List<EndConditionEntity>)
}
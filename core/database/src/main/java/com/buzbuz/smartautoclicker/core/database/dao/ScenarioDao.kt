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
import androidx.room.Transaction
import androidx.room.Update

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEndConditions
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEvents

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the scenario in the database. */
@Dao
interface ScenarioDao {

    /**
     * Get all events scenario and their events.
     *
     * @return the live data on the list of scenarios.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table ORDER BY name ASC")
    fun getScenariosWithEvents(): Flow<List<ScenarioWithEvents>>

    /**
     * Get a scenario and its end conditions.
     *
     * @return the live data on the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId")
    fun getScenarioWithEndConditions(scenarioId: Long): Flow<ScenarioWithEndConditions?>

    /**
     * Get a scenario
     *
     * @return the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId")
    suspend fun getScenario(scenarioId: Long): ScenarioEntity?

    /**
     * Get a complete scenario
     *
     * @return the scenario.
     */
    @Transaction
    @Query("SELECT * FROM scenario_table WHERE id=:scenarioId")
    suspend fun getCompleteScenario(scenarioId: Long): CompleteScenario?

    /**
     * Add a new scenario to the database.
     *
     * @param scenarioEntity the scenario to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(scenarioEntity: ScenarioEntity): Long

    /**
     * Update the selected scenario.
     *
     * @param scenarioEntity the scenario to be updated.
     */
    @Update
    suspend fun update(scenarioEntity: ScenarioEntity)

    /**
     * Delete the provided click scenario from the database.
     *
     * Any associated entity will be removed from the database due to the [androidx.room.ForeignKey.CASCADE]
     * deletion of this parent scenario.
     *
     * @param scenarioId the identifier of the scenario to be deleted.
     */
    @Query("DELETE FROM scenario_table WHERE id = :scenarioId")
    suspend fun delete(scenarioId: Long)
}
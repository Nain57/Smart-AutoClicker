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
package com.buzbuz.smartautoclicker.core.dumb.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the dumb scenario in the database. */
@Dao
interface DumbScenarioDao {

    /**
     * Get all dumb scenario and their dumb actions.
     *
     * @return the Flow on the list of scenarios.
     */
    @Transaction
    @Query("SELECT * FROM dumb_scenario_table ORDER BY name ASC")
    fun getDumbScenariosWithActionsFlow(): Flow<List<DumbScenarioWithActions>>

    /**
     * Get the specified dumb scenario with its dumb actions.
     *
     * @return the dumb scenario if found, null if not.
     */
    @Transaction
    @Query("SELECT * FROM dumb_scenario_table WHERE id=:dbId")
    suspend fun getDumbScenariosWithAction(dbId: Long): DumbScenarioWithActions?

    /**
     * Get the specified dumb scenario with its dumb actions.
     *
     * @return the dumb scenario if found, null if not.
     */
    @Transaction
    @Query("SELECT * FROM dumb_scenario_table WHERE id=:dbId")
    fun getDumbScenariosWithActionFlow(dbId: Long): Flow<DumbScenarioWithActions?>

    /** Get the dumb actions and their relations for all scenarios except the provided one. */
    @Transaction
    @Query("SELECT * FROM dumb_action_table WHERE dumb_scenario_id!=:dumbScenarioId")
    fun getAllDumbActionsExcept(dumbScenarioId: Long): Flow<List<DumbActionWithFingers>>

    /** Get the dumb actions for a scenario, ordered by their priority. */
    @Query("SELECT * FROM dumb_action_table WHERE dumb_scenario_id=:dumbScenarioId ORDER BY priority ASC")
    fun getDumbActions(dumbScenarioId: Long): List<DumbActionEntity>

    /**
     * Add a new scenario to the database.
     *
     * @param dumbScenario the dumb scenario to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDumbScenario(dumbScenario: DumbScenarioEntity): Long

    /**
     * Update a dumb scenario to the database.
     *
     * @param dumbScenario the dumb scenario to be added.
     */
    @Update
    suspend fun updateDumbScenario(dumbScenario: DumbScenarioEntity)

    /**
     * Delete the provided click scenario from the database.
     *
     * @param dumbScenarioId the identifier of the scenario to be deleted.
     */
    @Query("DELETE FROM dumb_scenario_table WHERE id = :dumbScenarioId")
    suspend fun deleteDumbScenario(dumbScenarioId: Long)

    /**
     * Add new dub actions to the database.
     *
     * @param dumbActions the dumb actions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDumbActions(dumbActions: List<DumbActionEntity>): List<Long>

    /**
     * Update the selected dumb actions.
     *
     * @param dumbActions the dumb actions to be updated.
     */
    @Update
    suspend fun updateDumbActions(dumbActions: List<DumbActionEntity>)

    /**
     * Delete the selected dumb actions.
     *
     * @param dumbActions the dumb actions to be deleted.
     */
    @Delete
    suspend fun deleteDumbActions(dumbActions: List<DumbActionEntity>)

    /**
     * Delete all gesture fingers for a given dumb action. Cascades to their motions.
     *
     * @param dumbActionId the identifier of the gesture action.
     */
    @Query("DELETE FROM $DUMB_GESTURE_FINGER_TABLE WHERE dumb_action_id=:dumbActionId")
    suspend fun deleteGestureFingersByActionId(dumbActionId: Long)

    /**
     * Add a gesture finger to the database.
     *
     * @param finger the finger to be added.
     * @return the database id of the inserted finger.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGestureFinger(finger: DumbGestureFingerEntity): Long

    /**
     * Add a list of gesture finger motions to the database.
     *
     * @param motions the motions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGestureFingerMotions(motions: List<DumbGestureFingerMotionEntity>): List<Long>

    /**
     * Get a scenario stats
     *
     * @return the scenario stats.
     */
    @Query("SELECT * FROM dumb_scenario_stats_table WHERE dumb_scenario_id=:scenarioId")
    suspend fun getScenarioStats(scenarioId: Long): DumbScenarioStatsEntity?

    /**
     * Add the stats for a scenario.
     *
     * @param stats the stats to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addScenarioStats(stats: DumbScenarioStatsEntity)

    /**
     * Update the stats for a scenario.
     *
     * @param stats the stats to be updated.
     */
    @Update
    suspend fun updateScenarioStats(stats: DumbScenarioStatsEntity)
}
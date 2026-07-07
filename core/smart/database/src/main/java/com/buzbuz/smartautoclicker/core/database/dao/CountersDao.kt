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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE
import com.buzbuz.smartautoclicker.core.database.entity.CountersEntity
import kotlinx.coroutines.flow.Flow

/** Allows to access the counters in the database. */
@Dao
interface CountersDao {

    @Query("SELECT * FROM $COUNTERS_TABLE WHERE scenarioId=:scenarioId")
    fun getScenarioCountersFlow(scenarioId: Long): Flow<List<CountersEntity>>

    @Query("SELECT * FROM $COUNTERS_TABLE WHERE scenarioId=:scenarioId")
    suspend fun getScenarioCounters(scenarioId: Long): List<CountersEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: CountersEntity)

    @Query("DELETE FROM $COUNTERS_TABLE WHERE counterName = :counterName AND scenarioId = :scenarioId")
    suspend fun deleteCounter(counterName: String, scenarioId: Long)
}
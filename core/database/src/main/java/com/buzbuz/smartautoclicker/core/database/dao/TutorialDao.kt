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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

import com.buzbuz.smartautoclicker.core.database.entity.TutorialSuccessEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the tutorial info in the database. */
@Dao
interface TutorialDao {

    /**
     * Add/Update a new tutorial success to the database.
     * @param entity the tutorial success to be added.
     */
    @Upsert(TutorialSuccessEntity::class)
    suspend fun upsert(entity: TutorialSuccessEntity)

    /** @return all tutorials successes, ordered by their index. */
    @Query("SELECT * FROM tutorial_success_table ORDER BY tutorial_index ASC")
    fun getTutorialSuccessList(): Flow<List<TutorialSuccessEntity>>

    /** @return the success for a tutorial, or null if none was found. */
    @Query("SELECT * FROM tutorial_success_table WHERE tutorial_index=:index")
    suspend fun getTutorialSuccess(index: Int): TutorialSuccessEntity?

    /** @return the scenario id for a tutorial if a success for it was found, null if not. */
    @Query("SELECT scenario_id FROM tutorial_success_table WHERE tutorial_index=:index")
    suspend fun getTutorialScenarioId(index: Int): Long?
}
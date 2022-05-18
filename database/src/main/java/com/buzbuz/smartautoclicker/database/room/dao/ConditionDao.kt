/*
 * Copyright (C) 2021 Nain57
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

import androidx.room.Dao
import androidx.room.Query

import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import kotlinx.coroutines.flow.Flow

/** Allows to access the conditions in the database. */
@Dao
interface ConditionDao {

    /**
     * Get number of conditions in the database.
     *
     * @return the flow on the number of conditions.
     */
    @Query("SELECT COUNT(*) FROM condition_table")
    fun getConditionsCount(): Flow<Int>

    /**
     * Get all conditions from all events.
     *
     * @return the list containing all conditions.
     */
    @Query("SELECT * FROM condition_table")
    fun getAllConditions(): Flow<List<ConditionEntity>>

    /**
     * Get the list of conditions path for a given event.
     *
     * @param eventId the identifier of the event to get the conditions path from.
     * @return the list of path for the event.
     */
    @Query("SELECT path FROM condition_table WHERE eventId=:eventId")
    suspend fun getConditionsPath(eventId: Long): List<String>

    /**
     * Get the number of times this path is used in the condition table.
     *
     * @param path the value to be searched in the path column.
     * @return the number of conditions using this path.
     */
    @Query("SELECT COUNT(path) FROM condition_table WHERE path=:path")
    suspend fun getValidPathCount(path: String): Int
}
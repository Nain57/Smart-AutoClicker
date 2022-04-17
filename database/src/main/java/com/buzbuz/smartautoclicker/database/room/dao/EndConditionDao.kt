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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity

/** Allows to access and edit the end conditions in the database. */
@Dao
internal interface EndConditionDao {

    /**
     * Add a new end condition to the database.
     *
     * @param endConditionEntity the end condition to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(endConditionEntity: EndConditionEntity): Long

    /**
     * Update the selected end condition.
     *
     * @param endConditionEntity the end condition to be updated.
     */
    @Update
    suspend fun update(endConditionEntity: EndConditionEntity)

    /**
     * Delete the provided end condition from the database.
     *
     * @param endConditionEntity the end condition to be deleted.
     */
    @Delete
    suspend fun delete(endConditionEntity: EndConditionEntity)
}
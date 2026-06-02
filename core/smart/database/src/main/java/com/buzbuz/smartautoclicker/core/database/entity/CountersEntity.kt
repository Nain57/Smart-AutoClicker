/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.buzbuz.smartautoclicker.core.database.COUNTERS_TABLE
import kotlinx.serialization.Serializable


@Entity(
    tableName = COUNTERS_TABLE,
    primaryKeys = ["counterName", "scenarioId"],
    indices = [Index("scenarioId")],
    foreignKeys = [ForeignKey(
        entity = ScenarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["scenarioId"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class CountersEntity(
    @ColumnInfo(name= "counterName") val name: String,
    @ColumnInfo(name = "scenarioId") val scenarioId: Long,
    @ColumnInfo(name = "startingValue") val startingValue: Double,
)
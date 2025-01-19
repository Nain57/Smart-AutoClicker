/*
 * Copyright (C) 2025 Kevin Buzeau
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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import kotlinx.serialization.Serializable

/**
 * Entity defining the usage of a dumb scenario.
 *
 * A scenario has a relation "one to one" with [DumbScenarioEntity].
 *
 * @param scenarioId the unique identifier for a scenario.
 * @param lastStartTimestampMs timestamp of the last start for this scenario.
 * @param startCount the number of time this scenario has been started.
 */
@Entity(
    tableName = "dumb_scenario_stats_table",
    indices = [Index("dumb_scenario_id")],
    foreignKeys = [ForeignKey(
        entity = DumbScenarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["dumb_scenario_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class DumbScenarioStatsEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "dumb_scenario_id") val scenarioId: Long,
    @ColumnInfo(name = "last_start_timestamp_ms") val lastStartTimestampMs: Long,
    @ColumnInfo(name = "start_count") val startCount: Long,
) : EntityWithId
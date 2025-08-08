
package com.buzbuz.smartautoclicker.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.SCENARIO_USAGE_TABLE


/**
 * Entity defining the usage of a scenario.
 *
 * A scenario has a relation "one to one" with [ScenarioEntity].
 *
 * @param scenarioId the unique identifier for a scenario.
 * @param lastStartTimestampMs timestamp of the last start for this scenario.
 * @param startCount the number of time this scenario has been started.
 */
@Entity(
    tableName = SCENARIO_USAGE_TABLE,
    indices = [Index("scenario_id")],
    foreignKeys = [ForeignKey(
        entity = ScenarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["scenario_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScenarioStatsEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long,
    @ColumnInfo(name = "scenario_id") val scenarioId: Long,
    @ColumnInfo(name = "last_start_timestamp_ms") val lastStartTimestampMs: Long,
    @ColumnInfo(name = "start_count") val startCount: Long,
) : EntityWithId
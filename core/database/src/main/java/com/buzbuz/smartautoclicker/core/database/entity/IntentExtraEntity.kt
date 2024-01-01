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
package com.buzbuz.smartautoclicker.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.INTENT_EXTRA_TABLE

import kotlinx.serialization.Serializable

/**
 * Entity defining an intent extra.
 *
 * An intent extra is linked with an action of type [ActionType.INTENT] and contains one of the extra to put in the
 * [android.content.Intent] created when the action is executed.
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param actionId unique identifier of this extra's action. Reference the key [ActionEntity.id] in scenario_table.
 * @param type the type of the value for this extra.
 * @param key the key of the extra.
 * @param value the value for the extra. This is a string representation of the value with the type [type].
 */
@Entity(
    tableName = INTENT_EXTRA_TABLE,
    indices = [Index("action_id")],
    foreignKeys = [ForeignKey(
        entity = ActionEntity::class,
        parentColumns = ["id"],
        childColumns = ["action_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class IntentExtraEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long,
    @ColumnInfo(name = "action_id") var actionId: Long,
    @ColumnInfo(name = "type") val type: IntentExtraType,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
) : EntityWithId

/** The list of supported types for the value of an [IntentExtraEntity]. */
enum class IntentExtraType {
    BOOLEAN,
    BYTE,
    CHAR,
    DOUBLE,
    INTEGER,
    FLOAT,
    SHORT,
    STRING
}

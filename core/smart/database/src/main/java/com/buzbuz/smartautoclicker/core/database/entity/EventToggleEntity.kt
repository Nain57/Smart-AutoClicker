
package com.buzbuz.smartautoclicker.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.EVENT_TOGGLE_TABLE

import kotlinx.serialization.Serializable


/**
 * Entity defining an event toggle.
 *
 * An intent extra is linked with an action of type [ActionType.TOGGLE_EVENT] and contains the toggle to apply to an event.
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param actionId unique identifier the action containing this toggle. Reference the key [ActionEntity.id] in action_table.
 * @param type the type of manipulation to apply to the event.
 * @param eventId unique identifier of the event to apply the toggle to. Reference the key [EventEntity.id] in event_table.
 */
@Entity(
    tableName = EVENT_TOGGLE_TABLE,
    indices = [Index("action_id"), Index("toggle_event_id")],
    foreignKeys = [
        ForeignKey(
            entity = ActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["action_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["toggle_event_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
@Serializable
data class EventToggleEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long,
    @ColumnInfo(name = "action_id") var actionId: Long,
    @ColumnInfo(name = "toggle_type") val type: EventToggleType,
    @ColumnInfo(name = "toggle_event_id") var toggleEventId: Long,
) : EntityWithId


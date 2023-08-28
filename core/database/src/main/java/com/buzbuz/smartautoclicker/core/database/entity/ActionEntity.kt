/*
 * Copyright (C) 2022 Kevin Buzeau
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

import androidx.room.*
import kotlinx.serialization.Serializable

/**
 * Entity defining an action from an event.
 *
 * An action can have several types, defined by [ActionType] and contained in [ActionEntity.type] as a String. Depending
 * on this type, this entity will have several columns corresponding to that type with their values set, and the other
 * values for all others will have their values set to null.
 *
 * It has a one to many relation with [EventEntity], meaning that one event can have several actions. If the event is
 * deleted, this action will be deleted as well.
 *
 * @param id unique identifier for an action. Also the primary key in the table.
 * @param eventId identifier of this action's event. Reference the key [EventEntity.id] in event_table.
 * @param priority the order in the action list. Lowest priority will always be executed first.
 * @param name the name of this action
 * @param type type of this action. Must be the this representation of the [ActionType] enum.
 *
 * @param clickPositionType [ActionType.CLICK] only: indicates how the click position is interpreted.
 *                          If USER_SELECTED, [x] and [y] will be used.
 *                          If ON_DETECTED_CONDITION, the [clickOnConditionId] will be used.
 * @param x [ActionType.CLICK] only: the x position of the click. Null for others [ActionType].
 * @param y [ActionType.CLICK] only: the y position of the click. Null for others [ActionType].
 * @param clickOnConditionId [ActionType.CLICK] only: if defined, the condition to click on.
 *                         If null, the x and y coordinates will be used.
 * @param pressDuration [ActionType.CLICK] only: the duration of the click press in milliseconds.
 *                      Null for others [ActionType].
 *
 * @param fromX [ActionType.SWIPE] only: the swipe start x coordinates. Null for others [ActionType].
 * @param fromY [ActionType.SWIPE] only: the swipe start y coordinates. Null for others [ActionType].
 * @param toX [ActionType.SWIPE] only: the swipe end x coordinates. Null for others [ActionType].
 * @param toY [ActionType.SWIPE] only: the swipe end y coordinates. Null for others [ActionType].
 * @param swipeDuration [ActionType.SWIPE] only: the delay between the swipe start and end in milliseconds.
 *                      Null for others [ActionType].
 *
 * @param pauseDuration [ActionType.PAUSE] only: the duration of the pause in milliseconds.
 *
 * @param isAdvanced [ActionType.INTENT] only: true if the user have picked advanced intent config, false if simple.
 * @param isBroadcast [ActionType.INTENT] only: true the this intent should be broadcast, false for a start activity.
 * @param intentAction [ActionType.INTENT] only: action for the intent.
 * @param componentName [ActionType.INTENT] only: the component to send the intent to. Null for if [isBroadcast] is true.
 * @param flags [ActionType.INTENT] only: flags for the intent as defined in [android.content.Intent].
 *
 * @param toggleEventId [ActionType.TOGGLE_EVENT] only: the id of the event to be manipulated.
 * @param toggleEventType [ActionType.TOGGLE_EVENT] only: the type of toggle for the event.
 *                        Must be one of [ToggleEventType].
 */
@Entity(
    tableName = "action_table",
    indices = [Index("eventId"), Index("clickOnConditionId"), Index("toggle_event_id")],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ConditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["clickOnConditionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["toggle_event_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ]
)
@Serializable
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "eventId") var eventId: Long,
    @ColumnInfo(name = "priority") var priority: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: ActionType,

    // ActionType.CLICK
    @ColumnInfo(name = "clickPositionType") val clickPositionType: ClickPositionType? = null,
    @ColumnInfo(name = "x") val x: Int? = null,
    @ColumnInfo(name = "y") val y: Int? = null,
    @ColumnInfo(name = "clickOnConditionId") var clickOnConditionId: Long? = null,
    @ColumnInfo(name = "pressDuration") val pressDuration: Long? = null,

    // ActionType.SWIPE
    @ColumnInfo(name = "fromX") val fromX: Int? = null,
    @ColumnInfo(name = "fromY") val fromY: Int? = null,
    @ColumnInfo(name = "toX") val toX: Int? = null,
    @ColumnInfo(name = "toY") val toY: Int? = null,
    @ColumnInfo(name = "swipeDuration") val swipeDuration: Long? = null,

    // ActionType.PAUSE
    @ColumnInfo(name = "pauseDuration") val pauseDuration: Long? = null,

    // ActionType.INTENT
    @ColumnInfo(name = "isAdvanced") val isAdvanced: Boolean? = null,
    @ColumnInfo(name = "isBroadcast") val isBroadcast: Boolean ? = null,
    @ColumnInfo(name = "intent_action") val intentAction: String? = null,
    @ColumnInfo(name = "component_name") val componentName: String ? = null,
    @ColumnInfo(name = "flags") val flags: Int? = null,

    // ActionType.TOGGLE_EVENT
    @ColumnInfo(name = "toggle_event_id") var toggleEventId: Long? = null,
    @ColumnInfo(name = "toggle_type") val toggleEventType: ToggleEventType? = null,
)

/**
 * Type of [ActionEntity].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [ActionEntity] documentation for values/type association.
 *
 * /!\ DO NOT RENAME: ActionType enum name is used in the database.
 */
enum class ActionType {
    /** A single tap on the screen. */
    CLICK,
    /** A swipe on the screen. */
    SWIPE,
    /** A pause, waiting before the next action. */
    PAUSE,
    /** An Android Intent, allowing to interact with other applications. */
    INTENT,
    /** Toggle the enabled state of an event. */
    TOGGLE_EVENT,
}

/** Type converter to read/write the [ActionType] into the database. */
internal class ActionTypeStringConverter {
    @TypeConverter
    fun fromString(value: String): ActionType = ActionType.valueOf(value)
    @TypeConverter
    fun toString(action: ActionType): String = action.toString()
}

/**
 * Type of click position for a [ActionType.CLICK].
 * Indicates how to click on the screen for the action.
 *
 * /!\ DO NOT RENAME: ClickPositionType enum name is used in the database.
 */
enum class ClickPositionType {
    /** The user must manually select a position to be clicked. */
    USER_SELECTED,
    /**
     * Click on the detected condition.
     * When the condition operator is AND, click on the condition specified by the user.
     * When the condition operator is OR, click on the condition detected condition.
     */
    ON_DETECTED_CONDITION,
}

/** Type converter to read/write the [ClickPositionType] into the database. */
internal class ClickPositionTypeStringConverter {
    @TypeConverter
    fun fromString(value: String?): ClickPositionType? = value?.let { ClickPositionType.valueOf(it) }
    @TypeConverter
    fun toString(type: ClickPositionType?): String? = type?.toString()
}

/**
 * The type of manipulation to apply to an event with a [ActionType.TOGGLE_EVENT].
 *
 * /!\ DO NOT RENAME: ToggleEventType enum name is used in the database.
 */
enum class ToggleEventType {
    /** Enable the event. Has no effect if the event is already enabled. */
    ENABLE,
    /** Disable the event. Has no effect if the event is already disabled. */
    DISABLE,
    /** Enable the event if it is disabled, disable it if it is enabled. */
    TOGGLE,
}

/** Type converter to read/write the [ToggleEventType] into the database. */
internal class ToggleEventTypeStringConverter {
    @TypeConverter
    fun fromString(value: String?): ToggleEventType? = value?.let { ToggleEventType.valueOf(it) }
    @TypeConverter
    fun toString(type: ToggleEventType?): String? = type?.toString()
}

/**
 * Entity embedding an intent action and its extras.
 *
 * Automatically do the junction between action_table and intent_extra_table, and provide this
 * representation of the one to many relations between scenario to actions and conditions entities.
 *
 * @param action
 * @param intentExtras
 */
@Serializable
data class CompleteActionEntity(
    @Embedded val action: ActionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "action_id"
    )
    val intentExtras: List<IntentExtraEntity>,
)
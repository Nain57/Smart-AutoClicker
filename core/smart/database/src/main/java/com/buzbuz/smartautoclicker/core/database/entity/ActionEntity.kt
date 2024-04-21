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

import androidx.room.*

import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.database.ACTION_TABLE

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
 * @param counterName [ActionType.CHANGE_COUNTER] only: the name of the counter to change apply the
 *                     operation on. There is no need for a db object for a "counter", we only need
 *                     to identify them at runtime using their names. Null for others [ActionType].
 * @param counterOperation [ActionType.CHANGE_COUNTER] only: the type of operation to apply to the
 *                                                     counter. Null for others [ActionType].
 * @param counterOperationValue [ActionType.CHANGE_COUNTER] only: the vale to use for the operation
 *                                                          on the counter. Null for others [ActionType].
 */
@Entity(
    tableName = ACTION_TABLE,
    indices = [Index("eventId"), Index("clickOnConditionId")],
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
    ]
)
@Serializable
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long,
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
    @ColumnInfo(name = "toggle_all") var toggleAll: Boolean? = null,
    @ColumnInfo(name = "toggle_all_type") val toggleAllType: EventToggleType? = null,

    // ActionType.CHANGE_COUNTER
    @ColumnInfo(name = "counter_name") var counterName: String? = null,
    @ColumnInfo(name = "counter_operation") val counterOperation: ChangeCounterOperationType? = null,
    @ColumnInfo(name = "counter_operation_value") val counterOperationValue: Int? = null,
) : EntityWithId

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
    /** Change the value of a counter. */
    CHANGE_COUNTER,
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

/**
 * Types of counter change of a [ActionType.CHANGE_COUNTER].
 * /!\ DO NOT RENAME: ChangeCounterOperationType enum name is used in the database.
 */
enum class ChangeCounterOperationType {
    /** Add to the current counter value. */
    ADD,
    /** Remove from the current counter value. */
    MINUS,
    /** Set the counter to a specific value. */
    SET;
}

/**
 * Entity embedding an intent action and its extras.
 *
 * Automatically do the junction between action_table and intent_extra_table, and provide this
 * representation of the one to many relations between scenario to actions and conditions entities.
 */
@Serializable
data class CompleteActionEntity(
    @Embedded val action: ActionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "action_id"
    )
    val intentExtras: List<IntentExtraEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "action_id"
    )
    val eventsToggle: List<EventToggleEntity>,
)
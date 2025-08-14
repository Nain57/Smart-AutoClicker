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
 * @param clickPositionType [ActionType.CLICK] only: indicates how the click position is interpreted. If USER_SELECTED,
 *  [x] and [y] will be used. If ON_DETECTED_CONDITION, the [clickOnConditionId] will be used.
 * @param x [ActionType.CLICK] only: the x position of the click. Null for others [ActionType].
 * @param y [ActionType.CLICK] only: the y position of the click. Null for others [ActionType].
 * @param clickOnConditionId [ActionType.CLICK] only: if defined, the condition to click on. If null, the x and y
 *  coordinates will be used.
 * @param pressDuration [ActionType.CLICK] only: the duration of the click press in milliseconds.
 *  Null for others [ActionType].
 * @param clickOffsetX [ActionType.CLICK] & [ClickPositionType.ON_DETECTED_CONDITION] only: the offset to apply in the X
 *  axis when clicking on a condition. Null for others [ActionType].
 * @param clickOffsetY [ActionType.CLICK] & [ClickPositionType.ON_DETECTED_CONDITION] only: the offset to apply in the Y
 *  axis when clicking on a condition.
 *
 * @param fromX [ActionType.SWIPE] only: the swipe start x coordinates. Null for others [ActionType].
 * @param fromY [ActionType.SWIPE] only: the swipe start y coordinates. Null for others [ActionType].
 * @param toX [ActionType.SWIPE] only: the swipe end x coordinates. Null for others [ActionType].
 * @param toY [ActionType.SWIPE] only: the swipe end y coordinates. Null for others [ActionType].
 * @param swipeDuration [ActionType.SWIPE] only: the delay between the swipe start and end in milliseconds.
 *  Null for others [ActionType].
 *
 * @param pauseDuration [ActionType.PAUSE] only: the duration of the pause in milliseconds.
 *
 * @param isAdvanced [ActionType.INTENT] only: true if the user have picked advanced intent config, false if simple.
 * @param isBroadcast [ActionType.INTENT] only: true the this intent should be broadcast, false for a start activity.
 * @param intentAction [ActionType.INTENT] only: action for the intent.
 * @param componentName [ActionType.INTENT] only: the component to send the intent to. Null for if [isBroadcast] is true.
 * @param flags [ActionType.INTENT] only: flags for the intent as defined in [android.content.Intent].
 *
 * @param counterName [ActionType.CHANGE_COUNTER] only: the name of the counter to change apply the operation on. There
 *  is no need for a db object for a "counter", we only need to identify them at runtime using their names.
 *  Null for others [ActionType].
 * @param counterOperation [ActionType.CHANGE_COUNTER] only: the type of operation to apply to the
 *  counter. Null for others [ActionType].
 * @param counterOperationValueType [ActionType.CHANGE_COUNTER] only: the type of value.
 * If [CounterOperationValueType.NUMBER], the param [counterOperationValue] will be used for the operation.
 * If [CounterOperationValueType.COUNTER], the value contained by the counter defined by the param
 * [counterOperationCounterName] will be used for the operation.
 * @param counterOperationValue [ActionType.CHANGE_COUNTER] & [CounterOperationValueType.NUMBER] only: the value to use
 * for the operation on this counter. Null for others [ActionType].
 * @param counterOperationCounterName [ActionType.CHANGE_COUNTER] & [CounterOperationValueType.COUNTER] only: the name
 * of the counter containing the value for the operation on this counter. Null for others [ActionType].
 *
 * @param notificationMessageType [ActionType.NOTIFICATION] only: tells what kind of message the notification will contains.
 * @param notificationMessageText [ActionType.NOTIFICATION] only: used as notification message when [notificationMessageType]
 *  is [NotificationMessageType.TEXT].
 * @param notificationMessageCounterName [ActionType.NOTIFICATION] only: the counter value will be used as notification
 *  message when [notificationMessageType] is [NotificationMessageType.COUNTER_VALUE].
 * @param notificationImportance [ActionType.NOTIFICATION] only: how the notification will behave.
 *
 * @param systemActionType [ActionType.SYSTEM] only: the type of system action to execute.
 *
 * @param textValue [ActionType.TEXT] only: the text to type in the focused view
 * @param textValidateInput [ActionType.TEXT] only: the type of system action to execute.
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
    @ColumnInfo(name = "clickOffsetX") val clickOffsetX: Int? = null,
    @ColumnInfo(name = "clickOffsetY") val clickOffsetY: Int? = null,

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
    @ColumnInfo(name = "counter_operation_value_type") val counterOperationValueType: CounterOperationValueType? = null,
    @ColumnInfo(name = "counter_operation_value") val counterOperationValue: Int? = null,
    @ColumnInfo(name = "counter_operation_counter_name") val counterOperationCounterName: String? = null,

    // ActionType.NOTIFICATION
    @ColumnInfo(name = "notification_message_type") val notificationMessageType: NotificationMessageType? = null,
    @ColumnInfo(name = "notification_message_text") val notificationMessageText: String? = null,
    @ColumnInfo(name = "notification_message_counter_name") val notificationMessageCounterName: String? = null,
    @ColumnInfo(name = "notification_importance") var notificationImportance: Int? = null,

    // ActionType.SYSTEM
    @ColumnInfo(name = "system_action_type") val systemActionType: SystemActionType? = null,

    // ActionType.TEXT
    @ColumnInfo(name = "text_value") val textValue: String? = null,
    @ColumnInfo(name = "text_validate_input") val textValidateInput: Boolean? = null,
) : EntityWithId

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
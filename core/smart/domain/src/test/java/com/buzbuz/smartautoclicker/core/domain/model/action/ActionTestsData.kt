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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName
import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerWithMotions
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraType
import com.buzbuz.smartautoclicker.core.database.entity.SystemActionType
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue.Literal
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.event.EventTestsData
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

internal object ActionTestsData {

    const val ACTION_EVENT_ID = EventTestsData.EVENT_ID

    /* ------- Click Action Data ------- */

    private const val CLICK_ID = 7L
    private const val CLICK_NAME = "Click name"
    private const val CLICK_PRESS_DURATION = 250L
    private const val CLICK_X_POSITION = 24
    private const val CLICK_Y_POSITION = 87
    const val CLICK_FINGER_ID = 70L
    private const val CLICK_PRESS_MOTION_ID = 701L
    private const val CLICK_WAIT_MOTION_ID = 702L
    private const val CLICK_RELEASE_MOTION_ID = 703L

    fun getNewClickEntity(
        id: Long = CLICK_ID,
        name: String = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        motions: List<GestureFingerMotionEntity> = getNewClickMotionEntities(
            pressDuration = pressDuration,
            x = x,
            y = y,
            clickOnConditionId = clickOnConditionId,
        ),
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.CLICK),
        gestureFingers = listOf(
            GestureFingerWithMotions(
                finger = GestureFingerEntity(CLICK_FINGER_ID, id, 0, 0),
                motions = motions,
            ),
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    /** A click is stored as a press on the click position, a wait for the press duration, and a release. */
    fun getNewClickMotionEntities(
        fingerId: Long = CLICK_FINGER_ID,
        pressMotionId: Long = CLICK_PRESS_MOTION_ID,
        waitMotionId: Long = CLICK_WAIT_MOTION_ID,
        releaseMotionId: Long = CLICK_RELEASE_MOTION_ID,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        offsetX: Int? = null,
        offsetY: Int? = null,
    ): List<GestureFingerMotionEntity> {
        val isOnCondition = x == null && y == null

        return listOf(
            GestureFingerMotionEntity(
                id = pressMotionId,
                fingerId = fingerId,
                index = 0,
                motionType = GestureFingerMotionType.PRESS,
                xCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else CoordinatesType.LITERAL,
                yCoordinateType = if (isOnCondition) CoordinatesType.CONDITION else CoordinatesType.LITERAL,
                conditionId = clickOnConditionId,
                xLiteral = x,
                yLiteral = y,
                xOffsetCoordinateType = offsetX?.let { CoordinatesType.LITERAL },
                yOffsetCoordinateType = offsetY?.let { CoordinatesType.LITERAL },
                xOffsetLiteral = offsetX,
                yOffsetLiteral = offsetY,
            ),
            GestureFingerMotionEntity(
                id = waitMotionId,
                fingerId = fingerId,
                index = 1,
                motionType = GestureFingerMotionType.WAIT,
                durationMs = pressDuration,
            ),
            GestureFingerMotionEntity(
                id = releaseMotionId,
                fingerId = fingerId,
                index = 2,
                motionType = GestureFingerMotionType.RELEASE,
            ),
        )
    }

    fun getNewClick(
        id: Long = CLICK_ID,
        name: String? = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long? = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        position: Position? =
            if (x != null && y != null) Position.Coordinates(Literal(x), Literal(y))
            else Position.Condition(clickOnConditionId?.let { Identifier(databaseId = it) }),
        offset: Position? = null,
        eventId: Long,
    ) = Click(id.asIdentifier(), eventId.asIdentifier(), name, priority, pressDuration, position, offset)


    /* ------- Gesture Action Data ------- */

    private const val GESTURE_ID = 8L
    private const val GESTURE_NAME = "Gesture name"
    private const val GESTURE_FINGER_ID = 80L
    private const val GESTURE_PRESS_MOTION_ID = 801L
    private const val GESTURE_RELEASE_MOTION_ID = 802L
    private const val GESTURE_FINGER_START_TIME_MS = 0L
    private const val GESTURE_PRESS_X = 42
    private const val GESTURE_PRESS_Y = 78

    fun getNewGestureEntity(
        id: Long = GESTURE_ID,
        name: String = GESTURE_NAME,
        priority: Int = 0,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.GESTURE),
        gestureFingers = listOf(
            GestureFingerWithMotions(
                finger = GestureFingerEntity(GESTURE_FINGER_ID, id, 0, GESTURE_FINGER_START_TIME_MS),
                motions = listOf(
                    GestureFingerMotionEntity(
                        id = GESTURE_PRESS_MOTION_ID,
                        fingerId = GESTURE_FINGER_ID,
                        index = 0,
                        motionType = GestureFingerMotionType.PRESS,
                        xCoordinateType = CoordinatesType.LITERAL,
                        yCoordinateType = CoordinatesType.LITERAL,
                        xLiteral = GESTURE_PRESS_X,
                        yLiteral = GESTURE_PRESS_Y,
                        xOffsetCoordinateType = CoordinatesType.LITERAL,
                        yOffsetCoordinateType = CoordinatesType.LITERAL,
                        xOffsetLiteral = 0,
                        yOffsetLiteral = 0,
                    ),
                    GestureFingerMotionEntity(
                        id = GESTURE_RELEASE_MOTION_ID,
                        fingerId = GESTURE_FINGER_ID,
                        index = 1,
                        motionType = GestureFingerMotionType.RELEASE,
                    ),
                ),
            ),
        ),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewGesture(
        id: Long = GESTURE_ID,
        name: String? = GESTURE_NAME,
        priority: Int = 0,
        eventId: Long,
    ) = Gesture(
        id = id.asIdentifier(),
        eventId = eventId.asIdentifier(),
        name = name,
        priority = priority,
        fingers = listOf(
            GestureFinger(
                id = GESTURE_FINGER_ID.asIdentifier(),
                startTimeMs = GESTURE_FINGER_START_TIME_MS,
                motions = listOf(
                    GestureFingerMotion.Press(
                        id = GESTURE_PRESS_MOTION_ID.asIdentifier(),
                        position = Position.Coordinates(Literal(GESTURE_PRESS_X), Literal(GESTURE_PRESS_Y)),
                        offset = Position.Coordinates(Literal(0), Literal(0)),
                    ),
                    GestureFingerMotion.Release(
                        id = GESTURE_RELEASE_MOTION_ID.asIdentifier(),
                    ),
                ),
            ),
        ),
    )


    /* ------- Pause Action Data ------- */

    private const val PAUSE_ID = 9L
    private const val PAUSE_NAME = "Pause name"
    private const val PAUSE_DURATION = 500L

    fun getNewPauseEntity(
        id: Long = PAUSE_ID,
        name: String = PAUSE_NAME,
        priority: Int = 0,
        pauseDuration: Long = PAUSE_DURATION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.PAUSE, pauseDuration = pauseDuration),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewPause(
        id: Long = PAUSE_ID,
        name: String? = PAUSE_NAME,
        priority: Int = 0,
        pauseDuration: Long? = PAUSE_DURATION,
        eventId: Long,
    ) = Pause(id.asIdentifier(), eventId.asIdentifier(), name, priority, pauseDuration)


    /* ------- Intent Action Data ------- */

    private const val INTENT_ID = 149L
    private const val INTENT_NAME = "Intent name"
    private const val INTENT_IS_ADVANCED = true
    private const val INTENT_IS_BROADCAST = true
    private const val INTENT_ACTION = "com.toto.tata.ACTION_TOTO"
    private const val INTENT_COMPONENT_NAME_STRING = "com.toto.tata/com.toto.tata.Activity"
    private val INTENT_COMPONENT_NAME = ComponentName.unflattenFromString("com.toto.tata/com.toto.tata.Activity")
    private const val INTENT_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

    fun getNewIntentEntity(
        id: Long = INTENT_ID,
        name: String = INTENT_NAME,
        priority: Int = 0,
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean? = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: String? = INTENT_COMPONENT_NAME_STRING,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: List<IntentExtraEntity> = emptyList()
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.INTENT, isAdvanced = isAdvanced,
            isBroadcast = isBroadcast, intentAction = action, componentName = componentName, flags = flags),
        gestureFingers = emptyList(),
        intentExtras = intentExtras,
        eventsToggle = emptyList(),
    )

    fun getNewIntent(
        id: Long = INTENT_ID,
        name: String? = INTENT_NAME,
        priority: Int = 0,
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: ComponentName? = INTENT_COMPONENT_NAME,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: MutableList<IntentExtra<out Any>> = mutableListOf()
    ) = Intent(id.asIdentifier(), eventId.asIdentifier(), name, priority, isAdvanced, isBroadcast, action, componentName, flags, intentExtras)


    /* ------- Intent Extra Data ------- */

    private const val INTENT_EXTRA_ID = 547L
    private const val INTENT_EXTRA_ACTION_ID = INTENT_ID
    private val INTENT_EXTRA_TYPE = IntentExtraType.INTEGER
    private const val INTENT_EXTRA_KEY = "toto"

    fun getNewIntentExtraEntity(
        id: Long = INTENT_EXTRA_ID,
        actionId: Long = INTENT_EXTRA_ACTION_ID,
        type: IntentExtraType = INTENT_EXTRA_TYPE,
        key: String = INTENT_EXTRA_KEY,
        value: String,
    ) = IntentExtraEntity(id, actionId, type, key, value)

    fun <T> getNewIntentExtra(
        id: Long = INTENT_EXTRA_ID,
        actionId: Long = INTENT_EXTRA_ACTION_ID,
        key: String = INTENT_EXTRA_KEY,
        value: T,
    ) = IntentExtra(id.asIdentifier(), actionId.asIdentifier(), key, value)


    /* ------- Toggle Event Action Data ------- */

    private const val TOGGLE_EVENT_ID = 159L
    private const val TOGGLE_EVENT_NAME = "Toggle name"
    private const val TOGGLE_EVENT_TOGGLE_ALL = true
    private val TOGGLE_EVENT_TOGGLE_ALL_TYPE = ToggleEvent.ToggleType.TOGGLE

    fun getNewToggleEventEntity(
        id: Long = TOGGLE_EVENT_ID,
        name: String = TOGGLE_EVENT_NAME,
        priority: Int = 0,
        toggleAll: Boolean? = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType? = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
        eventToggles: List<EventToggleEntity> = emptyList(),
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.TOGGLE_EVENT, toggleAll = toggleAll, toggleAllType = toggleType?.toEntity()),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = eventToggles,
    )

    fun getNewToggleEvent(
        id: Long = TOGGLE_EVENT_ID,
        name: String? = TOGGLE_EVENT_NAME,
        priority: Int = 0,
        toggleAll: Boolean = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType? = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
        eventToggle: MutableList<EventToggle> = mutableListOf(),
        eventId: Long,
    ) = ToggleEvent(id.asIdentifier(), eventId.asIdentifier(), name, priority, toggleAll, toggleType, eventToggle)


    /* ------- Event toggle Data ------- */

    private const val EVENT_TOGGLE_ID = 875L
    private const val EVENT_TOGGLE_ACTION_ID = TOGGLE_EVENT_ID
    private const val EVENT_TOGGLE_TARGET_ID = 562L
    private val EVENT_TOGGLE_TYPE = ToggleEvent.ToggleType.TOGGLE

    fun getNewEventToggleEntity(
        id: Long = EVENT_TOGGLE_ID,
        actionId: Long = EVENT_TOGGLE_ACTION_ID,
        targetEventId: Long = EVENT_TOGGLE_TARGET_ID,
        type: ToggleEvent.ToggleType = EVENT_TOGGLE_TYPE,
    ) = EventToggleEntity(id, actionId, type.toEntity(), targetEventId)

    /* ------- Change Counter Action Data ------- */

    private const val CHANGE_COUNTER_ID = 214L
    private const val CHANGE_COUNTER_NAME = "ChangeCounter name"
    private const val CHANGE_COUNTER_COUNTER_NAME = "myCounter"
    private val CHANGE_COUNTER_OPERATION = ChangeCounter.OperationType.ADD
    private const val CHANGE_COUNTER_VALUE = 5.0

    fun getNewChangeCounterEntity(
        id: Long = CHANGE_COUNTER_ID,
        name: String = CHANGE_COUNTER_NAME,
        priority: Int = 0,
        counterName: String = CHANGE_COUNTER_COUNTER_NAME,
        operation: ChangeCounter.OperationType = CHANGE_COUNTER_OPERATION,
        operationValue: CounterOperationValue = CounterOperationValue.Number(CHANGE_COUNTER_VALUE),
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.CHANGE_COUNTER,
            counterName = counterName,
            counterOperation = ChangeCounterOperationType.valueOf(operation.name),
            counterOperationValueType = when (operationValue) {
                is CounterOperationValue.Number -> CounterOperationValueType.NUMBER
                is CounterOperationValue.Counter -> CounterOperationValueType.COUNTER
            },
            counterOperationValue = (operationValue as? CounterOperationValue.Number)?.value,
            counterOperationCounterName = (operationValue as? CounterOperationValue.Counter)?.counterName,
        ),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewChangeCounter(
        id: Long = CHANGE_COUNTER_ID,
        name: String? = CHANGE_COUNTER_NAME,
        priority: Int = 0,
        counterName: String = CHANGE_COUNTER_COUNTER_NAME,
        operation: ChangeCounter.OperationType = CHANGE_COUNTER_OPERATION,
        operationValue: CounterOperationValue = CounterOperationValue.Number(CHANGE_COUNTER_VALUE),
        eventId: Long,
    ) = ChangeCounter(id.asIdentifier(), eventId.asIdentifier(), name, priority, counterName, operation, operationValue)


    /* ------- Notification Action Data ------- */

    private const val NOTIFICATION_ID = 312L
    private const val NOTIFICATION_NAME = "Notification name"
    private const val NOTIFICATION_MESSAGE = "Hello from Smart AutoClicker"
    private const val NOTIFICATION_IMPORTANCE = 3

    fun getNewNotificationEntity(
        id: Long = NOTIFICATION_ID,
        name: String = NOTIFICATION_NAME,
        priority: Int = 0,
        messageText: String = NOTIFICATION_MESSAGE,
        channelImportance: Int = NOTIFICATION_IMPORTANCE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.NOTIFICATION,
            notificationMessageText = messageText,
            notificationImportance = channelImportance,
        ),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewNotification(
        id: Long = NOTIFICATION_ID,
        name: String? = NOTIFICATION_NAME,
        priority: Int = 0,
        messageText: String = NOTIFICATION_MESSAGE,
        channelImportance: Int = NOTIFICATION_IMPORTANCE,
        eventId: Long,
    ) = Notification(id.asIdentifier(), eventId.asIdentifier(), name, priority, messageText, channelImportance)


    /* ------- System Action Data ------- */

    private const val SYSTEM_ACTION_ID = 421L
    private const val SYSTEM_ACTION_NAME = "SystemAction name"
    private val SYSTEM_ACTION_TYPE = SystemAction.Type.BACK

    fun getNewSystemActionEntity(
        id: Long = SYSTEM_ACTION_ID,
        name: String = SYSTEM_ACTION_NAME,
        priority: Int = 0,
        type: SystemAction.Type = SYSTEM_ACTION_TYPE,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.SYSTEM,
            systemActionType = SystemActionType.valueOf(type.name),
        ),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSystemAction(
        id: Long = SYSTEM_ACTION_ID,
        name: String? = SYSTEM_ACTION_NAME,
        priority: Int = 0,
        type: SystemAction.Type = SYSTEM_ACTION_TYPE,
        eventId: Long,
    ) = SystemAction(id.asIdentifier(), eventId.asIdentifier(), name, priority, type)


    /* ------- Set Text Action Data ------- */

    private const val SET_TEXT_ID = 534L
    private const val SET_TEXT_NAME = "SetText name"
    private const val SET_TEXT_VALUE = "Hello World"
    private const val SET_TEXT_VALIDATE_INPUT = true

    fun getNewSetTextEntity(
        id: Long = SET_TEXT_ID,
        name: String = SET_TEXT_NAME,
        priority: Int = 0,
        text: String? = SET_TEXT_VALUE,
        validateInput: Boolean? = SET_TEXT_VALIDATE_INPUT,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(
            id, eventId, priority, name, ActionType.TEXT,
            textValue = text,
            textValidateInput = validateInput,
        ),
        gestureFingers = emptyList(),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSetText(
        id: Long = SET_TEXT_ID,
        name: String? = SET_TEXT_NAME,
        priority: Int = 0,
        text: String = SET_TEXT_VALUE,
        validateInput: Boolean = SET_TEXT_VALIDATE_INPUT,
        eventId: Long,
    ) = SetText(id.asIdentifier(), eventId.asIdentifier(), name, priority, text, validateInput)


    fun getNewEventToggleExtra(
        id: Long = EVENT_TOGGLE_ID,
        actionId: Long = EVENT_TOGGLE_ACTION_ID,
        targetEventId: Long = EVENT_TOGGLE_TARGET_ID,
        type: ToggleEvent.ToggleType = EVENT_TOGGLE_TYPE,
    ) = EventToggle(id.asIdentifier(), actionId.asIdentifier(), targetEventId.asIdentifier(), type)
}
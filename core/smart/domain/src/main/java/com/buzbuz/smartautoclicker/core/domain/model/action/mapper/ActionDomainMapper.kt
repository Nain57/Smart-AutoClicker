package com.buzbuz.smartautoclicker.core.domain.model.action.mapper

import android.content.ComponentName
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionEntity
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.entity.SystemActionType
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.toDomainGestureFinger
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.toDomainIntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.counter.getDomainCounterOperationValue

/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toDomain(cleanIds: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toDomainClick(cleanIds)
    ActionType.GESTURE -> toDomainGesture(cleanIds)
    ActionType.PAUSE -> toDomainPause(cleanIds)
    ActionType.INTENT -> toDomainIntent(cleanIds)
    ActionType.TOGGLE_EVENT -> toDomainToggleEvent(cleanIds)
    ActionType.CHANGE_COUNTER -> toDomainChangeCounter(cleanIds)
    ActionType.NOTIFICATION -> toDomainNotification(cleanIds)
    ActionType.SYSTEM -> toDomainSystem(cleanIds)
    ActionType.TEXT -> toDomainSetText(cleanIds)
}

/**
 * A click is stored as a gesture with a single finger pressing on the click position, waiting for the press duration,
 * and releasing.
 */
private fun CompleteActionEntity.toDomainClick(cleanIds: Boolean = false): Click {
    val motions = gestureFingers.firstOrNull()?.motions?.sortedBy { it.index }
    val press = motions?.find { it.motionType == GestureFingerMotionType.PRESS }

    return Click(
        id = Identifier(id = action.id, asTemporary = cleanIds),
        eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
        name = action.name,
        priority = action.priority,
        pressDuration = motions?.find { it.motionType == GestureFingerMotionType.WAIT }?.durationMs
            ?: DEFAULT_PRESS_DURATION_MS,
        position = press?.toDomainClickPosition(cleanIds),
        offset = press?.toDomainClickOffset(),
    )
}

private fun GestureFingerMotionEntity.toDomainClickPosition(cleanIds: Boolean): Position? {
    if (xCoordinateType == CoordinatesType.CONDITION && yCoordinateType == CoordinatesType.CONDITION) {
        return Position.Condition(
            conditionId = conditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
        )
    }

    val x = toClickCoordinatesValue(xCoordinateType, xLiteral, xCounter) ?: return null
    val y = toClickCoordinatesValue(yCoordinateType, yLiteral, yCounter) ?: return null

    return Position.Coordinates(x = x, y = y)
}

private fun GestureFingerMotionEntity.toDomainClickOffset(): Position.Coordinates? {
    val x = toClickCoordinatesValue(xOffsetCoordinateType, xOffsetLiteral, xOffsetCounter) ?: return null
    val y = toClickCoordinatesValue(yOffsetCoordinateType, yOffsetLiteral, yOffsetCounter) ?: return null

    return Position.Coordinates(x = x, y = y)
}

private fun CompleteActionEntity.toDomainGesture(cleanIds: Boolean = false) = Gesture(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    fingers = gestureFingers
        .sortedBy { it.finger.index }
        .map { it.toDomainGestureFinger() },
)

private fun CompleteActionEntity.toDomainPause(cleanIds: Boolean = false) = Pause(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pauseDuration = action.pauseDuration!!,
)

private fun CompleteActionEntity.toDomainIntent(cleanIds: Boolean = false) = Intent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    isAdvanced = action.isAdvanced,
    isBroadcast = action.isBroadcast ?: false,
    intentAction = action.intentAction,
    componentName = action.componentName.toComponentName(),
    flags = action.flags,
    extras = intentExtras.map { it.toDomainIntentExtra(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainToggleEvent(cleanIds: Boolean = false) = ToggleEvent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    toggleAll = action.toggleAll == true,
    toggleAllType = action.toggleAllType?.toDomain(),
    eventToggles = eventsToggle.map { it.toDomain(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainChangeCounter(cleanIds: Boolean = false) = ChangeCounter(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    counterName = action.counterName!!,
    operation = action.counterOperation!!.toDomain(),
    operationValue = getDomainCounterOperationValue(
        type = action.counterOperationValueType,
        numberValue = action.counterOperationValue,
        counterName = action.counterOperationCounterName,
        conditionId = action.counterOperationConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
        conditionPositionType = action.counterOperationConditionPositionType,
    ),
)

private fun CompleteActionEntity.toDomainNotification(cleanIds: Boolean = false) = Notification(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    channelImportance = action.notificationImportance!!,
    messageText = action.notificationMessageText!!,
)

private fun CompleteActionEntity.toDomainSystem(cleanIds: Boolean = false) = SystemAction(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    type = action.systemActionType?.toDomain()!!,
)

private fun CompleteActionEntity.toDomainSetText(cleanIds: Boolean = false) = SetText(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    text = action.textValue ?: "",
    validateInput = action.textValidateInput ?: false,
)

private fun EventToggleType.toDomain(): ToggleEvent.ToggleType =
    ToggleEvent.ToggleType.valueOf(name)

private fun ChangeCounterOperationType.toDomain(): ChangeCounter.OperationType =
    ChangeCounter.OperationType.valueOf(name)

private fun SystemActionType.toDomain(): SystemAction.Type =
    SystemAction.Type.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}

/** A null [type] with a null [literal] means the click has no position yet. */
private fun toClickCoordinatesValue(type: CoordinatesType?, literal: Int?, counter: String?): CoordinatesValue? =
    when (type) {
        CoordinatesType.COUNTER -> CoordinatesValue.Counter(counterName = counter ?: "")
        CoordinatesType.LITERAL, null -> literal?.let(CoordinatesValue::Literal)
        CoordinatesType.CONDITION -> null
    }

/** Press duration used for the clicks without a wait motion, in milliseconds. */
private const val DEFAULT_PRESS_DURATION_MS = 1L

package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Axis
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Back
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideMethod
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Home
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.KeyEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.LongPress
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.OpenNotifications
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.OpenQuickSettings
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Recents
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.RectPx
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Screenshot
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Scroll
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ShowKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.TypeText
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.toDomainIntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.toDomain

internal fun Action.toEntity(): ActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, action is incomplete: $this")

    return when (this) {
        is Click -> toClickEntity()
        is Swipe -> toSwipeEntity()
        is Pause -> toPauseEntity()
        is Intent -> toIntentEntity()
        is ToggleEvent -> toToggleEventEntity()
        is ChangeCounter -> toChangeCounterEntity()
        is Notification -> toNotificationEntity()

        is LongPress -> toLongPressEntity()
        is Scroll -> toScrollEntity()
        is Back -> toSimpleTypeEntity(ActionType.BACK)
        is Home -> toSimpleTypeEntity(ActionType.HOME)
        is Recents -> toSimpleTypeEntity(ActionType.RECENTS)
        is OpenNotifications -> toSimpleTypeEntity(ActionType.OPEN_NOTIFICATIONS)
        is OpenQuickSettings -> toSimpleTypeEntity(ActionType.OPEN_QUICK_SETTINGS)
        is Screenshot -> toScreenshotEntity()
        is HideKeyboard -> toHideKeyboardEntity()
        is ShowKeyboard -> toShowKeyboardEntity()
        is TypeText -> toTypeTextEntity()
        is KeyEvent -> toKeyEventEntity()
    }
}

private fun Click.toClickEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CLICK,
        pressDuration = pressDuration,
        clickPositionType = positionType.toEntity(),
        x = position?.x,
        y = position?.y,
        clickOnConditionId = clickOnConditionId?.databaseId,
        clickOffsetX = clickOffset?.x,
        clickOffsetY = clickOffset?.y,
    )

private fun Swipe.toSwipeEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.SWIPE,
        swipeDuration = swipeDuration,
        fromX = from?.x,
        fromY = from?.y,
        toX = to?.x,
        toY = to?.y,
    )

private fun Pause.toPauseEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.PAUSE,
        pauseDuration = pauseDuration,
    )

private fun Intent.toIntentEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.INTENT,
        isAdvanced = isAdvanced,
        isBroadcast = isBroadcast,
        intentAction = intentAction,
        componentName = componentName?.flattenToString(),
        flags = flags,
    )

private fun ToggleEvent.toToggleEventEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.TOGGLE_EVENT,
        toggleAllType = toggleAllType?.toEntity(),
        toggleAll = toggleAll,
    )

private fun ChangeCounter.toChangeCounterEntity(): ActionEntity {
    val isNumberValue = operationValue is CounterOperationValue.Number

    return ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.CHANGE_COUNTER,
        counterName = counterName,
        counterOperation = operation.toEntity(),
        counterOperationValueType = if (isNumberValue) CounterOperationValueType.NUMBER else CounterOperationValueType.COUNTER,
        counterOperationValue = if (isNumberValue) operationValue.value as Int else null,
        counterOperationCounterName = if (isNumberValue) null else operationValue.value as String,
    )
}

private fun Notification.toNotificationEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!,
        type = ActionType.NOTIFICATION,
        notificationImportance = channelImportance,
        notificationMessageType = messageType.toEntity(),
        notificationMessageText = messageText,
        notificationMessageCounterName = messageCounterName,
    )

private fun LongPress.toLongPressEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.LONG_PRESS,
    // reuse click columns if you chose reuse:
    pressDuration = holdDuration,
    clickPositionType = positionType.toEntity(),
    x = position?.x, y = position?.y,
    clickOnConditionId = onConditionId?.databaseId,
    clickOffsetX = offset?.x, clickOffsetY = offset?.y,
)

private fun Scroll.toScrollEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.SCROLL,
    scrollAxis = axis!!.name,
    scrollDistancePercent = distancePercent,
    scrollDuration = duration,
    scrollStutter = stutter,
)

private fun Action.toSimpleTypeEntity(kind: ActionType) = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!, type = kind
)

private fun Screenshot.toScreenshotEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.SCREENSHOT,
    screenshotLeft = roi?.left, screenshotTop = roi?.top,
    screenshotWidth = roi?.width, screenshotHeight = roi?.height,
    screenshotPath = savePath,
)

private fun HideKeyboard.toHideKeyboardEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.HIDE_KEYBOARD,
    hideKeyboardMethod = method.name,
)

private fun ShowKeyboard.toShowKeyboardEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.SHOW_KEYBOARD,
    // reuse click columns for focus target:
    clickPositionType = positionType.toEntity(),
    x = position?.x, y = position?.y,
    clickOnConditionId = onConditionId?.databaseId,
    clickOffsetX = offset?.x, clickOffsetY = offset?.y,
)

private fun TypeText.toTypeTextEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.TYPE_TEXT,
    typeText = text,
)

private fun KeyEvent.toKeyEventEntity() = ActionEntity(
    id = id.databaseId, eventId = eventId.databaseId, priority = priority, name = name!!,
    type = ActionType.KEY_EVENT,
    keyCodesCsv = codes!!.joinToString(","),
    keyIntervalMs = intervalMs,
)

/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toDomain(cleanIds: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toDomainClick(cleanIds)
    ActionType.SWIPE -> toDomainSwipe(cleanIds)
    ActionType.PAUSE -> toDomainPause(cleanIds)
    ActionType.INTENT -> toDomainIntent(cleanIds)
    ActionType.TOGGLE_EVENT -> toDomainToggleEvent(cleanIds)
    ActionType.CHANGE_COUNTER -> toDomainChangeCounter(cleanIds)
    ActionType.NOTIFICATION -> toDomainNotification(cleanIds)

    ActionType.LONG_PRESS -> toDomainLongPress(cleanIds)
    ActionType.SCROLL -> toDomainScroll(cleanIds)
    ActionType.BACK -> toDomainBack(cleanIds)
    ActionType.HOME -> toDomainHome(cleanIds)
    ActionType.RECENTS -> toDomainRecents(cleanIds)
    ActionType.OPEN_NOTIFICATIONS -> toDomainOpenNotifications(cleanIds)
    ActionType.OPEN_QUICK_SETTINGS -> toDomainOpenQuickSettings(cleanIds)
    ActionType.SCREENSHOT -> toDomainScreenshot(cleanIds)
    ActionType.HIDE_KEYBOARD -> toDomainHideKeyboard(cleanIds)
    ActionType.SHOW_KEYBOARD -> toDomainShowKeyboard(cleanIds)
    ActionType.TYPE_TEXT -> toDomainTypeText(cleanIds)
    ActionType.KEY_EVENT -> toDomainKeyEvent(cleanIds)
}

private fun CompleteActionEntity.toDomainClick(cleanIds: Boolean = false) = Click(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pressDuration = action.pressDuration!!,
    positionType = action.clickPositionType!!.toDomain(),
    position = getPositionIfValid(action.x, action.y),
    clickOnConditionId = action.clickOnConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
    clickOffset =
        if (action.clickOffsetX != null && action.clickOffsetY != null) Point(action.clickOffsetX!!, action.clickOffsetY!!)
        else null
)

private fun CompleteActionEntity.toDomainSwipe(cleanIds: Boolean = false) = Swipe(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    swipeDuration = action.swipeDuration!!,
    from = getPositionIfValid(action.fromX, action.fromY),
    to = getPositionIfValid(action.toX, action.toY),
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
    operationValue = CounterOperationValue.getCounterOperationValue(
        type = action.counterOperationValueType,
        numberValue = action.counterOperationValue,
        counterName = action.counterOperationCounterName,
    ),
)

private fun CompleteActionEntity.toDomainNotification(cleanIds: Boolean = false) = Notification(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    channelImportance = action.notificationImportance!!,
    messageType = action.notificationMessageType!!.toDomain(),
    messageText = action.notificationMessageText!!,
    messageCounterName = action.notificationMessageCounterName!!,
)

private fun ClickPositionType.toDomain(): Click.PositionType =
    Click.PositionType.valueOf(name)

private fun EventToggleType.toDomain(): ToggleEvent.ToggleType =
    ToggleEvent.ToggleType.valueOf(name)

private fun ChangeCounterOperationType.toDomain(): ChangeCounter.OperationType =
    ChangeCounter.OperationType.valueOf(name)

private fun NotificationMessageType.toDomain(): Notification.MessageType =
    Notification.MessageType.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}

private fun getPositionIfValid(x: Int?, y: Int?): Point? =
    if (x != null && y != null) Point(x, y) else null

private fun CompleteActionEntity.toDomainLongPress(cleanIds: Boolean) = LongPress(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    holdDuration = action.pressDuration!!, // reused
    positionType = action.clickPositionType!!.toDomain(),
    position = getPositionIfValid(action.x, action.y),
    onConditionId = action.clickOnConditionId?.let { Identifier(it, asTemporary = cleanIds) },
    offset = if (action.clickOffsetX != null && action.clickOffsetY != null)
        Point(action.clickOffsetX!!, action.clickOffsetY!!) else null
)

private fun CompleteActionEntity.toDomainScroll(cleanIds: Boolean) = Scroll(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    axis = action.scrollAxis?.let { Axis.valueOf(it) },
    distancePercent = action.scrollDistancePercent,
    duration = action.scrollDuration,
    stutter = action.scrollStutter ?: true,
)

private fun CompleteActionEntity.toDomainBack(cleanIds: Boolean) = Back(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
)
private fun CompleteActionEntity.toDomainHome(cleanIds: Boolean) = Home(
    id = Identifier(action.id, asTemporary = cleanIds), eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name, priority = action.priority
)
private fun CompleteActionEntity.toDomainRecents(cleanIds: Boolean) = Recents(
    id = Identifier(action.id, asTemporary = cleanIds), eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name, priority = action.priority
)
private fun CompleteActionEntity.toDomainOpenNotifications(cleanIds: Boolean) = OpenNotifications(
    id = Identifier(action.id, asTemporary = cleanIds), eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name, priority = action.priority
)
private fun CompleteActionEntity.toDomainOpenQuickSettings(cleanIds: Boolean) = OpenQuickSettings(
    id = Identifier(action.id, asTemporary = cleanIds), eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name, priority = action.priority
)

private fun CompleteActionEntity.toDomainScreenshot(cleanIds: Boolean) = Screenshot(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    roi = if (action.screenshotLeft != null && action.screenshotTop != null &&
        action.screenshotWidth != null && action.screenshotHeight != null)
        RectPx(action.screenshotLeft!!, action.screenshotTop!!, action.screenshotWidth!!, action.screenshotHeight!!)
    else null,
    savePath = action.screenshotPath
)

private fun CompleteActionEntity.toDomainHideKeyboard(cleanIds: Boolean) = HideKeyboard(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    method = action.hideKeyboardMethod?.let { HideMethod.valueOf(it) } ?: HideMethod.BACK_THEN_TAP_OUTSIDE,
)

private fun CompleteActionEntity.toDomainShowKeyboard(cleanIds: Boolean) = ShowKeyboard(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    positionType = action.clickPositionType!!.toDomain(),
    position = getPositionIfValid(action.x, action.y),
    onConditionId = action.clickOnConditionId?.let { Identifier(it, asTemporary = cleanIds) },
    offset = if (action.clickOffsetX != null && action.clickOffsetY != null)
        Point(action.clickOffsetX!!, action.clickOffsetY!!) else null
)

private fun CompleteActionEntity.toDomainTypeText(cleanIds: Boolean) = TypeText(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    text = action.typeText,
)

private fun CompleteActionEntity.toDomainKeyEvent(cleanIds: Boolean) = KeyEvent(
    id = Identifier(action.id, asTemporary = cleanIds),
    eventId = Identifier(action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    codes = action.keyCodesCsv?.split(',')?.filter { it.isNotBlank() }?.map { it.trim().toInt() },
    intervalMs = action.keyIntervalMs ?: 50,
)
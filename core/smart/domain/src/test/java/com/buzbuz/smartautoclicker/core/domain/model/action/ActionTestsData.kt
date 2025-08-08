
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName
import android.content.Intent
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraType
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

    fun getNewClickEntity(
        id: Long = CLICK_ID,
        name: String = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: ClickPositionType =
            if (x != null && y != null) ClickPositionType.USER_SELECTED
            else ClickPositionType.ON_DETECTED_CONDITION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.CLICK, x = x, y = y,
            clickOnConditionId = clickOnConditionId, clickPositionType = positionType,
            pressDuration = pressDuration),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewClick(
        id: Long = CLICK_ID,
        name: String? = CLICK_NAME,
        priority: Int = 0,
        pressDuration: Long? = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: Click.PositionType =
            if (x != null && y != null) Click.PositionType.USER_SELECTED
            else Click.PositionType.ON_DETECTED_CONDITION,
        eventId: Long,
    ) = Click(id.asIdentifier(), eventId.asIdentifier(), name, priority, pressDuration, positionType,
        if (x != null && y != null) Point(x, y) else null,
        clickOnConditionId?.let { Identifier(databaseId = clickOnConditionId) },
    )


    /* ------- Swipe Action Data ------- */

    private const val SWIPE_ID = 8L
    private const val SWIPE_NAME = "Swipe name"
    private const val SWIPE_DURATION = 1000L
    private const val SWIPE_FROM_X_POSITION = 42
    private const val SWIPE_FROM_Y_POSITION = 78
    private const val SWIPE_TO_X_POSITION = 789
    private const val SWIPE_TO_Y_POSITION = 1445

    fun getNewSwipeEntity(
        id: Long = SWIPE_ID,
        name: String = SWIPE_NAME,
        priority: Int = 0,
        swipeDuration: Long = SWIPE_DURATION,
        fromX: Int = SWIPE_FROM_X_POSITION,
        fromY: Int = SWIPE_FROM_Y_POSITION,
        toX: Int = SWIPE_TO_X_POSITION,
        toY: Int = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.SWIPE, fromX = fromX, fromY = fromY, toX = toX,
            toY = toY, swipeDuration = swipeDuration),
        intentExtras = emptyList(),
        eventsToggle = emptyList(),
    )

    fun getNewSwipe(
        id: Long = SWIPE_ID,
        name: String? = SWIPE_NAME,
        priority: Int = 0,
        swipeDuration: Long? = SWIPE_DURATION,
        fromX: Int? = SWIPE_FROM_X_POSITION,
        fromY: Int? = SWIPE_FROM_Y_POSITION,
        toX: Int? = SWIPE_TO_X_POSITION,
        toY: Int? = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) : Swipe = Swipe(id.asIdentifier(), eventId.asIdentifier(), name, priority, swipeDuration,
        if (fromX != null && fromY != null) Point(fromX, fromY) else null,
        if (toX != null && toY != null) Point(toX, toY) else null,
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
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: String = INTENT_COMPONENT_NAME_STRING,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: List<IntentExtraEntity> = emptyList()
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.INTENT, isAdvanced = isAdvanced,
            isBroadcast = isBroadcast, intentAction = action, componentName = componentName, flags = flags),
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
        componentName: ComponentName = INTENT_COMPONENT_NAME!!,
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
        toggleAll: Boolean = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
        eventToggles: List<EventToggleEntity> = emptyList(),
        eventId: Long,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.TOGGLE_EVENT, toggleAll = toggleAll, toggleAllType = toggleType.toEntity()),
        intentExtras = emptyList(),
        eventsToggle = eventToggles,
    )

    fun getNewToggleEvent(
        id: Long = TOGGLE_EVENT_ID,
        name: String? = TOGGLE_EVENT_NAME,
        priority: Int = 0,
        toggleAll: Boolean = TOGGLE_EVENT_TOGGLE_ALL,
        toggleType: ToggleEvent.ToggleType = TOGGLE_EVENT_TOGGLE_ALL_TYPE,
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

    fun getNewEventToggleExtra(
        id: Long = EVENT_TOGGLE_ID,
        actionId: Long = EVENT_TOGGLE_ACTION_ID,
        targetEventId: Long = EVENT_TOGGLE_TARGET_ID,
        type: ToggleEvent.ToggleType = EVENT_TOGGLE_TYPE,
    ) = EventToggle(id.asIdentifier(), actionId.asIdentifier(), targetEventId.asIdentifier(), type)
}
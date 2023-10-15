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
package com.buzbuz.smartautoclicker.core.domain.utils

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.database.entity.*
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

/** Data set for the database tests. */
internal object TestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_END_CONDITION_OPERATOR = AND
    const val SCENARIO_RANDOMIZE = false

    fun getNewScenarioEntity(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        @ConditionOperator endConditionOperator: Int = SCENARIO_END_CONDITION_OPERATOR,
        randomize: Boolean = SCENARIO_RANDOMIZE,
    ) = ScenarioEntity(id, name, detectionQuality, endConditionOperator, randomize)

    fun getNewScenario(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        @ConditionOperator endConditionOperator: Int = SCENARIO_END_CONDITION_OPERATOR,
        randomize: Boolean = SCENARIO_RANDOMIZE,
        eventCount: Int = 0,
    ) = Scenario(id.asIdentifier(), name, detectionQuality, endConditionOperator, randomize, eventCount)


    /* ------- Event Data ------- */

    const val EVENT_ID = 1667L
    private const val EVENT_NAME = "EventName"
    private const val EVENT_CONDITION_OPERATOR = AND
    private const val EVENT_ENABLED_ON_START = true

    fun getNewEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        scenarioId: Long,
        priority: Int,
    ) = EventEntity(id, scenarioId, name, conditionOperator, priority, enabledOnStart)

    fun getNewEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        actions: List<Action> = emptyList(),
        conditions: List<Condition> = emptyList(),
        scenarioId: Long,
        priority: Int,
    ) = Event(id.asIdentifier(), scenarioId.asIdentifier(), name, conditionOperator, priority, actions, conditions, enabledOnStart)


    /* ------- End Condition Data ------- */

    private const val END_ID = 42L
    private const val END_SCENARIO_ID = SCENARIO_ID
    private const val END_EVENT_ID = EVENT_ID
    private const val END_EVENT_NAME = EVENT_NAME
    private const val END_EXECUTIONS = 42

    fun getNewEndConditionEntity(
        id: Long = END_ID,
        scenarioId: Long = END_SCENARIO_ID,
        eventId: Long = END_EVENT_ID,
        executions: Int = END_EXECUTIONS,
    ) = EndConditionEntity(id, scenarioId, eventId, executions)

    fun getNewEndCondition(
        id: Long = SCENARIO_ID,
        scenarioId: Long = END_SCENARIO_ID,
        eventId: Long = END_EVENT_ID,
        eventName: String = END_EVENT_NAME,
        executions: Int = END_EXECUTIONS,
    ) = EndCondition(id.asIdentifier(), scenarioId.asIdentifier(), eventId.asIdentifier(), eventName, executions)

    fun getNewEndConditionWithEvent(
        id: Long = END_ID,
        scenarioId: Long = END_SCENARIO_ID,
        eventId: Long = END_EVENT_ID,
        executions: Int = END_EXECUTIONS,
        event: EventEntity,
    ) = EndConditionWithEvent(EndConditionEntity(id, scenarioId, eventId, executions), event)

    /* ------- Click Action Data ------- */

    const val CLICK_ID = 7L
    private const val CLICK_NAME = "Click name"
    private const val CLICK_PRESS_DURATION = 250L
    private const val CLICK_X_POSITION = 24
    private const val CLICK_Y_POSITION = 87

    fun getNewClickEntity(
        id: Long = CLICK_ID,
        name: String = CLICK_NAME,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: ClickPositionType =
            if (x != null && y != null) ClickPositionType.USER_SELECTED
            else ClickPositionType.ON_DETECTED_CONDITION,
        eventId: Long,
        priority: Int,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.CLICK, x = x, y = y,
            clickOnConditionId = clickOnConditionId, clickPositionType = positionType,
            pressDuration = pressDuration),
        intentExtras = emptyList(),
    )

    fun getNewClick(
        id: Long = CLICK_ID,
        name: String? = CLICK_NAME,
        pressDuration: Long? = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnConditionId: Long? = null,
        positionType: Action.Click.PositionType =
            if (x != null && y != null) Action.Click.PositionType.USER_SELECTED
            else Action.Click.PositionType.ON_DETECTED_CONDITION,
        eventId: Long,
    ) = Action.Click(id.asIdentifier(), eventId.asIdentifier(), name, pressDuration, positionType, x, y,
            clickOnConditionId?.let { Identifier(databaseId = clickOnConditionId) })


    /* ------- Swipe Action Data ------- */

    const val SWIPE_ID = 8L
    private const val SWIPE_NAME = "Swipe name"
    private const val SWIPE_DURATION = 1000L
    private const val SWIPE_FROM_X_POSITION = 42
    private const val SWIPE_FROM_Y_POSITION = 78
    private const val SWIPE_TO_X_POSITION = 789
    private const val SWIPE_TO_Y_POSITION = 1445

    fun getNewSwipeEntity(
        id: Long = SWIPE_ID,
        name: String = SWIPE_NAME,
        swipeDuration: Long = SWIPE_DURATION,
        fromX: Int = SWIPE_FROM_X_POSITION,
        fromY: Int = SWIPE_FROM_Y_POSITION,
        toX: Int = SWIPE_TO_X_POSITION,
        toY: Int = SWIPE_TO_Y_POSITION,
        eventId: Long,
        priority: Int,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.SWIPE, fromX = fromX, fromY = fromY, toX = toX,
            toY = toY, swipeDuration = swipeDuration),
        intentExtras = emptyList(),
    )

    fun getNewSwipe(
        id: Long = SWIPE_ID,
        name: String? = SWIPE_NAME,
        swipeDuration: Long? = SWIPE_DURATION,
        fromX: Int? = SWIPE_FROM_X_POSITION,
        fromY: Int? = SWIPE_FROM_Y_POSITION,
        toX: Int? = SWIPE_TO_X_POSITION,
        toY: Int? = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) : Action.Swipe = Action.Swipe(id.asIdentifier(), eventId.asIdentifier(), name, swipeDuration, fromX, fromY, toX, toY)


    /* ------- Pause Action Data ------- */

    const val PAUSE_ID = 9L
    private const val PAUSE_NAME = "Pause name"
    private const val PAUSE_DURATION = 500L

    fun getNewPauseEntity(
        id: Long = PAUSE_ID,
        name: String = PAUSE_NAME,
        pauseDuration: Long = PAUSE_DURATION,
        eventId: Long,
        priority: Int,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.PAUSE, pauseDuration = pauseDuration),
        intentExtras = emptyList(),
    )

    fun getNewPause(
        id: Long = PAUSE_ID,
        name: String? = PAUSE_NAME,
        pauseDuration: Long? = PAUSE_DURATION,
        eventId: Long,
    ) = Action.Pause(id.asIdentifier(), eventId.asIdentifier(), name, pauseDuration)


    /* ------- Intent Action Data ------- */

    const val INTENT_ID = 149L
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
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: String = INTENT_COMPONENT_NAME_STRING,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        priority: Int,
        intentExtras: List<IntentExtraEntity> = emptyList()
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.INTENT, isAdvanced = isAdvanced,
            isBroadcast = isBroadcast, intentAction = action, componentName = componentName, flags = flags),
        intentExtras = intentExtras,
    )

    fun getNewIntent(
        id: Long = INTENT_ID,
        name: String? = INTENT_NAME,
        isAdvanced: Boolean = INTENT_IS_ADVANCED,
        isBroadcast: Boolean = INTENT_IS_BROADCAST,
        action: String = INTENT_ACTION,
        componentName: ComponentName = INTENT_COMPONENT_NAME!!,
        flags: Int = INTENT_FLAGS,
        eventId: Long,
        intentExtras: MutableList<IntentExtra<out Any>> = mutableListOf()
    ) = Action.Intent(id.asIdentifier(), eventId.asIdentifier(), name, isAdvanced, isBroadcast, action, componentName, flags, intentExtras)


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

    const val TOGGLE_EVENT_ID = 159L
    private const val TOGGLE_EVENT_NAME = "Toggle name"
    private const val TOGGLE_EVENT_EVENT_ID = 500L
    private val TOGGLE_EVENT_TYPE = Action.ToggleEvent.ToggleType.ENABLE

    fun getToggleEventEntity(
        id: Long = TOGGLE_EVENT_ID,
        name: String = TOGGLE_EVENT_NAME,
        toggleEventId: Long = TOGGLE_EVENT_EVENT_ID,
        toggleType: ToggleEventType = TOGGLE_EVENT_TYPE.toEntity(),
        eventId: Long,
        priority: Int,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.TOGGLE_EVENT, toggleEventId = toggleEventId, toggleEventType = toggleType),
        intentExtras = emptyList(),
    )

    fun getNewToggleEvent(
        id: Long = TOGGLE_EVENT_ID,
        name: String? = TOGGLE_EVENT_NAME,
        toggleEventId: Long = TOGGLE_EVENT_EVENT_ID,
        toggleType: Action.ToggleEvent.ToggleType = TOGGLE_EVENT_TYPE,
        eventId: Long,
    ) = Action.ToggleEvent(id.asIdentifier(), eventId.asIdentifier(), name, Identifier(databaseId = toggleEventId), toggleType)

    /* ------- Condition Data ------- */

    private const val CONDITION_ID = 25L
    private const val CONDITION_PATH = "/toto/tutu/tata"
    private const val CONDITION_LEFT = 0
    private const val CONDITION_TOP = 45
    private const val CONDITION_RIGHT = 69
    private const val CONDITION_BOTTOM = 89
    private const val CONDITION_THRESHOLD = 25
    private const val CONDITION_NAME = "Condition name"
    private const val CONDITION_DETECTION_TYPE = EXACT

    fun getNewConditionEntity(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        path: String = CONDITION_PATH,
        left: Int = CONDITION_LEFT,
        top: Int = CONDITION_TOP,
        right: Int = CONDITION_RIGHT,
        bottom: Int = CONDITION_BOTTOM,
        threshold: Int = CONDITION_THRESHOLD,
        detectionType: Int = CONDITION_DETECTION_TYPE,
        eventId: Long
    ) = ConditionEntity(id, eventId, name, path, left, top, right, bottom, threshold, detectionType, true)

    fun getNewCondition(
        id: Long = CONDITION_ID,
        name: String = CONDITION_NAME,
        path: String? = CONDITION_PATH,
        left: Int = CONDITION_LEFT,
        top: Int = CONDITION_TOP,
        right: Int = CONDITION_RIGHT,
        bottom: Int = CONDITION_BOTTOM,
        threshold: Int = CONDITION_THRESHOLD,
        detectionType: Int = CONDITION_DETECTION_TYPE,
        bitmap: Bitmap? = null,
        eventId: Long
    ) = Condition(id.asIdentifier(), eventId.asIdentifier(), name, path, Rect(left, top, right, bottom), threshold, detectionType, true, bitmap)

    private fun Long.asIdentifier() = Identifier(
        databaseId = this,
        domainId = if (this == 0L) 1L else null,
    )
}
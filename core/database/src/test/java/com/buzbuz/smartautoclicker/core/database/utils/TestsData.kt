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
package com.buzbuz.smartautoclicker.core.database.utils

import android.content.Intent
import com.buzbuz.smartautoclicker.core.database.entity.*

/** Data set for the database tests. */
internal object TestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_END_CONDITION_OPERATOR = 1

    fun getNewScenarioEntity(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        endConditionOperator: Int = SCENARIO_END_CONDITION_OPERATOR,
    ) = ScenarioEntity(id, name, detectionQuality, endConditionOperator)


    /* ------- Event Data ------- */

    const val EVENT_ID = 1667L
    const val EVENT_ID_2 = 123456L

    const val EVENT_NAME = "EventName"
    const val EVENT_CONDITION_OPERATOR = 1
    val EVENT_STOP_AFTER = null

    fun getNewEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        scenarioId: Long,
        priority: Int,
    ) = EventEntity(id, scenarioId, name, conditionOperator, priority)

    /* ------- End Condition Data ------- */

    private const val END_ID = 42L
    const val END_SCENARIO_ID = SCENARIO_ID
    private const val END_EVENT_ID = EVENT_ID
    private const val END_EXECUTIONS = 42

    fun getNewEndConditionEntity(
        id: Long = END_ID,
        scenarioId: Long = END_SCENARIO_ID,
        eventId: Long = END_EVENT_ID,
        executions: Int = END_EXECUTIONS,
    ) = EndConditionEntity(id, scenarioId, eventId, executions)


    /* ------- Click Action Data ------- */

    private const val CLICK_ID = 7L
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
        eventId: Long,
        priority: Int,
        clickOnConditionId: Long? = null,
        clickPositionType: ClickPositionType = if (x != null && y != null) ClickPositionType.USER_SELECTED else ClickPositionType.ON_DETECTED_CONDITION,
    ) = CompleteActionEntity(
        action = ActionEntity(id, eventId, priority, name, ActionType.CLICK, x = x, y = y,
            clickPositionType = clickPositionType, clickOnConditionId = clickOnConditionId, pressDuration = pressDuration),
        intentExtras = emptyList(),
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


    /* ------- Pause Action Data ------- */

    private const val PAUSE_ID = 9L
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


    /* ------- Intent Action Data ------- */

    const val INTENT_ID = 149L
    private const val INTENT_NAME = "Intent name"
    private const val INTENT_IS_ADVANCED = true
    private const val INTENT_IS_BROADCAST = true
    private const val INTENT_ACTION = "com.toto.tata.ACTION_TOTO"
    private const val INTENT_COMPONENT_NAME_STRING = "com.toto.tata/com.toto.tata.Activity"
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


    /* ------- Condition Data ------- */

    private const val CONDITION_ID = 25L
    const val CONDITION_PATH = "/toto/tutu/tata"
    private const val CONDITION_LEFT = 0
    private const val CONDITION_TOP = 45
    private const val CONDITION_RIGHT = 69
    private const val CONDITION_BOTTOM = 89
    private const val CONDITION_THRESHOLD = 25
    private const val CONDITION_NAME = "Condition name"
    private const val CONDITION_DETECTION_TYPE = 1

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

    fun CompleteEventEntity.cloneEvent() = copy(
        event = event.copy(),
        conditions = conditions.cloneConditions(),
        actions = actions.cloneActions(),
    )

    fun List<ConditionEntity>.cloneConditions() = map { it.copy() }

    fun List<CompleteActionEntity>.cloneActions() = map {
        it.copy(
            action = it.action.copy(),
            intentExtras = it.intentExtras.cloneExtras(),
        )
    }

    fun List<IntentExtraEntity>.cloneExtras() = map { it.copy() }
}
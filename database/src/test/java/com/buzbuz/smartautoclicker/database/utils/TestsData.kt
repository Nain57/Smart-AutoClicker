/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.buzbuz.smartautoclicker.database.domain.*

import com.buzbuz.smartautoclicker.database.room.entity.*

/** Data set for the database tests. */
internal object TestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_END_CONDITION_OPERATOR = AND

    fun getNewScenarioEntity(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        @ConditionOperator endConditionOperator: Int = SCENARIO_END_CONDITION_OPERATOR,
    ) = ScenarioEntity(id, name, detectionQuality, endConditionOperator)

    fun getNewScenario(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        @ConditionOperator endConditionOperator: Int = SCENARIO_END_CONDITION_OPERATOR,
        eventCount: Int = 0,
    ) = Scenario(id, name, detectionQuality, endConditionOperator, eventCount)


    /* ------- Event Data ------- */

    const val EVENT_ID = 1667L
    const val EVENT_NAME = "EventName"
    const val EVENT_CONDITION_OPERATOR = AND
    val EVENT_STOP_AFTER = null

    fun getNewEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        stopAfter: Int? = EVENT_STOP_AFTER,
        scenarioId: Long,
        priority: Int,
    ) = EventEntity(id, scenarioId, name, conditionOperator, priority, stopAfter)

    fun getNewEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        stopAfter: Int? = EVENT_STOP_AFTER,
        actions: MutableList<Action>? = null,
        conditions: MutableList<Condition>? = null,
        scenarioId: Long,
        priority: Int,
    ) = Event(id, scenarioId, name, conditionOperator, priority, actions, conditions, stopAfter)


    /* ------- End Condition Data ------- */

    const val END_ID = 42L
    const val END_SCENARIO_ID = SCENARIO_ID
    const val END_EVENT_ID = EVENT_ID
    const val END_EVENT_NAME = EVENT_NAME
    const val END_EXECUTIONS = 42

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
    ) = EndCondition(id, scenarioId, eventId, eventName, executions)

    fun getNewEndConditionWithEvent(
        id: Long = END_ID,
        scenarioId: Long = END_SCENARIO_ID,
        eventId: Long = END_EVENT_ID,
        executions: Int = END_EXECUTIONS,
        event: EventEntity,
    ) = EndConditionWithEvent(EndConditionEntity(id, scenarioId, eventId, executions), event)

    /* ------- Click Action Data ------- */

    const val CLICK_ID = 7L
    const val CLICK_NAME = "Click name"
    const val CLICK_PRESS_DURATION = 250L
    const val CLICK_X_POSITION = 24
    const val CLICK_Y_POSITION = 87

    fun getNewClickEntity(
        id: Long = CLICK_ID,
        name: String = CLICK_NAME,
        pressDuration: Long = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        eventId: Long,
        priority: Int,
        clickOnCondition: Boolean = x != null && y != null,
    ) = ActionEntity(id, eventId, priority, name, ActionType.CLICK, x = x, y = y, clickOnCondition = clickOnCondition, pressDuration = pressDuration)

    fun getNewClick(
        id: Long = CLICK_ID,
        name: String? = CLICK_NAME,
        pressDuration: Long? = CLICK_PRESS_DURATION,
        x: Int? = CLICK_X_POSITION,
        y: Int? = CLICK_Y_POSITION,
        clickOnCondition: Boolean = x != null && y != null,
        eventId: Long,
    ) = Action.Click(id, eventId, name, pressDuration, x, y, clickOnCondition)


    /* ------- Swipe Action Data ------- */

    const val SWIPE_ID = 8L
    const val SWIPE_NAME = "Swipe name"
    const val SWIPE_DURATION = 1000L
    const val SWIPE_FROM_X_POSITION = 42
    const val SWIPE_FROM_Y_POSITION = 78
    const val SWIPE_TO_X_POSITION = 789
    const val SWIPE_TO_Y_POSITION = 1445

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
    ) = ActionEntity(id, eventId, priority, name, ActionType.SWIPE, fromX = fromX, fromY = fromY, toX = toX, toY = toY,
        swipeDuration = swipeDuration)

    fun getNewSwipe(
        id: Long = SWIPE_ID,
        name: String? = SWIPE_NAME,
        swipeDuration: Long? = SWIPE_DURATION,
        fromX: Int? = SWIPE_FROM_X_POSITION,
        fromY: Int? = SWIPE_FROM_Y_POSITION,
        toX: Int? = SWIPE_TO_X_POSITION,
        toY: Int? = SWIPE_TO_Y_POSITION,
        eventId: Long,
    ) : Action.Swipe = Action.Swipe(id, eventId, name, swipeDuration, fromX, fromY, toX, toY)


    /* ------- Pause Action Data ------- */

    const val PAUSE_ID = 9L
    const val PAUSE_NAME = "Pause name"
    const val PAUSE_DURATION = 500L

    fun getNewPauseEntity(
        id: Long = PAUSE_ID,
        name: String = PAUSE_NAME,
        pauseDuration: Long = PAUSE_DURATION,
        eventId: Long,
        priority: Int,
    ) = ActionEntity(id, eventId, priority, name, ActionType.PAUSE, pauseDuration = pauseDuration)

    fun getNewPause(
        id: Long = PAUSE_ID,
        name: String? = PAUSE_NAME,
        pauseDuration: Long? = PAUSE_DURATION,
        eventId: Long,
    ) = Action.Pause(id, eventId, name, pauseDuration)


    /* ------- Condition Data ------- */

    const val CONDITION_ID = 25L
    const val CONDITION_PATH = "/toto/tutu/tata"
    const val CONDITION_LEFT = 0
    const val CONDITION_TOP = 45
    const val CONDITION_RIGHT = 69
    const val CONDITION_BOTTOM = 89
    const val CONDITION_THRESHOLD = 25
    const val CONDITION_NAME = "Condition name"
    const val CONDITION_DETECTION_TYPE = EXACT

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
    ) = Condition(id, eventId, name, path, Rect(left, top, right, bottom), threshold, detectionType, true, bitmap)
}
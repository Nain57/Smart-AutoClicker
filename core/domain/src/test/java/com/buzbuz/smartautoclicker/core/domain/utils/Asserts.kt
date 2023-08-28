/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail


fun assertSameEndConditionNoIdCheck(expected: EndCondition, actual: EndCondition) = assertTrue(
    "End conditions are not the same",
    expected.scenarioId == actual.scenarioId
            && expected.eventName == actual.eventName
            && expected.executions == actual.executions
)

fun assertSameEventListNoIdCheck(expected: List<Event>, actual: List<Event>) {
    forEachExpectedAndActual(expected, actual) { expectedEvent, actualEvent ->
        assertSameEventNoIdCheck(expectedEvent, actualEvent)
    }
}

private fun assertSameEventNoIdCheck(expected: Event, actual: Event) {
    assertTrue(
        "Events content are not the same",
        expected.name == actual.name
            && expected.conditionOperator == actual.conditionOperator
            && expected.enabledOnStart == actual.enabledOnStart
            && expected.priority == actual.priority
    )

    val expectedConditions = expected.conditions
    val actualConditions = actual.conditions
    forEachExpectedAndActual(expectedConditions, actualConditions) { expectedCondition, actualCondition ->
        assertSameConditionNoIdCheck(expectedCondition, actualCondition)
    }

    val expectedActions = expected.actions
    val actualActions = actual.actions
    forEachExpectedAndActual(expectedActions, actualActions) { expectedAction, actualAction ->
        assertSameActionNoIdCheck(expectedAction, actualAction)
    }
}

private fun assertSameConditionNoIdCheck(expected: Condition, actual: Condition) = assertTrue(
    "Conditions are not the same",
    expected.name == actual.name
            && expected.shouldBeDetected == actual.shouldBeDetected
            && expected.area == actual.area
            && expected.detectionType == actual.detectionType
            && expected.threshold == actual.threshold
)

private fun assertSameActionNoIdCheck(expected: Action, actual: Action) {
    when {
        expected is Action.Click && actual is Action.Click -> assertSameClickNoIdCheck(expected, actual)
        expected is Action.Swipe && actual is Action.Swipe -> assertSameSwipeNoIdCheck(expected, actual)
        expected is Action.Pause && actual is Action.Pause -> assertSamePauseNoIdCheck(expected, actual)
        expected is Action.Intent && actual is Action.Intent -> assertSameIntentNoIdCheck(expected, actual)
        expected is Action.ToggleEvent && actual is Action.ToggleEvent -> assertSameToggleEventNoIdCheck(expected, actual)
        else -> fail("Actions doesn't have the same type")
    }
}

private fun assertSameClickNoIdCheck(expected: Action.Click, actual: Action.Click) = assertTrue(
    "Clicks are not the same",
    expected.name == actual.name
            && expected.pressDuration == actual.pressDuration
            && expected.positionType == actual.positionType
            && expected.x == actual.x
            && expected.y == actual.y
            && expected.clickOnConditionId == actual.clickOnConditionId
)

private fun assertSameSwipeNoIdCheck(expected: Action.Swipe, actual: Action.Swipe) = assertTrue(
    "Swipes are not the same",
    expected.name == actual.name
            && expected.swipeDuration == actual.swipeDuration
            && expected.fromX == actual.fromX
            && expected.fromY == actual.fromY
            && expected.toX == actual.toX
            && expected.toY == actual.toY
)

private fun assertSamePauseNoIdCheck(expected: Action.Pause, actual: Action.Pause) = assertTrue(
    "Pauses are not the same",
    expected.name == actual.name
            && expected.pauseDuration == actual.pauseDuration
)

private fun assertSameIntentNoIdCheck(expected: Action.Intent, actual: Action.Intent) = assertTrue(
    "Intents are not the same",
    expected.name == actual.name
            && expected.isAdvanced == actual.isAdvanced
            && expected.isBroadcast == actual.isBroadcast
            && expected.intentAction == actual.intentAction
            && expected.componentName == actual.componentName
            && expected.flags == actual.flags
)

private fun assertSameToggleEventNoIdCheck(expected: Action.ToggleEvent, actual: Action.ToggleEvent) = assertTrue(
    "ToggleEvents are not the same",
    expected.name == actual.name
            && expected.toggleEventType == actual.toggleEventType
)

private fun <T> forEachExpectedAndActual(expected: List<T>, actual: List<T>, closure: (T, T) -> Unit) {
    assertEquals("Can't execute expected and actual for each, size are different", expected.size, actual.size)
    for (i in expected.indices) closure(expected[i], actual[i])
}
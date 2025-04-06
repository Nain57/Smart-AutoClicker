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
package com.buzbuz.smartautoclicker.core.domain.utils

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

fun assertSameEventListNoIdCheck(expected: List<ImageEvent>, actual: List<ImageEvent>) {
    forEachExpectedAndActual(expected, actual) { expectedEvent, actualEvent ->
        assertSameEventNoIdCheck(expectedEvent, actualEvent)
    }
}

private fun assertSameEventNoIdCheck(expected: ImageEvent, actual: ImageEvent) {
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

private fun assertSameConditionNoIdCheck(expected: ImageCondition, actual: ImageCondition) = assertTrue(
    "Conditions are not the same",
    expected.name == actual.name
            && expected.shouldBeDetected == actual.shouldBeDetected
            && expected.captureArea == actual.captureArea
            && expected.detectionType == actual.detectionType
            && expected.threshold == actual.threshold
)

private fun assertSameActionNoIdCheck(expected: Action, actual: Action) {
    when {
        expected is Click && actual is Click -> assertSameClickNoIdCheck(expected, actual)
        expected is Swipe && actual is Swipe -> assertSameSwipeNoIdCheck(expected, actual)
        expected is Pause && actual is Pause -> assertSamePauseNoIdCheck(expected, actual)
        expected is Intent && actual is Intent -> assertSameIntentNoIdCheck(expected, actual)
        expected is ToggleEvent && actual is ToggleEvent -> assertSameToggleEventNoIdCheck(expected, actual)
        else -> fail("Actions doesn't have the same type")
    }
}

private fun assertSameClickNoIdCheck(expected: Click, actual: Click) = assertTrue(
    "Clicks are not the same",
    expected.name == actual.name
            && expected.pressDuration == actual.pressDuration
            && expected.positionType == actual.positionType
            && expected.position?.x == actual.position?.x
            && expected.position?.y == actual.position?.y
            && expected.clickOnConditionId == actual.clickOnConditionId
)

private fun assertSameSwipeNoIdCheck(expected: Swipe, actual: Swipe) = assertTrue(
    "Swipes are not the same",
    expected.name == actual.name
            && expected.swipeDuration == actual.swipeDuration
            && expected.from?.x == actual.from?.x
            && expected.from?.y == actual.from?.y
            && expected.to?.x == actual.to?.x
            && expected.to?.y == actual.to?.y
)

private fun assertSamePauseNoIdCheck(expected: Pause, actual: Pause) = assertTrue(
    "Pauses are not the same",
    expected.name == actual.name
            && expected.pauseDuration == actual.pauseDuration
)

private fun assertSameIntentNoIdCheck(expected: Intent, actual: Intent) = assertTrue(
    "Intents are not the same",
    expected.name == actual.name
            && expected.isAdvanced == actual.isAdvanced
            && expected.isBroadcast == actual.isBroadcast
            && expected.intentAction == actual.intentAction
            && expected.componentName == actual.componentName
            && expected.flags == actual.flags
)

private fun assertSameToggleEventNoIdCheck(expected: ToggleEvent, actual: ToggleEvent) = assertTrue(
    "ToggleEvents are not the same",
    expected.name == actual.name
            //&& expected.toggleEventType == actual.toggleEventType
)

private fun <T> forEachExpectedAndActual(expected: List<T>, actual: List<T>, closure: (T, T) -> Unit) {
    assertEquals("Can't execute expected and actual for each, size are different", expected.size, actual.size)
    for (i in expected.indices) closure(expected[i], actual[i])
}
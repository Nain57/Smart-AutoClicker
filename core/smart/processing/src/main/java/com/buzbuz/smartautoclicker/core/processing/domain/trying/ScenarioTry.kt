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
package com.buzbuz.smartautoclicker.core.processing.domain.trying

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

internal sealed class ScenarioTry {

    internal abstract val scenario: Scenario
    internal abstract val imageEvents: List<ImageEvent>
    internal abstract val triggerEvents: List<TriggerEvent>

}

internal class ImageEventTry(
    override val scenario: Scenario,
    val event: ImageEvent,
) : ScenarioTry() {

    override val imageEvents: List<ImageEvent> = listOf(event)
    override val triggerEvents: List<TriggerEvent> = emptyList()
}

internal class ImageConditionTry(
    override val scenario: Scenario,
    val condition: ImageCondition,
) : ScenarioTry() {

    override val imageEvents: List<ImageEvent> = listOf(getTestImageEvent())
    override val triggerEvents: List<TriggerEvent> = emptyList()

    private fun getTestImageEvent(): ImageEvent {
        val tryEventId = Identifier(databaseId = 1L)
        return ImageEvent(
            id = tryEventId,
            scenarioId = scenario.id,
            name = "Test Event",
            conditionOperator = AND,
            enabledOnStart = true,
            priority = 0,
            conditions = listOf(condition.copy(eventId = tryEventId)),
            actions = listOf(getTestPauseAction(tryEventId)),
            keepDetecting = false,
        )
    }

    private fun getTestPauseAction(eventId: Identifier): Action.Pause =
        Action.Pause(
            id = Identifier(databaseId = 1L),
            eventId = eventId,
            name = "Test Pause",
            pauseDuration = 500L,
            priority = 0,
        )
}

internal class ActionTry(
    override val scenario: Scenario,
    val action: Action,
) : ScenarioTry() {

    override val imageEvents: List<ImageEvent> = emptyList()
    override val triggerEvents: List<TriggerEvent> = listOf(getTestTriggerEvent())

    private fun getTestTriggerEvent(): TriggerEvent {
        val tryEventId = Identifier(databaseId = 1L)
        return TriggerEvent(
            id = tryEventId,
            scenarioId = scenario.id,
            name = "Test Event",
            conditionOperator = AND,
            enabledOnStart = true,
            conditions = listOf(getTestTriggerCondition(tryEventId)),
            actions = listOf(action, getStopScenarioAction(tryEventId)),
        )
    }

    private fun getStopScenarioAction(eventId: Identifier): Action.ToggleEvent =
        Action.ToggleEvent(
            id = Identifier(databaseId = 1L),
            eventId = eventId,
            name = "Test Pause",
            priority = 0,
            toggleAll = true,
            toggleAllType = Action.ToggleEvent.ToggleType.DISABLE,
        )

    private fun getTestTriggerCondition(eventId: Identifier): TriggerCondition.OnTimerReached =
        TriggerCondition.OnTimerReached(
            id = Identifier(databaseId = 1L),
            eventId = eventId,
            name = "Test timer reached",
            durationMs = 1L,
            restartWhenReached = false,
        )
}
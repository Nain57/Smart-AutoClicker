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
package com.buzbuz.smartautoclicker.core.domain.model.event

import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.ScenarioTestsData
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

internal object EventTestsData {

    const val EVENT_SCENARIO_ID = ScenarioTestsData.SCENARIO_ID
    const val EVENT_ID = 1667L
    const val EVENT_NAME = "EventName"
    const val EVENT_CONDITION_OPERATOR = AND
    const val EVENT_ENABLED_ON_START = true

    fun getNewImageEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        scenarioId: Long,
        priority: Int = 0,
    ) = EventEntity(id, scenarioId, name, conditionOperator, priority, enabledOnStart, EventType.IMAGE_EVENT, keepDetecting = false)

    fun getNewTriggerEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        scenarioId: Long,
    ) = EventEntity(id, scenarioId, name, conditionOperator, -1, enabledOnStart, EventType.TRIGGER_EVENT)

    fun getNewImageEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        actions: List<Action> = emptyList(),
        conditions: List<ImageCondition> = emptyList(),
        scenarioId: Long,
        priority: Int = 0,
    ) = ScreenEvent(id.asIdentifier(), scenarioId.asIdentifier(), name, conditionOperator, actions, conditions, enabledOnStart, priority, false)

    fun getNewTriggerEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        actions: List<Action> = emptyList(),
        conditions: List<TriggerCondition> = emptyList(),
        scenarioId: Long,
    ) = TriggerEvent(id.asIdentifier(), scenarioId.asIdentifier(), name, conditionOperator, actions, conditions, enabledOnStart)
}
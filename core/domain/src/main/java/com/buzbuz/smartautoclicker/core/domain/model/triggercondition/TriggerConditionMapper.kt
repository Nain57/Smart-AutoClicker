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
package com.buzbuz.smartautoclicker.core.domain.model.triggercondition

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType


internal fun TriggerCondition.toEntity(): ConditionEntity = when (this) {
    is TriggerCondition.OnScenarioStart -> toScenarioStartEntity()
    is TriggerCondition.OnScenarioEnd -> toScenarioEndEntity()
    is TriggerCondition.OnBroadcastReceived -> toBroadcastReceivedEntity()
    is TriggerCondition.OnCounterCountReached -> toCounterReachedEntity()
    is TriggerCondition.OnTimerReached -> toTimerReachedEntity()
}

private fun TriggerCondition.OnScenarioStart.toScenarioStartEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = triggerEventId.databaseId,
        name = name,
        type = ConditionType.ON_SCENARIO_START,
    )

private fun TriggerCondition.OnScenarioEnd.toScenarioEndEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = triggerEventId.databaseId,
        name = name,
        type = ConditionType.ON_SCENARIO_END,
    )

private fun TriggerCondition.OnBroadcastReceived.toBroadcastReceivedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = triggerEventId.databaseId,
        name = name,
        type = ConditionType.ON_BROADCAST_RECEIVED,
        broadcastAction = intentAction,
    )

private fun TriggerCondition.OnCounterCountReached.toCounterReachedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = triggerEventId.databaseId,
        name = name,
        type = ConditionType.ON_COUNTER_REACHED,
        counterName = counterName,
        counterComparisonOperation = comparisonOperation.toEntity(),
        counterValue = counterValue,
    )

private fun TriggerCondition.OnTimerReached.toTimerReachedEntity(): ConditionEntity =
    ConditionEntity(
        id = id.databaseId,
        eventId = triggerEventId.databaseId,
        name = name,
        type = ConditionType.ON_COUNTER_REACHED,
        timerValueMs = durationMs,
    )

internal fun ConditionEntity.toTriggerCondition(asDomain: Boolean = false): TriggerCondition =
    when (type) {
        ConditionType.ON_SCENARIO_START -> toScenarioStart(asDomain)
        ConditionType.ON_SCENARIO_END-> toScenarioEnd(asDomain)
        ConditionType.ON_BROADCAST_RECEIVED-> toBroadcastReceived(asDomain)
        ConditionType.ON_COUNTER_REACHED -> toCounterReached(asDomain)
        ConditionType.ON_TIMER_REACHED -> toTimerReached(asDomain)
        else -> throw IllegalArgumentException("Unsupported condition type for a TriggerCondition")
    }

private fun ConditionEntity.toScenarioStart(asDomain: Boolean = false): TriggerCondition =
    TriggerCondition.OnScenarioStart(
        id = Identifier(id = id, asDomain = asDomain),
        triggerEventId = Identifier(id = eventId, asDomain = asDomain),
        name = name,
    )

private fun ConditionEntity.toScenarioEnd(asDomain: Boolean = false): TriggerCondition =
    TriggerCondition.OnScenarioEnd(
        id = Identifier(id = id, asDomain = asDomain),
        triggerEventId = Identifier(id = eventId, asDomain = asDomain),
        name = name,
    )

private fun ConditionEntity.toBroadcastReceived(asDomain: Boolean = false): TriggerCondition =
    TriggerCondition.OnBroadcastReceived(
        id = Identifier(id = id, asDomain = asDomain),
        triggerEventId = Identifier(id = eventId, asDomain = asDomain),
        name = name,
        intentAction = broadcastAction!!,
    )

private fun ConditionEntity.toCounterReached(asDomain: Boolean = false): TriggerCondition =
    TriggerCondition.OnCounterCountReached(
        id = Identifier(id = id, asDomain = asDomain),
        triggerEventId = Identifier(id = eventId, asDomain = asDomain),
        name = name,
        counterName = counterName!!,
        comparisonOperation = counterComparisonOperation!!.toDomain(),
        counterValue = counterValue!!,
    )

private fun ConditionEntity.toTimerReached(asDomain: Boolean = false): TriggerCondition =
    TriggerCondition.OnTimerReached(
        id = Identifier(id = id, asDomain = asDomain),
        triggerEventId = Identifier(id = eventId, asDomain = asDomain),
        name = name,
        durationMs = timerValueMs!!,
    )

private fun CounterComparisonOperation.toDomain(): TriggerCondition.OnCounterCountReached.ComparisonOperation =
    TriggerCondition.OnCounterCountReached.ComparisonOperation.valueOf(name)
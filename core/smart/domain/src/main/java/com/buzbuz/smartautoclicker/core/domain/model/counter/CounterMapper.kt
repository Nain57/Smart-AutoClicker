/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.counter

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationConditionPositionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.CountersEntity

internal fun Counter.toEntity(): CountersEntity =
    CountersEntity(
        name = counterName,
        scenarioId = scenarioId.databaseId,
        startingValue = defaultValue,
    )

internal fun ComparisonOperation.toEntity(): CounterComparisonOperation =
    when (this) {
        ComparisonOperation.EQUALS -> CounterComparisonOperation.EQUALS
        ComparisonOperation.LOWER -> CounterComparisonOperation.LOWER
        ComparisonOperation.LOWER_OR_EQUALS -> CounterComparisonOperation.LOWER_OR_EQUALS
        ComparisonOperation.GREATER -> CounterComparisonOperation.GREATER
        ComparisonOperation.GREATER_OR_EQUALS -> CounterComparisonOperation.GREATER_OR_EQUALS
    }

internal fun CounterOperationValue.toEntityType(): CounterOperationValueType =
    when (this) {
        is CounterOperationValue.Counter -> CounterOperationValueType.COUNTER
        is CounterOperationValue.Number -> CounterOperationValueType.NUMBER
        is CounterOperationValue.Condition -> CounterOperationValueType.CONDITION
    }

internal fun CounterOperationValue.Condition.PositionType.toEntity(): CounterOperationConditionPositionType =
    when (this) {
        CounterOperationValue.Condition.PositionType.CONDITION_BOTTOM -> CounterOperationConditionPositionType.CONDITION_BOTTOM
        CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X -> CounterOperationConditionPositionType.CONDITION_CENTER_X
        CounterOperationValue.Condition.PositionType.CONDITION_CENTER_Y -> CounterOperationConditionPositionType.CONDITION_CENTER_Y
        CounterOperationValue.Condition.PositionType.CONDITION_LEFT -> CounterOperationConditionPositionType.CONDITION_LEFT
        CounterOperationValue.Condition.PositionType.CONDITION_RIGHT -> CounterOperationConditionPositionType.CONDITION_RIGHT
        CounterOperationValue.Condition.PositionType.CONDITION_TOP -> CounterOperationConditionPositionType.CONDITION_TOP
    }

internal fun CounterOperationConditionPositionType.toDomain(): CounterOperationValue.Condition.PositionType =
    when (this) {
        CounterOperationConditionPositionType.CONDITION_BOTTOM -> CounterOperationValue.Condition.PositionType.CONDITION_BOTTOM
        CounterOperationConditionPositionType.CONDITION_CENTER_X -> CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X
        CounterOperationConditionPositionType.CONDITION_CENTER_Y -> CounterOperationValue.Condition.PositionType.CONDITION_CENTER_Y
        CounterOperationConditionPositionType.CONDITION_LEFT -> CounterOperationValue.Condition.PositionType.CONDITION_LEFT
        CounterOperationConditionPositionType.CONDITION_RIGHT -> CounterOperationValue.Condition.PositionType.CONDITION_RIGHT
        CounterOperationConditionPositionType.CONDITION_TOP -> CounterOperationValue.Condition.PositionType.CONDITION_TOP
    }

internal fun CountersEntity.toDomain(): Counter =
    Counter(
        counterName = name,
        scenarioId = Identifier(scenarioId, asTemporary = false),
        defaultValue = startingValue,
    )

internal fun CounterComparisonOperation.toDomain(): ComparisonOperation =
    when (this) {
        CounterComparisonOperation.EQUALS -> ComparisonOperation.EQUALS
        CounterComparisonOperation.LOWER -> ComparisonOperation.LOWER
        CounterComparisonOperation.LOWER_OR_EQUALS -> ComparisonOperation.LOWER_OR_EQUALS
        CounterComparisonOperation.GREATER -> ComparisonOperation.GREATER
        CounterComparisonOperation.GREATER_OR_EQUALS -> ComparisonOperation.GREATER_OR_EQUALS
    }

internal fun getDomainCounterOperationValue(
    type: CounterOperationValueType?,
    numberValue: Double?,
    counterName: String?,
    conditionId: Identifier? = null,
    conditionPositionType: CounterOperationConditionPositionType? = null,
): CounterOperationValue = when (type ?: CounterOperationValueType.NUMBER) {
    CounterOperationValueType.COUNTER -> CounterOperationValue.Counter(counterName ?: "")
    CounterOperationValueType.NUMBER -> CounterOperationValue.Number(numberValue ?: 0.0)
    CounterOperationValueType.CONDITION ->
        if (conditionId != null && conditionPositionType != null) {
            CounterOperationValue.Condition(conditionId, conditionPositionType.toDomain())
        } else {
            CounterOperationValue.Number(numberValue ?: 0.0)
        }
}
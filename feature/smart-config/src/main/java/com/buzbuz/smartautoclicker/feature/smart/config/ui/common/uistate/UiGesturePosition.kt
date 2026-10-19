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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

sealed interface UiGesturePosition {

    data class Position(
        val xValue: String,
        val yValue: String,
    ) : UiGesturePosition

    data class Condition(
        val inError: Boolean,
        val condition: ScreenCondition?,
    ) : UiGesturePosition
}

internal fun Position.toPositionUiState(editionRepository: EditionRepository): UiGesturePosition =
    when (this) {
        is Position.Coordinates ->
            UiGesturePosition.Position(
                xValue = x.toValueString(),
                yValue = y.toValueString(),
            )

        is Position.Condition -> {
            val screenConditions = editionRepository.editionState
                .getEditedEventScreenConditions() ?: emptyList()
            val isAnd = editionRepository.editionState.getEditedEvent()?.conditionOperator == AND
            val resolvedCondition =
                if (isAnd) conditionId?.let { id -> screenConditions.find { it.id == id } }
                else null

            UiGesturePosition.Condition(
                inError = screenConditions.isEmpty() || (isAnd && resolvedCondition == null),
                condition = resolvedCondition,
            )
        }
    }

/** A click without a position yet is displayed as an empty pair of coordinates. */
internal fun Click.toPositionUiState(editionRepository: EditionRepository): UiGesturePosition =
    position?.toPositionUiState(editionRepository)
        ?: UiGesturePosition.Position(xValue = "", yValue = "")

private fun CoordinatesValue.toValueString(): String =
    when (this) {
        is CoordinatesValue.Literal -> value.toString()
        is CoordinatesValue.Counter -> "{$counterName}"
    }
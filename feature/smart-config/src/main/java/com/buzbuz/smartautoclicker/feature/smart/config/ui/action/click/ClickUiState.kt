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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition

data class ClickUiState(
    val canBeSaved: Boolean,
    val hasUnsavedModifications: Boolean,
    val name: String?,
    val nameError: Boolean,
    val pressDuration: String?,
    val pressDurationError: Boolean,
    val positionState: ClickPositionUiState?,
    val availableConditions: List<ScreenCondition>,
)

data class ClickPositionUiState(
    val position: UiGesturePosition,
    /** False when the parent event has no screen condition to click on, forcing the click on a position. */
    val isConditionSelectionVisible: Boolean,
    val isClickOffsetVisible: Boolean,
    val clickOffsetDescription: String? = null,
)
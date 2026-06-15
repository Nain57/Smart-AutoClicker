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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition

sealed class EventDialogUiState {

    abstract val canBeSaved: Boolean
    abstract val hasUnsavedModifications: Boolean
    abstract val name: String?
    abstract val nameError: Boolean
    abstract val enabledOnStart: Boolean
    abstract val conditionOperator: Int
    abstract val actionsItems: List<EventChildrenItem>

    data class ScreenEvent(
        override val canBeSaved: Boolean,
        override val hasUnsavedModifications: Boolean,
        override val name: String?,
        override val nameError: Boolean,
        override val enabledOnStart: Boolean,
        override val conditionOperator: Int,
        override val actionsItems: List<EventChildrenItem>,
        val keepDetecting: Boolean,
        val canTryEvent: Boolean,
        val cooldownEnabled: Boolean,
        val cooldownValue: String,
        val cooldownUnit: TimeUnitDropDownItem,
        val imageConditionsItems: List<UiScreenCondition>,
    ) : EventDialogUiState()

    data class TriggerEvent(
        override val canBeSaved: Boolean,
        override val hasUnsavedModifications: Boolean,
        override val name: String?,
        override val nameError: Boolean,
        override val enabledOnStart: Boolean,
        override val conditionOperator: Int,
        override val actionsItems: List<EventChildrenItem>,
        val triggerConditionsItems: List<EventChildrenItem>,
    ) : EventDialogUiState()
}

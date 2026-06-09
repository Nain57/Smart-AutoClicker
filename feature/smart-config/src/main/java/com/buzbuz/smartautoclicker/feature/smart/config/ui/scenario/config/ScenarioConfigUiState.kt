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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R

data class ScenarioConfigUiState(
    val name: String,
    val randomizeChecked: Boolean,
    val keepScreenOnChecked: Boolean,
    val computeRateState: ComputeRateLimitUiState,
    val qualityUiState: DetectionQualityUiState,
)

data class ComputeRateLimitUiState(
    val isEnabled: Boolean,
    val value: Double,
    val maxValue: Double,
    val unit: ComputeRateUnitDropdownItem
)

data class DetectionQualityUiState(
    val displayText: String,
    val qualityValue: Float,
    val min: Float,
    val max: Float,
)

sealed class ComputeRateUnitDropdownItem(@StringRes title: Int) : DropdownItem(title) {
    data object Minute : ComputeRateUnitDropdownItem(R.string.field_scenario_fps_dropdown_item_minute)
    data object Second : ComputeRateUnitDropdownItem(R.string.field_scenario_fps_dropdown_item_second)
}

fun allComputeRateUnitDropdownItems(): List<ComputeRateUnitDropdownItem> =
    listOf(ComputeRateUnitDropdownItem.Second, ComputeRateUnitDropdownItem.Minute)
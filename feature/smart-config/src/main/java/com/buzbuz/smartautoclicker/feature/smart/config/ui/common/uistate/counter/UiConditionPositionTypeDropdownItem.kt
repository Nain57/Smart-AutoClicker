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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class UiConditionPositionTypeDropdownItem(
    @StringRes title: Int,
    @StringRes helperText: Int,
) : DropdownItem(title, helperText) {

    data object Left : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_left,
        R.string.dropdown_condition_position_type_item_left_desc,
    )
    data object Right : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_right,
        R.string.dropdown_condition_position_type_item_right_desc,
    )
    data object CenterX : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_center_x,
        R.string.dropdown_condition_position_type_item_center_x_desc,
    )
    data object Top : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_top,
        R.string.dropdown_condition_position_type_item_top_desc,
    )
    data object Bottom : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_bottom,
        R.string.dropdown_condition_position_type_item_bottom_desc,
    )
    data object CenterY : UiConditionPositionTypeDropdownItem(
        R.string.dropdown_condition_position_type_item_center_y,
        R.string.dropdown_condition_position_type_item_center_y_desc,
    )
}

internal fun allConditionPositionTypeDropdownItems(): List<UiConditionPositionTypeDropdownItem> = listOf(
    UiConditionPositionTypeDropdownItem.CenterX,
    UiConditionPositionTypeDropdownItem.CenterY,
    UiConditionPositionTypeDropdownItem.Left,
    UiConditionPositionTypeDropdownItem.Right,
    UiConditionPositionTypeDropdownItem.Top,
    UiConditionPositionTypeDropdownItem.Bottom,
)

internal fun CounterOperationValue.Condition.PositionType.toDropdownItem(): UiConditionPositionTypeDropdownItem =
    when (this) {
        CounterOperationValue.Condition.PositionType.CONDITION_LEFT -> UiConditionPositionTypeDropdownItem.Left
        CounterOperationValue.Condition.PositionType.CONDITION_RIGHT -> UiConditionPositionTypeDropdownItem.Right
        CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X -> UiConditionPositionTypeDropdownItem.CenterX
        CounterOperationValue.Condition.PositionType.CONDITION_TOP -> UiConditionPositionTypeDropdownItem.Top
        CounterOperationValue.Condition.PositionType.CONDITION_BOTTOM -> UiConditionPositionTypeDropdownItem.Bottom
        CounterOperationValue.Condition.PositionType.CONDITION_CENTER_Y -> UiConditionPositionTypeDropdownItem.CenterY
    }

internal fun UiConditionPositionTypeDropdownItem.toPositionType(): CounterOperationValue.Condition.PositionType =
    when (this) {
        UiConditionPositionTypeDropdownItem.Left -> CounterOperationValue.Condition.PositionType.CONDITION_LEFT
        UiConditionPositionTypeDropdownItem.Right -> CounterOperationValue.Condition.PositionType.CONDITION_RIGHT
        UiConditionPositionTypeDropdownItem.CenterX -> CounterOperationValue.Condition.PositionType.CONDITION_CENTER_X
        UiConditionPositionTypeDropdownItem.Top -> CounterOperationValue.Condition.PositionType.CONDITION_TOP
        UiConditionPositionTypeDropdownItem.Bottom -> CounterOperationValue.Condition.PositionType.CONDITION_BOTTOM
        UiConditionPositionTypeDropdownItem.CenterY -> CounterOperationValue.Condition.PositionType.CONDITION_CENTER_Y
    }

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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition

import com.buzbuz.smartautoclicker.core.domain.model.condition.NumberFormatType
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R

sealed class UiNumberFormatDropdownItem(title: Int, helperText: Int) : DropdownItem(title, helperText) {
    data object Auto : UiNumberFormatDropdownItem(
        R.string.dropdown_item_number_format_auto,
        R.string.dropdown_item_number_format_auto_helper,
    )
    data object DotDecimal : UiNumberFormatDropdownItem(
        R.string.dropdown_item_number_format_dot_decimal,
        R.string.dropdown_item_number_format_dot_decimal_helper,
    )
    data object CommaDecimal : UiNumberFormatDropdownItem(
        R.string.dropdown_item_number_format_comma_decimal,
        R.string.dropdown_item_number_format_comma_decimal_helper,
    )
}

internal fun allNumberFormatDropdownItems(): List<UiNumberFormatDropdownItem> = listOf(
    UiNumberFormatDropdownItem.Auto,
    UiNumberFormatDropdownItem.DotDecimal,
    UiNumberFormatDropdownItem.CommaDecimal,
)

internal fun NumberFormatType.toDropdownItem(): UiNumberFormatDropdownItem =
    when (this) {
        NumberFormatType.AUTO -> UiNumberFormatDropdownItem.Auto
        NumberFormatType.DOT_DECIMAL -> UiNumberFormatDropdownItem.DotDecimal
        NumberFormatType.COMMA_DECIMAL -> UiNumberFormatDropdownItem.CommaDecimal
    }

internal fun UiNumberFormatDropdownItem.toNumberFormatType(): NumberFormatType =
    when (this) {
        UiNumberFormatDropdownItem.Auto -> NumberFormatType.AUTO
        UiNumberFormatDropdownItem.DotDecimal -> NumberFormatType.DOT_DECIMAL
        UiNumberFormatDropdownItem.CommaDecimal -> NumberFormatType.COMMA_DECIMAL
    }

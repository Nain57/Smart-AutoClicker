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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter

import android.text.InputType
import android.view.View

import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setup
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.NumberInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeStaticOrCounterSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection


fun IncludeStaticOrCounterSelectionBinding.setup(
    dropdownItems: List<UiCounterOperatorDropdownItem>,
    onOperatorSelected: (UiCounterOperatorDropdownItem) -> Unit,
    onChangeTypeClicked: (UiOperandType) -> Unit,
    onStaticValueChangedListener: (Double) -> Unit,
    onOpenCounterSelectionClicked: () -> Unit,
) {
    operatorField.setItems(
        label = root.context.getString(R.string.dropdown_comparison_operator_label),
        items = dropdownItems,
        onItemSelected = onOperatorSelected,
    )

    valueTypeMultiStateButton.apply {
        setup(
            MultiStateButtonConfig(
                icons = listOf(R.drawable.ic_numbers, R.drawable.ic_change_counter),
                singleSelection = true,
                selectionRequired = true,
            )
        )

        setOnCheckedListener { checkedId ->
            onChangeTypeClicked(
                when (checkedId) {
                    0 -> UiOperandType.STATIC
                    1 -> UiOperandType.COUNTER
                    else -> return@setOnCheckedListener
                }
            )
        }
    }

    staticValueLayout.apply {
        textField.filters = arrayOf(NumberInputFilter(type = Double::class))
        staticValueLayout.textLayout.setHint(R.string.field_counter_operation_value_label)

        setOnTextChangedListener {
            try {
                onStaticValueChangedListener(textField.text.toString().toDouble())
            } catch (_: NumberFormatException) { }
        }
    }

    counterValueLayout.setOnClickListener { onOpenCounterSelectionClicked() }
}

fun IncludeStaticOrCounterSelectionBinding.setSelectedOperator(item: UiCounterOperatorDropdownItem) {
    operatorField.setSelectedItem(item)
}

fun IncludeStaticOrCounterSelectionBinding.setValueInfo(uiState: UiStaticOrCounterSelection) {
    when (uiState) {
        is UiStaticOrCounterSelection.CounterValue -> {
            valueTypeMultiStateButton.setChecked(1)

            staticValueLayout.root.visibility = View.GONE
            counterValueLayout.root.visibility = View.VISIBLE

            counterValueLayout.setCounter(uiState)
        }

        is UiStaticOrCounterSelection.StaticValue -> {
            valueTypeMultiStateButton.setChecked(0)

            staticValueLayout.root.visibility = View.VISIBLE
            counterValueLayout.root.visibility = View.GONE

            staticValueLayout.setText(
                uiState.value.toNaturalDisplayString(),
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            )
        }
    }
}

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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.text.InputType
import android.view.View

import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.NumberInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeStaticOrCounterSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.UiStaticOrCounterSelection


fun IncludeStaticOrCounterSelectionBinding.setup(
    onStaticValueChangedListener: (Double) -> Unit,
    onOpenCounterSelectionClicked: () -> Unit,
) {

    staticValueLayout.apply {
        textField.filters = arrayOf(NumberInputFilter(type = Double::class))
        staticValueLayout.textLayout.setHint(R.string.field_counter_operation_value_label)

        setOnTextChangedListener {
            try {
                onStaticValueChangedListener(textField.text.toString().toDouble())
            } catch (nfEx: NumberFormatException) { }

        }
    }

    counterValueLayout.setOnClickListener { onOpenCounterSelectionClicked() }
    buttonCheckbox.setOnClickListener { onOpenCounterSelectionClicked() }
}

fun IncludeStaticOrCounterSelectionBinding.setValueInfo(uiState: UiStaticOrCounterSelection) {
    when (uiState) {
        is UiStaticOrCounterSelection.CounterValue -> {
            staticValueLayout.root.visibility = View.GONE
            counterValueLayout.visibility = View.VISIBLE

            if (uiState.counter?.counterName.isNullOrEmpty()) {
                title.setText(R.string.field_counter_selection_title_empty)
                description.setText(R.string.field_counter_selection_desc_empty)
            } else {
                title.text = uiState.counter.counterName
                description.text = root.context.getString(
                    R.string.field_counter_selection_desc,
                    uiState.counter.defaultValue,
                )
            }
        }

        is UiStaticOrCounterSelection.StaticValue -> {
            staticValueLayout.root.visibility = View.VISIBLE
            counterValueLayout.visibility = View.GONE

            staticValueLayout.setText(uiState.value.toString(), InputType.TYPE_CLASS_NUMBER)
        }
    }
}
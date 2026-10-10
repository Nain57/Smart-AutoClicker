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

import android.graphics.Bitmap
import android.text.InputType
import android.view.View

import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setup
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setIconBitmap
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setImageDrawable
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.utils.NumberInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeStaticOrCounterSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiConditionPositionTypeDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiOperandType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiCounterOperatorDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.UiStaticOrCounterSelection
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.counter.allConditionPositionTypeDropdownItems

import kotlinx.coroutines.Job


/**
 * Enables the condition operand type in [IncludeStaticOrCounterSelectionBinding].
 *
 * @param onOpenConditionSelectionClicked called when the user clicks on the condition field.
 * @param onPositionTypeSelected called when the user selects the condition position to use as value.
 */
data class ConditionOperandConfig(
    val onOpenConditionSelectionClicked: () -> Unit,
    val onPositionTypeSelected: (UiConditionPositionTypeDropdownItem) -> Unit,
)

fun IncludeStaticOrCounterSelectionBinding.setup(
    dropdownItems: List<UiCounterOperatorDropdownItem>,
    onOperatorSelected: (UiCounterOperatorDropdownItem) -> Unit,
    onChangeTypeClicked: (UiOperandType) -> Unit,
    onStaticValueChangedListener: (Double) -> Unit,
    onOpenCounterSelectionClicked: () -> Unit,
    onItemBound: ((UiCounterOperatorDropdownItem, View?) -> Unit)? = null,
    conditionOperandConfig: ConditionOperandConfig? = null,
) {
    operatorField.setItems(
        label = root.context.getString(R.string.dropdown_comparison_operator_label),
        items = dropdownItems,
        onItemSelected = onOperatorSelected,
        onItemBound = onItemBound,
    )

    valueTypeMultiStateButton.apply {
        setup(
            MultiStateButtonConfig(
                icons = buildList {
                    add(R.drawable.ic_numbers)
                    add(R.drawable.ic_change_counter)
                    if (conditionOperandConfig != null) add(R.drawable.ic_screen_event)
                },
                singleSelection = true,
                selectionRequired = true,
            )
        )

        setOnCheckedListener { checkedId ->
            onChangeTypeClicked(
                when (checkedId) {
                    0 -> UiOperandType.STATIC
                    1 -> UiOperandType.COUNTER
                    2 -> UiOperandType.CONDITION
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

    conditionOperandConfig ?: return
    conditionField.setOnClickListener(conditionOperandConfig.onOpenConditionSelectionClicked)
    conditionPositionTypeField.setItems(
        label = root.context.getString(R.string.dropdown_condition_position_type_label),
        items = allConditionPositionTypeDropdownItems(),
        onItemSelected = conditionOperandConfig.onPositionTypeSelected,
    )
}

fun IncludeStaticOrCounterSelectionBinding.setSelectedOperator(item: UiCounterOperatorDropdownItem) {
    operatorField.setSelectedItem(item)
}

/** Show or hide the condition operand type. Only relevant if setup was called with a [ConditionOperandConfig]. */
fun IncludeStaticOrCounterSelectionBinding.setConditionOperandAvailable(isAvailable: Boolean) {
    valueTypeMultiStateButton.buttonRight.visibility = if (isAvailable) View.VISIBLE else View.GONE
}

/**
 * Update the views with the provided operand value.
 *
 * @param uiState the operand value to display.
 * @param conditionBitmapProvider provides the bitmap of an image condition operand.
 *
 * @return the job loading the image condition bitmap, if any.
 */
fun IncludeStaticOrCounterSelectionBinding.setValueInfo(
    uiState: UiStaticOrCounterSelection,
    conditionBitmapProvider: ((ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?)? = null,
): Job? {
    when (uiState) {
        is UiStaticOrCounterSelection.CounterValue -> {
            valueTypeMultiStateButton.setChecked(1)

            staticValueLayout.root.visibility = View.GONE
            counterValueLayout.root.visibility = View.VISIBLE
            conditionValueLayout.visibility = View.GONE

            counterValueLayout.setCounter(uiState)
        }

        is UiStaticOrCounterSelection.StaticValue -> {
            valueTypeMultiStateButton.setChecked(0)

            staticValueLayout.root.visibility = View.VISIBLE
            counterValueLayout.root.visibility = View.GONE
            conditionValueLayout.visibility = View.GONE

            staticValueLayout.setText(
                uiState.value.toNaturalDisplayString(),
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            )
        }

        is UiStaticOrCounterSelection.ConditionValue -> {
            valueTypeMultiStateButton.setChecked(2)

            staticValueLayout.root.visibility = View.GONE
            counterValueLayout.root.visibility = View.GONE
            conditionValueLayout.visibility = View.VISIBLE

            conditionPositionTypeField.setSelectedItem(uiState.positionType)
            return setCondition(uiState, conditionBitmapProvider)
        }
    }

    return null
}

private fun IncludeStaticOrCounterSelectionBinding.setCondition(
    uiState: UiStaticOrCounterSelection.ConditionValue,
    bitmapProvider: ((ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?)?,
): Job? {
    val context = root.context
    val condition = uiState.condition

    conditionField.setError(condition == null)
    conditionField.chevron.visibility = if (uiState.hasAvailableConditions) View.VISIBLE else View.GONE

    if (condition == null) {
        val (titleRes, descRes) =
            if (uiState.hasAvailableConditions) R.string.field_counter_operation_condition_selection_title_empty to
                    R.string.field_counter_operation_condition_selection_desc_empty
            else R.string.field_counter_operation_condition_selection_title_none to
                    R.string.field_counter_operation_condition_selection_desc_none

        conditionField.setTitle(context.getString(titleRes))
        conditionField.setDescription(context.getString(descRes))
        conditionField.setImageDrawable(null)
        return null
    }

    conditionField.setTitle(condition.name)
    conditionField.setDescription(context.getString(R.string.field_counter_operation_condition_selection_desc))

    // The bitmap is reloaded on each ui state update, keep the current icon until then to avoid flickering
    if (condition is ScreenCondition.Image && bitmapProvider != null) {
        return bitmapProvider(condition) { bitmap ->
            if (bitmap != null) conditionField.setIconBitmap(bitmap)
        }
    }

    conditionField.setImageDrawable(ContextCompat.getDrawable(context, condition.getTypeIconRes()))
    return null
}

private fun ScreenCondition.getTypeIconRes(): Int =
    when (this) {
        is ScreenCondition.Color -> R.drawable.ic_color_condition
        is ScreenCondition.Image -> R.drawable.ic_image_condition
        is ScreenCondition.Number -> R.drawable.ic_number_condition
        is ScreenCondition.Text -> R.drawable.ic_text_condition
    }

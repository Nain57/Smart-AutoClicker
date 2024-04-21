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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.setTextValue
import com.buzbuz.smartautoclicker.core.ui.bindings.setup
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionCounterBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.counter.CounterNameSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class CounterReachedConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: CounterReachedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterReachedConditionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionCounterBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionCounterBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_counter_reached)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        listener.onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        listener.onConfirmClicked()
                        back()
                    }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        listener.onDeleteClicked()
                        back()
                    }
                }
            }

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editNameLayout.textField)

            editCounterNameLayout.apply {
                setup(R.string.input_field_label_change_counter_name, R.drawable.ic_search, false)
                setOnTextChangedListener { viewModel.setCounterName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
                setOnCheckboxClickedListener { showCounterSelectionDialog() }
            }
            hideSoftInputOnFocusLoss(editCounterNameLayout.textField)

            comparisonOperatorField.setItems(
                label = context.getString(R.string.dropdown_label_comparison_operator),
                items = viewModel.operatorDropdownItems,
                onItemSelected = viewModel::setComparisonOperator,
            )

            editValueLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(0, Int.MAX_VALUE))
                setLabel(R.string.input_field_label_comparison_value)
                setOnTextChangedListener {
                    viewModel.setComparisonValue(if (it.isNotEmpty()) it.toString().toInt() else null)
                }
            }
            hideSoftInputOnFocusLoss(editValueLayout.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingCondition.collect(::onConditionEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(viewBinding.editNameLayout::setText) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.counterName.collect(viewBinding.editCounterNameLayout::setTextValue) }
                launch { viewModel.counterNameError.collect(viewBinding.editCounterNameLayout::setError) }
                launch { viewModel.operatorDropdownState.collect(viewBinding.comparisonOperatorField::setSelectedItem) }
                launch { viewModel.comparisonValueText.collect(::updateComparisonValue) }
                launch { viewModel.conditionCanBeSaved.collect(::updateSaveButton) }
            }
        }
    }

    private fun updateSaveButton(canBeSaved: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, canBeSaved)
    }

    private fun updateComparisonValue(newValue: String?) {
        viewBinding.editValueLayout.setText(newValue, InputType.TYPE_CLASS_NUMBER)
    }

    private fun showCounterSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterNameSelectionDialog(viewModel::setCounterName),
            hideCurrent = true,
        )
    }

    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) finish()
    }
}
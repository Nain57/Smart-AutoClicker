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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter

import android.text.InputFilter
import android.text.InputType
import android.util.Log
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
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionChangeCounterBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.counter.CounterNameSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ChangeCounterDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ChangeCounterViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { changeCounterViewModel() }
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionChangeCounterBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionChangeCounterBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_change_counter)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        listener.onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
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

            operatorField.setItems(
                label = context.getString(R.string.dropdown_label_comparison_operator),
                items = viewModel.operatorDropdownItems,
                onItemSelected = viewModel::setOperationItem,
            )

            editValueLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(0, Int.MAX_VALUE))
                setLabel(R.string.input_field_label_change_counter_operation_value)
                setOnTextChangedListener {
                    viewModel.setOperationValue(if (it.isNotEmpty()) it.toString().toInt() else null)
                }
            }
            hideSoftInputOnFocusLoss(editValueLayout.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingAction.collect(::onActionEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(viewBinding.editNameLayout::setText) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError) }
                launch { viewModel.counterName.collect(viewBinding.editCounterNameLayout::setTextValue) }
                launch { viewModel.counterNameError.collect(viewBinding.editCounterNameLayout::setError) }
                launch { viewModel.operatorDropdownState.collect(viewBinding.operatorField::setSelectedItem) }
                launch { viewModel.valueText.collect(::updateCounterValue) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        debounceUserInteraction {
            listener.onConfirmClicked()
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        debounceUserInteraction {
            listener.onDeleteClicked()
            back()
        }
    }

    private fun updateCounterValue(newValue: String?) {
        viewBinding.editValueLayout.setText(newValue, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSaveButton(isValidAction: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidAction)
    }

    private fun showCounterSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterNameSelectionDialog(viewModel::setCounterName),
            hideCurrent = true,
        )
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ChangeCounterDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "ChangeCounterDialog"
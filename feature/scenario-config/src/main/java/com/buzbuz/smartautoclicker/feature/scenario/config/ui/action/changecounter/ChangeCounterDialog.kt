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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.changecounter

import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState

import com.buzbuz.smartautoclicker.core.ui.bindings.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setIcons
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.setTextValue
import com.buzbuz.smartautoclicker.core.ui.bindings.setup
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigActionChangeCounterBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ChangeCounterDialog(
    private val onConfirmClicked: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ChangeCounterViewModel by viewModels()
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionChangeCounterBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionChangeCounterBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_change_counter)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { debounceUserInteraction { onConfirmClicked() } }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { debounceUserInteraction { onDeleteClicked() } }
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
            }
            hideSoftInputOnFocusLoss(editCounterNameLayout.textField)

            buttonsCounterOperation.apply {
                setIcons(listOf(R.drawable.ic_add, R.drawable.ic_minus, R.drawable.ic_equals), selectionRequired = true)
                setOnCheckedListener(viewModel::setOperationCheckedButtonId)
            }

            editCounterChangeValue.apply {
                textField.filters = arrayOf(MinMaxInputFilter(0, Int.MAX_VALUE))
                setLabel(R.string.input_field_label_change_counter_operation_value)
                setOnTextChangedListener {
                    viewModel.setOperationValue(if (it.isNotEmpty()) it.toString().toInt() else null)
                }
            }
            hideSoftInputOnFocusLoss(editCounterChangeValue.textField)
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
                launch { viewModel.counterOperationCheckedId.collect(viewBinding.buttonsCounterOperation::setChecked) }
                launch { viewModel.valueText.collect(viewBinding.editCounterChangeValue::setText) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun updateSaveButton(isValidAction: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidAction)
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ChangeCounterDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "ChangeCounterDialog"
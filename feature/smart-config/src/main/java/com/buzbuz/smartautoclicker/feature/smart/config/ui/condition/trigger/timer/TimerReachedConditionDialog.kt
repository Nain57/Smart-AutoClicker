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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer

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
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.timeUnitDropdownItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionTimerBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class TimerReachedConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: TimerReachedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { timerReachedConditionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionTimerBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionTimerBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_timer_reached)

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

            editDurationLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(min = 1))
                setLabel(R.string.input_field_label_timer_duration_no_unit)
                setOnTextChangedListener {
                    viewModel.setDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }
            hideSoftInputOnFocusLoss(editDurationLayout.textField)

            timeUnitField.setItems(
                label = context.getString(R.string.dropdown_label_time_unit),
                items = timeUnitDropdownItems,
                onItemSelected = viewModel::setTimeUnit,
            )

            checkboxResetWhenReached.setOnClickListener {
                viewModel.toggleRestartWhenReached()
            }
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
                launch { viewModel.duration.collect(::updateDuration) }
                launch { viewModel.durationError.collect(viewBinding.editDurationLayout::setError)}
                launch { viewModel.selectedUnitItem.collect(viewBinding.timeUnitField::setSelectedItem) }
                launch { viewModel.restartWhenReached.collect(viewBinding.checkboxResetWhenReached::setChecked) }
                launch { viewModel.conditionCanBeSaved.collect(::updateSaveButton) }
            }
        }
    }

    private fun updateDuration(newDuration: String?) {
        viewBinding.editDurationLayout.setText(newDuration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSaveButton(canBeSaved: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, canBeSaved)
    }

    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) finish()
    }
}
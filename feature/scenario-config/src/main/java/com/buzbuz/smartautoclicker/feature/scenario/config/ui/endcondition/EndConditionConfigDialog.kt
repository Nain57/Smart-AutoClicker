/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.endcondition

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigEndConditionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.updateState
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.setError

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying the end condition configuration.
 **
 * @param context the Android Context for the dialog shown by this controller.
 * @param onConfirmClicked called when the user clicks on confirm.
 * @param onDeleteClicked called when the user clicks on delete.
 */
class EndConditionConfigDialog(
    context: Context,
    private val onConfirmClicked: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
): OverlayDialogController(context, R.style.ScenarioConfigTheme) {

    /** View model for this dialog. */
    private val viewModel: EndConditionConfigModel by lazy { ViewModelProvider(this).get(EndConditionConfigModel::class.java) }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigEndConditionBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigEndConditionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_end_condition_config)
                buttonDismiss.setOnClickListener {
                    onDismissClicked()
                    destroy()
                }

                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }

                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
            }

            editExecutionCountLayout.apply {
                setLabel(R.string.input_field_label_execution_count)
                setOnTextChangedListener {
                    try { viewModel.setExecutions(it.toString().toInt()) }
                    catch (_: java.lang.NumberFormatException) {}
                }
                textField.filters = arrayOf(MinMaxInputFilter(MIN_EXECUTION_COUNT, MAX_EXECUTION_COUNT))
            }
            hideSoftInputOnFocusLoss(editExecutionCountLayout.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventViewState.collect(::updateEvent) }
                launch { viewModel.executionCountError.collect(viewBinding.editExecutionCountLayout::setError) }
                launch { viewModel.executions.collect(::updateExecutionCount) }
                launch { viewModel.endConditionCanBeSaved.collect(::updateSaveButton) }
            }
        }
    }

    /**
     * Called when the ok button is clicked.
     * Propagate the configured event to the provided listener and dismiss the dialog.
     */
    private fun onSaveButtonClicked() {
        onConfirmClicked()
        destroy()
    }

    /**
     * Called when the delete button is clicked.
     * Propagate the configured event to the provided listener and dismiss the dialog.
     */
    private fun onDeleteButtonClicked() {
        onDeleteClicked()
        destroy()
    }

    /** Update the enabled state of the save button. */
    private fun updateSaveButton(isEndConditionValid: Boolean) {
        viewBinding.layoutTopBar.buttonSave.isEnabled = isEndConditionValid
    }

    /** Update the ui state of the selected event for the end condition. */
    private fun updateEvent(viewState: EventPickerViewState) {
        viewBinding.eventPicker.updateState(viewState, ::showEventSelectionDialog)
    }

    /** Update the display of the executions count. */
    private fun updateExecutionCount(count: Int) {
        viewBinding.editExecutionCountLayout.setText(count.toString(), InputType.TYPE_CLASS_NUMBER)
    }

    /** Show the event selection dialog. */
    private fun showEventSelectionDialog(availableEvents: List<Event>) {
        showSubOverlay(
            EventSelectionDialog(
                context = context,
                eventList = availableEvents,
                onEventClicked = { event -> viewModel.setEvent(event) }
            )
        )
    }
}

private const val MIN_EXECUTION_COUNT = 0
private const val MAX_EXECUTION_COUNT = 9999

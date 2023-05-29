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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.toggleevent

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigActionToggleEventBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.EventPickerViewState
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.updateState
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.endcondition.EventSelectionDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.setError

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ToggleEventDialog(
    context: Context,
    private val onConfirmClicked: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialogController(context, R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ToggleEventViewModel by lazy {
        ViewModelProvider(this).get(ToggleEventViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionToggleEventBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionToggleEventBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_toggle_event)

                buttonDismiss.setOnClickListener {
                    onDismissClicked()
                    destroy()
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

            toggleTypeField.setItems(
                label = context.getString(R.string.input_field_toggle_event_type),
                items = viewModel.toggleStateItems,
                onItemSelected = viewModel::setToggleType,
            )

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editNameLayout.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateToggleEventName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError) }
                launch { viewModel.toggleStateItem.collect(::updateToggleType) }
                launch { viewModel.eventViewState.collect(::updateEvent) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        onConfirmClicked()
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked()
        destroy()
    }

    private fun updateToggleEventName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateToggleType(operatorItem: DropdownItem) {
        viewBinding.toggleTypeField.setSelectedItem(operatorItem)
    }

    private fun updateEvent(viewState: EventPickerViewState) {
        viewBinding.eventPicker.updateState(viewState, ::showEventSelectionDialog)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.SAVE, isValidCondition)
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
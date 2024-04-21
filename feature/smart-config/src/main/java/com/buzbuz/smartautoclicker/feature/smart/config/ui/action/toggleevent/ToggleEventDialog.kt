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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent

import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setIcons
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionToggleEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_ENABLED_ITEM

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ToggleEventDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ToggleEventViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { toggleEventViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionToggleEventBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionToggleEventBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_toggle_event)

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

            toggleAllButton.apply {
                setIcons(listOf(R.drawable.ic_confirm, R.drawable.ic_invert, R.drawable.ic_cancel))
                setOnCheckedListener(viewModel::setToggleAllType)
            }

            layoutEventToggles.setOnClickListener { showEventTogglesDialog() }
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
                launch { viewModel.name.collect(::updateToggleEventName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError) }
                launch { viewModel.toggleAllEnabledButton.collect(::updateToggleAllButton) }
                launch { viewModel.eventToggleSelectorState.collect(::updateEventToggleSelector) }
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

    private fun updateToggleEventName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun updateToggleAllButton(buttonState: ToggleAllButtonState) {
        viewBinding.apply {
            toggleAllButton.setChecked(buttonState.checkedButton)
            toggleAllSubtext.setText(buttonState.descriptionText)

            val isToggleSelectionEnabled = buttonState.checkedButton == null
            layoutEventToggles.alpha = if (isToggleSelectionEnabled) ALPHA_ENABLED_ITEM else ALPHA_DISABLED_ITEM
            layoutEventToggles.isEnabled = isToggleSelectionEnabled
        }
    }

    private fun updateEventToggleSelector(state: EventToggleSelectorState) {
        viewBinding.apply {
            toggleEventsTitle.text = state.title

            if (state.emptyText != null) {
                togglesCountLayout.visibility = View.GONE
                toggleEventsSubtext.visibility = View.VISIBLE
                toggleEventsSubtext.setText(state.emptyText)
            } else {
                toggleEventsSubtext.visibility = View.GONE
                togglesCountLayout.visibility = View.VISIBLE
                textEnableCount.text = state.enableCount.toString()
                textToggleCount.text = state.toggleCount.toString()
                textDisableCount.text = state.disableCount.toString()
            }
        }
    }

    /** Show the event selection dialog. */
    private fun showEventTogglesDialog() =
        overlayManager.navigateTo(
            context = context,
            newOverlay = EventTogglesDialog(
                onConfirmClicked = viewModel::setNewEventToggles,
            )
        )

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ToggleEventDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "ToggleEventDialog"
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

import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChildrenIcons
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChildrenTexts
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionToggleEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener

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
                dialogTitle.setText(R.string.dialog_title_toggle_event)

                buttonDismiss.setDebouncedOnClickListener {
                    listener.onDismissClicked()
                    back()
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteButtonClicked() }
                }
            }

            fieldName.apply {
                setLabel(R.string.generic_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldName.textField)

            fieldMultiStateToggleAll.apply {
                setTitle(context.getString(R.string.field_change_all_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_change_all_desc_manual),
                        context.getString(R.string.field_change_all_desc_enable_all),
                        context.getString(R.string.field_change_all_desc_invert_all),
                        context.getString(R.string.field_change_all_desc_disable_all),
                    )
                )
                setButtonConfig(
                    MultiStateButtonConfig(
                        icons = listOf(R.drawable.ic_confirm, R.drawable.ic_invert, R.drawable.ic_cancel),
                        selectionRequired = false,
                        singleSelection = true,
                    )
                )
                setOnCheckedListener(viewModel::setToggleAllType)
            }

            fieldSelectionToggles.apply {
                setChildrenIcons(listOf(R.drawable.ic_confirm, R.drawable.ic_invert, R.drawable.ic_cancel))
                setOnClickListener { debounceUserInteraction { showEventTogglesDialog() } }
            }
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
                launch { viewModel.nameError.collect(viewBinding.fieldName::setError) }
                launch { viewModel.toggleAllButtonCheckIndex.collect(::updateToggleAllField) }
                launch { viewModel.eventToggleSelectorState.collect(::updateEventToggleSelector) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        listener.onConfirmClicked()
        back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        back()
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun updateToggleEventName(newName: String?) {
        viewBinding.fieldName.setText(newName)
    }

    private fun updateToggleAllField(checkedButtonIndex: Int?) {
        viewBinding.fieldMultiStateToggleAll.apply {
            setChecked(checkedButtonIndex)
            setDescription(
                if (checkedButtonIndex == null) 0
                else checkedButtonIndex + 1
            )
        }
    }

    private fun updateEventToggleSelector(state: EventToggleSelectorState) {
        viewBinding.fieldSelectionToggles.apply {
            setTitle(state.title)
            setEnabled(state.isEnabled)

            val emptyTextRes = state.emptyText
            if (emptyTextRes != null) {
                setDescription(context.getString(emptyTextRes))
                return
            }

            setChildrenTexts(
                listOf(state.enableCount.toString(), state.toggleCount.toString(), state.disableCount.toString())
            )
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
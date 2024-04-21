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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast

import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
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
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionBroadcastBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.intent.IntentActionsSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class BroadcastReceivedConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: BroadcastReceivedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { broadcastReceivedConditionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionBroadcastBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionBroadcastBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_broadcast_received)

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

            editBroadcastAction.apply {
                setup(R.string.input_field_label_broadcast_action, R.drawable.ic_search, false)
                setOnTextChangedListener { viewModel.setIntentAction(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
                setOnCheckboxClickedListener { showBroadcastActionSelectionDialog() }
            }
            hideSoftInputOnFocusLoss(editBroadcastAction.textField)
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
                launch { viewModel.intentAction.collect(viewBinding.editBroadcastAction::setTextValue) }
                launch { viewModel.intentActionError.collect(viewBinding.editBroadcastAction::setError)}
                launch { viewModel.conditionCanBeSaved.collect(::updateSaveButton) }
            }
        }
    }

    private fun updateSaveButton(canBeSaved: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, canBeSaved)
    }

    private fun showBroadcastActionSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = IntentActionsSelectionDialog(
                currentAction = viewModel.getIntentAction(),
                onConfigComplete = { newAction ->
                    viewModel.setIntentAction(newAction ?: "")
                    viewBinding.editBroadcastAction.textField.setText(newAction)
                },
                forBroadcastReception = true,
            ),
            hideCurrent = true,
        )
    }

    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) finish()
    }
}
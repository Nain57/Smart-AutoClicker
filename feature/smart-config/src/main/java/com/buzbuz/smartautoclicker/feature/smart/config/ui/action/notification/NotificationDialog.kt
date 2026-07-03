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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification

import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTextValue
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionNotificationBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.newNotificationSettingsStarterOverlay
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class NotificationDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.NOTIFICATION.name

    /** The view model for this dialog. */
    private val viewModel: NotificationViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { notificationViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionNotificationBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionNotificationBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_notification)

                buttonDismiss.setDebouncedOnClickListener { back() }
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
                textField.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length)))
                setOnTextChangedListener { viewModel.setName(it.toString()) }
            }
            hideSoftInputOnFocusLoss(fieldName.textField)

            fieldTextToWrite.apply {
                setup(
                    label = R.string.field_notification_message_text_label,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                textField.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(300))
                setOnTextChangedListener { text ->
                    viewModel.setNotificationMessage(text.toString())
                }
                setOnCheckboxClickedListener {
                    showCounterSelectionDialog { selectedCounter ->
                        setTextValue(
                            textField.text.toString().appendCounterReference(
                                counterName = selectedCounter,
                                atIndex = textField.selectionEnd,
                            ),
                            force = true,
                        )
                    }
                }
            }

            fieldDropdownChannelType.setItems(
                label = context.getString(R.string.field_dropdown_notification_importance_title),
                items = notificationImportanceItems,
                onItemSelected = viewModel::setNotificationImportance,
            )

            buttonNotificationSettings.apply {
                visibility = if (viewModel.shouldShowSettingsButton()) View.VISIBLE else View.GONE
                setDebouncedOnClickListener { showNotificationSettings() }
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
                launch { viewModel.uiState.collect(::onUiStateUpdated) }
            }
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }

        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveButtonClicked() {
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun onUiStateUpdated(uiState: NotificationDialogUiState?) {
        uiState ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeSaved)

            fieldName.setText(uiState.name)
            fieldTextToWrite.setTextValue(uiState.message)
            fieldDropdownChannelType.setSelectedItem(uiState.importance)
        }
    }

    private fun showCounterSelectionDialog(onSelected: (String) -> Unit) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog(onSelected),
            hideCurrent = true,
        )
    }

    private fun showNotificationSettings() {
        if (!viewModel.shouldShowSettingsButton()) return

        overlayManager.navigateTo(
            context = context,
            newOverlay = newNotificationSettingsStarterOverlay(),
            hideCurrent = true,
        )
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing PauseDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "PauseDialog"

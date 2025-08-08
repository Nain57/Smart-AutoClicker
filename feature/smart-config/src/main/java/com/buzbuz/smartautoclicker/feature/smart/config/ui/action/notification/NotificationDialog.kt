
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification

import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
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
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.counter.CounterNameSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.newNotificationSettingsStarterOverlay

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class NotificationDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

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

            fieldDropdownMessageType.setItems(
                label = context.getString(R.string.field_dropdown_notification_message_type_title),
                items = notificationMessageTypeItems,
                onItemSelected = viewModel::setNotificationMessageType,
            )

            fieldMessageText.apply {
                setLabel(R.string.field_notification_message_text_label)
                textField.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length)))
                setOnTextChangedListener { viewModel.setNotificationMessage(it.toString()) }
            }

            fieldMessageCounterName.apply {
                setup(
                    label = R.string.field_notification_message_counter_label,
                    icon = R.drawable.ic_search,
                    disableInputWithCheckbox = false,
                )
                setOnCheckboxClickedListener { showCounterSelectionDialog() }
                setOnTextChangedListener { viewModel.setNotificationMessageCounterName(it.toString()) }
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
                launch { viewModel.name.collect(viewBinding.fieldName::setText) }
                launch { viewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { viewModel.notificationMessage.collect(::updateMessageCard) }
                launch { viewModel.importanceItem.collect(viewBinding.fieldDropdownChannelType::setSelectedItem) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
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

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun updateMessageCard(uiState: UiNotificationMessage) {
        when (uiState.typeItem) {
            NotificationMessageTypeItem.Text -> {
                viewBinding.fieldMessageText.apply {
                    if (root.visibility != View.GONE) return@apply

                    root.visibility = View.VISIBLE
                    setText(uiState.messageContent)
                }

                viewBinding.fieldMessageCounterName.root.visibility = View.GONE
            }

            NotificationMessageTypeItem.Counter -> {
                viewBinding.fieldMessageCounterName.apply {
                    if (root.visibility != View.GONE) return@apply

                    root.visibility = View.VISIBLE
                    setTextValue(uiState.messageContent)
                }

                viewBinding.fieldMessageText.root.visibility = View.GONE
            }
        }

        viewBinding.fieldDropdownMessageType.setSelectedItem(uiState.typeItem)
    }


    private fun showCounterSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterNameSelectionDialog { counterName ->
                viewModel.setNotificationMessageCounterName(counterName)
                viewBinding.fieldMessageCounterName.setTextValue(counterName)
            },
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
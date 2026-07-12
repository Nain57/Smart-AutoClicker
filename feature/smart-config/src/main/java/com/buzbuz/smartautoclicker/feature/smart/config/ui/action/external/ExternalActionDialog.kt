/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.external

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
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionExternalActionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ExternalActionDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ExternalActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { externalActionViewModel() },
    )
    private lateinit var viewBinding: DialogConfigActionExternalActionBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionExternalActionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_external_action)
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
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldName.textField)

            fieldExternalActionName.apply {
                setLabel(R.string.field_external_action_name_label)
                setOnTextChangedListener { viewModel.setExternalActionName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldExternalActionName.textField)

            buttonSelectExternalAction.setDebouncedOnClickListener { showExternalActionSelectionDialog() }
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
                launch { viewModel.uiState.collect(::updateUiState) }
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

    private fun updateUiState(state: ExternalActionUiState?) {
        state ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)
            fieldName.setText(state.name)
            fieldName.setError(state.nameError)
            fieldExternalActionName.setText(state.externalActionName)
            fieldExternalActionName.setError(state.externalActionNameError)
        }
    }

    private fun showExternalActionSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ExternalActionSelectionDialog { selectedName ->
                viewModel.setExternalActionName(selectedName)
            },
            hideCurrent = true,
        )
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ExternalActionDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "ExternalActionDialog"

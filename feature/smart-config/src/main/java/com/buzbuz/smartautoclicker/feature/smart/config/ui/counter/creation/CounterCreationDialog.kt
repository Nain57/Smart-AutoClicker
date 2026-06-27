/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogCounterCreationBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

import kotlin.getValue

class CounterCreationDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** View model for this dialog. */
    private val viewModel: CountersCreationViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterCreationViewModel() },
    )

    private lateinit var viewBinding: DialogCounterCreationBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogCounterCreationBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_new_counter)

                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DISMISS, View.VISIBLE)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.setDebouncedOnClickListener {
                    viewModel.createCounter()
                    back()
                }
            }

            fieldName.root.hint = context.getString(R.string.generic_name)
            fieldName.textField.doAfterTextChanged { viewModel.setName(it.toString()) }

            fieldStartingValue.root.hint = context.getString(R.string.field_new_counter_starting_value)
            fieldStartingValue.textField.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            fieldStartingValue.textField.setText("0")
            fieldStartingValue.textField.doAfterTextChanged {
                viewModel.setStartingValue(it.toString().toDoubleOrNull() ?: 0.0)
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUi) }
            }
        }
    }

    private fun updateUi(uiState: CounterCreationUiState?) {
        uiState ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeSaved)
            fieldName.setError(
                messageId = R.string.field_counter_name_already_declared,
                isError = uiState.nameError,
            )
        }
    }
}

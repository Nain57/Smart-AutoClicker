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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter

import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionCounterBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter.setCounter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter.setOnClickListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter.setSelectedOperator
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter.setValueInfo
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.counter.setup
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.allCounterComparisonOperatorDropdownItems
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class CounterReachedConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_REACHED_CONDITION.name

    /** The view model for this dialog. */
    private val viewModel: CounterReachedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterReachedConditionViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionCounterBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionCounterBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_counter_reached)

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

            counterToCheck.setOnClickListener {
                showCounterSelectionDialog { counter  ->
                    viewModel.setCounterName(counter)
                }
            }

            editValueLayout.apply {
                setup(
                    dropdownItems = allCounterComparisonOperatorDropdownItems(),
                    onOperatorSelected = viewModel::setOperationItem,
                    onChangeTypeClicked = viewModel::setOperandType,
                    onStaticValueChangedListener = { newValue ->
                        viewModel.setOperationValue(CounterOperationValue.Number(newValue))
                    },
                    onOpenCounterSelectionClicked = {
                        showCounterSelectionDialog { counterSelected ->
                            viewModel.setOperationValue(CounterOperationValue.Counter(counterSelected))
                        }
                    },
                )
                hideSoftInputOnFocusLoss(staticValueLayout.textField)
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
                viewModel.uiState.collect(::updateUiState)
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

    private fun updateUiState(state: CounterReachedConditionUiState?) {
        state ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)

            fieldName.setText(state.name)
            fieldName.setError(state.nameError)

            counterToCheck.setCounter(state.counter)
            editValueLayout.setSelectedOperator(state.operator)
            editValueLayout.setValueInfo(state.operandValue)

            effectDesc.text = state.conditionEffectText
        }
    }

    private fun showCounterSelectionDialog(onCounterSelected: (String) -> Unit) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog(onCounterSelected),
            hideCurrent = true,
        )
    }

    private fun onConditionEditingStateChanged(isEditingCondition: Boolean) {
        if (!isEditingCondition) {
            Log.e(TAG, "Closing dialog because there is no condition edited")
            finish()
        }
    }
}

private const val TAG = "CounterReachedConditionDialog"

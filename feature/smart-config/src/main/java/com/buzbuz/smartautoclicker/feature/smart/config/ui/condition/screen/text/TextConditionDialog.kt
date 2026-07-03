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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text

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
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnValueChangedFromUserListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setSliderRange
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setSliderValue
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setValueLabelState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionTextBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector.ConditionAreaSelectorMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.MAX_THRESHOLD
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.selection.AlphabetSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.roundToInt
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class TextConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TEXT_CONDITION.name

    /** The view model for this dialog. */
    private val viewModel: TextConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { textConditionViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionTextBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionTextBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_condition_config)

                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener {
                        listener.onConfirmClicked()
                        super.back()
                    }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteClicked() }
                }
            }

            fieldEditName.apply {
                setLabel(R.string.generic_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldEditName.textField)

            fieldTextToSearch.apply {
                setLabel(R.string.field_text_to_detect_label)
                textField.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(
                    context.resources.getInteger(R.integer.text_condition_max_length)
                ))
                setOnTextChangedListener { viewModel.setTextToDetect(it.toString()) }
            }
            hideSoftInputOnFocusLoss(fieldEditName.textField)

            fieldAlphabet.apply {
                setTitle(context.getString(R.string.field_text_detection_alphabet_title))
                setOnClickListener { showAlphabetSelectionDialog() }
            }

            fieldSelectArea.apply {
                setTitle(context.getString(R.string.generic_detection_area_title))
                setOnClickListener { showDetectionAreaSelector() }
            }

            fieldShouldAppear.apply {
                setTitle(context.getString(R.string.field_condition_visibility_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_condition_visibility_desc_absent),
                        context.getString(R.string.field_condition_visibility_desc_present),
                    )
                )
                setOnClickListener { viewModel.toggleShouldBeDetected() }
            }

            fieldSliderThreshold.apply {
                setTitle(context.getString(R.string.generic_condition_threshold_title))
                setValueLabelState(isEnabled = true, prefix = "%")
                setSliderRange(0f, MAX_THRESHOLD)
                setOnValueChangedFromUserListener { value -> viewModel.setThreshold(value.roundToInt()) }
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
                launch { viewModel.uiState.collect(::updateUi) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorSaveButtonView(viewBinding.layoutTopBar.buttonSave)
        viewModel.monitorDetectionAreaSelectorView(viewBinding.fieldSelectArea.root)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
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

    private fun updateUi(uiState: TextConditionUiState?) {
        if (uiState == null) return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeSaved)
            if (fieldEditName.textField.text.isNullOrEmpty()) fieldEditName.setText(uiState.name)
            fieldEditName.setError(uiState.nameError)

            if (fieldTextToSearch.textField.text.isNullOrEmpty()) fieldTextToSearch.setText(uiState.textToSearch)

            fieldAlphabet.setDescription(uiState.alphabetDesc)
            fieldSelectArea.setDescription(uiState.detectionAreaDescription)
            fieldSelectArea.setError(uiState.detectionAreaError)
            fieldShouldAppear.setChecked(uiState.shouldBeDetectedChecked)
            fieldShouldAppear.setDescription(if (uiState.shouldBeDetectedChecked) 1 else 0)
            fieldSliderThreshold.setSliderValue(uiState.detectionThreshold.toFloat())
        }
    }

    private fun onDeleteClicked() {
        if (viewModel.isConditionRelatedToClick()) {
            context.showDeleteConditionsWithAssociatedActionsDialog { onConfirmDelete() }
            return
        }

        onConfirmDelete()
    }

    private fun onConfirmDelete() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) {
            Log.e(TAG, "Closing ConditionDialog because there is no condition edited")
            finish()
        }
    }

    private fun showDetectionAreaSelector() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionAreaSelectorMenu(
                onAreaSelected = viewModel::setDetectionArea,
            ),
            hideCurrent = true,
        )
    }

    private fun showAlphabetSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = AlphabetSelectionDialog(),
            hideCurrent = true,
        )
    }
}

private const val TAG = "TextConditionDialog"

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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image

import android.graphics.Bitmap
import android.graphics.Color
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckedListener
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
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionImageBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.ScreenConditionAreaSelectorMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common.DetectionTypeState

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ImageConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ImageConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionImageBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionImageBinding.inflate(LayoutInflater.from(context)).apply {
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

            fieldDetectionType.apply {
                setTitle(context.getString(R.string.field_detection_type_title))
                setButtonConfig(
                    MultiStateButtonConfig(
                        icons = listOf(
                            R.drawable.ic_detect_exact,
                            R.drawable.ic_detect_whole_screen,
                            R.drawable.ic_detect_in_area,
                        ),
                        selectionRequired = true,
                    )
                )
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_detection_type_desc_exact),
                        context.getString(R.string.field_detection_type_desc_screen),
                        context.getString(R.string.field_select_detection_area_title),
                    )
                )
                setOnCheckedListener { index -> viewModel.setDetectionType(index.fromIndexToDetectionType())}
            }

            fieldSelectArea.apply {
                setTitle(context.getString(R.string.field_select_detection_area_title))
                setOnClickListener { debounceUserInteraction { showDetectionAreaSelector() } }
            }

            fieldSliderThreshold.apply {
                setTitle(context.getString(R.string.field_title_condition_threshold))
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
                launch { viewModel.name.collect(::updateConditionName) }
                launch { viewModel.nameError.collect(viewBinding.fieldEditName::setError) }
                launch { viewModel.conditionBitmap.collect(::updateConditionBitmap) }
                launch { viewModel.shouldBeDetected.collect(::updateShouldBeDetected) }
                launch { viewModel.detectionType.collect(::updateDetectionType) }
                launch { viewModel.threshold.collect(::updateThreshold) }
                launch { viewModel.conditionCanBeSaved.collect(::updateSaveButton) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorSaveButtonView(viewBinding.layoutTopBar.buttonSave)
        viewModel.monitorDetectionTypeItemWholeScreenView(viewBinding.fieldDetectionType.multiStateButton.buttonMiddle)
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

    private fun onDeleteClicked() {
        if (viewModel.isConditionRelatedToClick()) {
            context.showDeleteConditionsWithAssociatedActionsDialog { confirmDelete() }
            return
        }

        confirmDelete()
    }

    private fun updateConditionName(newName: String?) {
        viewBinding.fieldEditName.setText(newName)
    }

    private fun updateConditionBitmap(newBitmap: Bitmap?) {
        if (newBitmap != null) {
            viewBinding.imageCondition.setImageBitmap(newBitmap)
        } else {
            viewBinding.imageCondition.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                    setTint(Color.RED)
                }
            )
        }
    }

    private fun updateShouldBeDetected(newValue: Boolean) {
        viewBinding.fieldShouldAppear.apply {
            setChecked(newValue)
            setDescription(if (newValue) 1 else 0)
        }
    }

    private fun updateDetectionType(detectionTypeState: DetectionTypeState) {
        val index = when (detectionTypeState.type) {
            EXACT -> 0
            WHOLE_SCREEN -> 1
            IN_AREA -> 2
            else -> return
        }

        viewBinding.fieldDetectionType.apply {
            setChecked(index)
            setDescription(index)
        }

        viewBinding.fieldSelectArea.apply {
            setEnabled(detectionTypeState.type == IN_AREA)
            setDescription(detectionTypeState.areaText)
        }
    }

    private fun updateThreshold(newThreshold: Int) {
        viewBinding.fieldSliderThreshold.setSliderValue(newThreshold.toFloat())
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showDetectionAreaSelector() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScreenConditionAreaSelectorMenu(
                onAreaSelected = viewModel::setDetectionArea,
            ),
            hideCurrent = true,
        )
    }

    private fun confirmDelete() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun onConditionEditingStateChanged(isEditingCondition: Boolean) {
        if (!isEditingCondition) {
            Log.e(TAG, "Closing ConditionDialog because there is no condition edited")
            finish()
        }
    }

    private fun Int?.fromIndexToDetectionType() : Int =
        when (this) {
            0 -> EXACT
            1 -> WHOLE_SCREEN
            else -> IN_AREA
        }
}

private const val TAG = "ImageConditionDialog"
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionColorBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture.ColorCaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.rgbToColorInt
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.setGradientBackground
import com.buzbuz.smartautoclicker.core.ui.utils.updateColorIndicatorDrawableColor
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.MAX_THRESHOLD

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.roundToInt
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class ColorConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COLOR_CONDITION.name

    /** The view model for this dialog. */
    private val viewModel: ColorConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { colorConditionViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionColorBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionColorBinding.inflate(LayoutInflater.from(context)).apply {
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

            iconColorValue.setOnClickListener { showPixelColorPickerOverlay() }
            sliderRed.slider.addOnChangeListener(::onColorSliderValueChanged)
            sliderGreen.slider.addOnChangeListener(::onColorSliderValueChanged)
            sliderBlue.slider.addOnChangeListener(::onColorSliderValueChanged)

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
            context.showDeleteConditionsWithAssociatedActionsDialog { onConfirmDelete() }
            return
        }

        onConfirmDelete()
    }

    private fun onConfirmDelete() {
        listener.onDeleteClicked()
        super.back()
    }

    @Suppress("unused")
    private fun onColorSliderValueChanged(view: Slider, value: Float, fromUser: Boolean) {
        if (!fromUser) return

        viewModel.setColor(
            rgbToColorInt(
                red = viewBinding.sliderRed.slider.value.toInt(),
                green = viewBinding.sliderGreen.slider.value.toInt(),
                blue = viewBinding.sliderBlue.slider.value.toInt(),
            )
        )
    }

    private fun updateUi(uiState: ColorConditionUiState?) {
        if (uiState == null) return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, uiState.canBeSaved)

            if (fieldEditName.textField.text.isNullOrEmpty()) fieldEditName.setText(uiState.conditionName)
            fieldEditName.setError(uiState.conditionNameError)
            iconColorValue.updateColorIndicatorDrawableColor(uiState.conditionColor)
            textColorValue.text = uiState.conditionColorText

            updateRGBSliders(uiState)

            fieldShouldAppear.setChecked(uiState.shouldBeDetectedChecked)
            fieldShouldAppear.setDescription(if (uiState.shouldBeDetectedChecked) 1 else 0)

            fieldSliderThreshold.setSliderValue(uiState.detectionThreshold.toFloat())
        }
    }

    private fun updateRGBSliders(uiState: ColorConditionUiState) {
        viewBinding.sliderRed.apply {
            slider.value = uiState.redValue.toFloat()
            slider.thumbTintList =
                ColorStateList.valueOf(Color.rgb(uiState.redValue, 0, 0))
            backgroundGradient.setGradientBackground(
                startColor = Color.rgb(0, uiState.greenValue, uiState.blueValue),
                endColor = Color.rgb(255, uiState.greenValue, uiState.blueValue)
            )
        }

        viewBinding.sliderGreen.apply {
            slider.value = uiState.greenValue.toFloat()
            slider.thumbTintList =
                ColorStateList.valueOf(Color.rgb(0, uiState.greenValue, 0))
            backgroundGradient.setGradientBackground(
                startColor = Color.rgb(uiState.redValue, 0, uiState.blueValue),
                endColor = Color.rgb(uiState.redValue, 255, uiState.blueValue),
            )
        }

        viewBinding.sliderBlue.apply {
            slider.value = uiState.blueValue.toFloat()
            slider.thumbTintList =
                ColorStateList.valueOf(Color.rgb(0 , 0, uiState.blueValue))
            backgroundGradient.setGradientBackground(
                startColor = Color.rgb(uiState.redValue, uiState.greenValue, 0),
                endColor = Color.rgb(uiState.redValue, uiState.greenValue, 255)
            )
        }
    }


    private fun onConditionEditingStateChanged(isEditing: Boolean) {
        if (!isEditing) {
            Log.e(TAG, "Closing ConditionDialog because there is no condition edited")
            finish()
        }
    }

    private fun showPixelColorPickerOverlay() {
        val uiState = viewModel.uiState.value ?: return

        overlayManager.navigateTo(
            context = context,
            newOverlay = ColorCaptureMenu(
                defaultPosition = uiState.conditionPosition,
                onColorSelected = { position, selectedColor ->
                    viewModel.setColor(selectedColor)
                    viewModel.setPosition(position)
                }
            ) ,
            hideCurrent = true,
        )
    }
}

private const val TAG = "ColorConditionDialog"

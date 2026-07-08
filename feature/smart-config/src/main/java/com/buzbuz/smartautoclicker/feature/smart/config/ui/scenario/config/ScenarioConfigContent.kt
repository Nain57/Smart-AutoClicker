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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config

import android.content.Context
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxDoubleInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ContentScenarioConfigBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import com.google.android.material.textfield.TextInputLayout

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ScenarioConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ScenarioConfigViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioConfigViewModel() },
    )

    private lateinit var viewBinding: ContentScenarioConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            fieldScenarioName.apply {
                setLabel(R.string.input_field_label_scenario_name)
                setOnTextChangedListener { viewModel.setScenarioName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(fieldScenarioName.textField)

            fieldAntiDetection.apply {
                setTitle(context.resources.getString(R.string.input_field_label_anti_detection))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.dropdown_helper_text_anti_detection_disabled),
                        context.getString(R.string.dropdown_helper_text_anti_detection_enabled),
                    )
                )
                setOnClickListener(viewModel::toggleRandomization)
            }

            fieldKeepScreenOn.apply {
                setTitle(context.resources.getString(R.string.field_scenario_keep_screen_on_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_scenario_keep_screen_on_disabled),
                        context.getString(R.string.field_scenario_keep_screen_on_enabled),
                    )
                )
                setOnClickListener(viewModel::toggleKeepScreenOn)
            }

            fieldLimitFps.apply {
                setTitle(context.getString(R.string.field_scenario_fps_limit_title))
                setDescription(context.getString(R.string.field_scenario_fps_limit_desc))
                setOnClickListener(viewModel::toggleFpsLimiter)
            }

            editFpsLimit.apply {
                textLayout.endIconMode = TextInputLayout.END_ICON_NONE
                setLabel(R.string.field_scenario_fps_rate_label)
                setOnTextChangedListener {
                    viewModel.setComputeRate(
                        if (it.isNotEmpty()) it.toString().toDoubleOrNull() ?: return@setOnTextChangedListener
                        else return@setOnTextChangedListener
                    )
                }
            }
            dialogController.hideSoftInputOnFocusLoss(editFpsLimit.textField)

            fpsTimeUnitField.setItems(
                label = context.getString(R.string.field_scenario_fps_rate_unit_label),
                items = allComputeRateUnitDropdownItems(),
                onItemSelected = viewModel::setComputeRateUnit,
            )

            textSpeed.setOnClickListener { viewModel.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel.increaseDetectionQuality() }
            seekbarResolution.addOnChangeListener { _, value, fromUser ->
                if (fromUser) viewModel.setDetectionQuality(value.roundToInt())
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState)}
            }
        }
    }

    private fun updateUiState(uiState: ScenarioConfigUiState?) {
        uiState ?: return

        viewBinding.apply {
            fieldScenarioName.setText(uiState.name)

            fieldAntiDetection.setChecked(uiState.randomizeChecked)
            fieldAntiDetection.setDescription(if (uiState.randomizeChecked) 1 else 0)

            fieldKeepScreenOn.setChecked(uiState.keepScreenOnChecked)
            fieldKeepScreenOn.setDescription(if (uiState.keepScreenOnChecked) 1 else 0)
        }

        updateQuality(uiState.qualityUiState)
        updateComputeRate(uiState.computeRateState)
    }

    private fun updateComputeRate(state: ComputeRateLimitUiState) {
        viewBinding.apply {
            fieldLimitFps.setChecked(state.isEnabled)

            val visibility = if (state.isEnabled) View.VISIBLE else View.GONE
            editFpsLimit.root.visibility = visibility
            textSeparator.visibility = visibility
            fpsTimeUnitField.root.visibility = visibility

            editFpsLimit.textLayout.isEnabled = state.isEnabled
            if (state.isEnabled) {
                editFpsLimit.textField.filters = arrayOf(MinMaxDoubleInputFilter(min = 0.0, max = state.maxValue))
                editFpsLimit.setText(state.value.toNaturalDisplayString(), InputType.TYPE_NUMBER_FLAG_DECIMAL)
            } else {
                editFpsLimit.textField.filters = emptyArray<InputFilter>()
                editFpsLimit.setText(context.getString(R.string.field_scenario_fps_limit_disable_rate))
            }

            fpsTimeUnitField.textLayout.isEnabled = state.isEnabled
            fpsTimeUnitField.setSelectedItem(state.unit)
        }
    }

    private fun updateQuality(quality: DetectionQualityUiState) {
        viewBinding.apply {
            textQualityValue.text = quality.displayText

            val isNotInitialized = seekbarResolution.value == 0f
            seekbarResolution.value = quality.qualityValue

            if (isNotInitialized && quality.min < quality.max) {
                seekbarResolution.valueFrom = quality.min
                seekbarResolution.valueTo = quality.max
            }
        }
    }
}

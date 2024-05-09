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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config

import android.content.Context
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ContentScenarioConfigBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

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

            textSpeed.setOnClickListener { viewModel.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel.increaseDetectionQuality() }
            seekbarQuality.addOnChangeListener { _, value, fromUser ->
                if (fromUser) viewModel.setDetectionQuality(value.roundToInt())
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.scenarioName.collect(::updateScenarioName) }
                launch { viewModel.scenarioNameError.collect(viewBinding.fieldScenarioName::setError) }
                launch { viewModel.randomization.collect(::updateRandomization) }
                launch { viewModel.detectionQuality.collect(::updateQuality) }
            }
        }
    }

    private fun updateScenarioName(name: String?) {
        viewBinding.fieldScenarioName.setText(name)
    }

    private fun updateRandomization(isEnabled: Boolean) {
        viewBinding.fieldAntiDetection.apply {
            setChecked(isEnabled)
            setDescription(if (isEnabled) 1 else 0)
        }
    }

    private fun updateQuality(quality: Int?) {
        if (quality == null) return

        viewBinding.apply {
            textQualityValue.text = quality.toString()

            val isNotInitialized = seekbarQuality.value == 0f
            seekbarQuality.value = quality.toFloat()

            if (isNotInitialized) {
                seekbarQuality.valueFrom = SLIDER_QUALITY_MIN
                seekbarQuality.valueTo = SLIDER_QUALITY_MAX
            }
        }
    }
}
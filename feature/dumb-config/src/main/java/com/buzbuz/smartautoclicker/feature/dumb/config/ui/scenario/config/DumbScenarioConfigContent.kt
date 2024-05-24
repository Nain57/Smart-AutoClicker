/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.config

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DUMB_SCENARIO_MAX_DURATION_MINUTES
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MAX_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MIN_VALUE
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setNumericValue
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ContentDumbScenarioConfigBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import kotlinx.coroutines.launch

class DumbScenarioConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: DumbScenarioConfigViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioConfigViewModel() },
    )

    private lateinit var viewBinding: ContentDumbScenarioConfigBinding
    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDumbScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            fieldName.apply {
                setLabel(R.string.input_field_label_scenario_name)
                setOnTextChangedListener { dialogViewModel.setDumbScenarioName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(fieldName.textField)

            fieldRepeatCount.apply {
                textField.filters = arrayOf(MinMaxInputFilter(
                    REPEAT_COUNT_MIN_VALUE,
                    REPEAT_COUNT_MAX_VALUE,
                ))
                setup(R.string.input_field_label_repeat_count, R.drawable.ic_infinite, disableInputWithCheckbox = true)
                setOnTextChangedListener {
                    dialogViewModel.setRepeatCount(if (it.isNotEmpty()) it.toString().toInt() else 0)
                }
                setOnCheckboxClickedListener(dialogViewModel::toggleInfiniteRepeat)
            }

            fieldMaxDuration.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, DUMB_SCENARIO_MAX_DURATION_MINUTES))
                setup(R.string.input_field_label_maximum_duration, R.drawable.ic_infinite, disableInputWithCheckbox = true)
                setOnTextChangedListener {
                    dialogViewModel.setMaxDurationMinutes(if (it.isNotEmpty()) it.toString().toInt() else 0)
                }
                setOnCheckboxClickedListener(dialogViewModel::toggleInfiniteMaxDuration)
            }

            fieldAntiDetection.apply {
                setTitle(context.resources.getString(R.string.input_field_label_anti_detection))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.dropdown_helper_text_anti_detection_disabled),
                        context.getString(R.string.dropdown_helper_text_anti_detection_enabled),
                    )
                )
                setOnClickListener(dialogViewModel::toggleRandomization)

            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.scenarioName.collect(viewBinding.fieldName::setText) }
                launch { dialogViewModel.scenarioNameError.collect(viewBinding.fieldName::setError)}
                launch { dialogViewModel.repeatCount.collect(viewBinding.fieldRepeatCount::setNumericValue) }
                launch { dialogViewModel.repeatCountError.collect(viewBinding.fieldRepeatCount::setError) }
                launch { dialogViewModel.repeatInfiniteState.collect(viewBinding.fieldRepeatCount::setChecked) }
                launch { dialogViewModel.maxDurationMin.collect(viewBinding.fieldMaxDuration::setNumericValue) }
                launch { dialogViewModel.maxDurationMinError.collect(viewBinding.fieldMaxDuration::setError) }
                launch { dialogViewModel.maxDurationMinInfiniteState.collect(viewBinding.fieldMaxDuration::setChecked) }
                launch { dialogViewModel.randomization.collect(::updateFieldRandomization) }
            }
        }
    }

    private fun updateFieldRandomization(isEnabled: Boolean) {
        viewBinding.fieldAntiDetection.apply {
            setChecked(isEnabled)
            setDescription(if (isEnabled) 1 else 0)
        }
    }
}
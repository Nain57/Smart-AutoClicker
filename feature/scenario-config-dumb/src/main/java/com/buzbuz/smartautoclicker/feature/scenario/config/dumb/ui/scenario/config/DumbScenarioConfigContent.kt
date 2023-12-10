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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.config

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
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setNumericValue
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.setup
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.ContentDumbScenarioConfigBinding

import kotlinx.coroutines.launch

class DumbScenarioConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: DumbScenarioConfigViewModel by viewModels()

    private lateinit var viewBinding: ContentDumbScenarioConfigBinding
    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDumbScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            scenarioNameInputLayout.apply {
                setLabel(R.string.input_field_label_scenario_name)
                setOnTextChangedListener { dialogViewModel.setDumbScenarioName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(scenarioNameInputLayout.textField)

            repeatCountInputField.apply {
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

            scenarioRandomization.setItems(
                label = context.resources.getString(R.string.input_field_label_anti_detection),
                items = dialogViewModel.randomizationDropdownItems,
                onItemSelected = dialogViewModel::setRandomization,
            )

            maxDurationInputField.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, DUMB_SCENARIO_MAX_DURATION_MINUTES))
                setup(R.string.input_field_label_maximum_duration, R.drawable.ic_infinite, disableInputWithCheckbox = true)
                setOnTextChangedListener {
                    dialogViewModel.setMaxDurationMinutes(if (it.isNotEmpty()) it.toString().toInt() else 0)
                }
                setOnCheckboxClickedListener(dialogViewModel::toggleInfiniteMaxDuration)
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.scenarioName.collect(viewBinding.scenarioNameInputLayout::setText) }
                launch { dialogViewModel.scenarioNameError.collect(viewBinding.scenarioNameInputLayout::setError)}
                launch { dialogViewModel.repeatCount.collect(viewBinding.repeatCountInputField::setNumericValue) }
                launch { dialogViewModel.repeatCountError.collect(viewBinding.repeatCountInputField::setError) }
                launch { dialogViewModel.repeatInfiniteState.collect(viewBinding.repeatCountInputField::setChecked) }
                launch { dialogViewModel.maxDurationMin.collect(viewBinding.maxDurationInputField::setNumericValue) }
                launch { dialogViewModel.maxDurationMinError.collect(viewBinding.maxDurationInputField::setError) }
                launch { dialogViewModel.maxDurationMinInfiniteState.collect(viewBinding.maxDurationInputField::setChecked) }
                launch { dialogViewModel.randomization.collect(viewBinding.scenarioRandomization::setSelectedItem) }
            }
        }
    }
}
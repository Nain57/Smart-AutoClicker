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
package com.buzbuz.smartautoclicker.overlays.config.scenario.config

import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.baseui.bindings.setItems
import com.buzbuz.smartautoclicker.baseui.bindings.setLabel
import com.buzbuz.smartautoclicker.baseui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.baseui.bindings.setSelectedItem
import com.buzbuz.smartautoclicker.baseui.bindings.setText
import com.buzbuz.smartautoclicker.databinding.ContentScenarioConfigBinding
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavigationRequest
import com.buzbuz.smartautoclicker.domain.edition.EditedEndCondition
import com.buzbuz.smartautoclicker.overlays.base.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.overlays.base.utils.ALPHA_ENABLED_ITEM
import com.buzbuz.smartautoclicker.overlays.base.utils.setError
import com.buzbuz.smartautoclicker.overlays.config.endcondition.EndConditionConfigDialog
import com.buzbuz.smartautoclicker.overlays.config.scenario.ScenarioDialogViewModel

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ScenarioConfigContent : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: ScenarioConfigViewModel by lazy {
        ViewModelProvider(this).get(ScenarioConfigViewModel::class.java)
    }
    /** View model for the container dialog. */
    private val dialogViewModel: ScenarioDialogViewModel by lazy {
        ViewModelProvider(dialogController).get(ScenarioDialogViewModel::class.java)
    }

    private lateinit var viewBinding: ContentScenarioConfigBinding
    private lateinit var endConditionAdapter: EndConditionAdapter

    private var billingFlowStarted: Boolean = false

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            scenarioNameField.apply {
                setLabel(R.string.input_field_label_scenario_name)
                setOnTextChangedListener { viewModel.setScenarioName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }

            textSpeed.setOnClickListener { viewModel.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel.increaseDetectionQuality() }
            seekbarQuality.addOnChangeListener { _, value, fromUser ->
                if (fromUser) viewModel.setDetectionQuality(value.roundToInt())
            }

            endConditionsOperatorField.setItems(
                items = viewModel.endConditionOperatorsItems,
                onItemSelected = viewModel::setConditionOperator,
            )

            endConditionAdapter = EndConditionAdapter(
                addEndConditionClickedListener = ::onAddEndConditionClicked,
                endConditionClickedListener = ::onEndConditionClicked,
            )
            endConditionsList.adapter = endConditionAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        // When the billing flow is not longer displayed, restore the dialogs states
        lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.CREATED)) {
                viewModel.isBillingFlowDisplayed.collect { isDisplayed ->
                    if (!isDisplayed) {
                        if (billingFlowStarted) {
                            dialogController.show()
                            billingFlowStarted = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.scenarioName.collect(::updateScenarioName) }
                launch { viewModel.scenarioNameError.collect(viewBinding.scenarioNameField::setError) }
                launch { viewModel.randomizationDropdownState.collect(::updateRandomizationDropdown) }
                launch { viewModel.randomization.collect(::updateRandomization) }
                launch { viewModel.detectionQuality.collect(::updateQuality) }
                launch { viewModel.endConditionOperator.collect(::updateEndConditionOperator) }
                launch { viewModel.endConditions.collect(::updateEndConditions) }
            }
        }
    }

    private fun updateScenarioName(name: String?) {
        viewBinding.scenarioNameField.setText(name)
    }

    private fun updateRandomizationDropdown(dropdownState: RandomizationDropdownUiState) {
        viewBinding.scenarioActionRandomization.setItems(
            label = context.resources.getString(R.string.input_field_label_anti_detection),
            items = dropdownState.items,
            enabled = dropdownState.enabled,
            disabledIcon = dropdownState.disabledIcon,
            onItemSelected = viewModel::setRandomization,
            onDisabledClick = {
                billingFlowStarted = true
                dialogController.hide()
                viewModel.onAntoDetectionClickedWithoutProMode(context)
            },
        )

        viewBinding.scenarioActionRandomization.root.alpha =
            if (dropdownState.enabled) ALPHA_ENABLED_ITEM
            else ALPHA_DISABLED_ITEM
    }

    private fun updateRandomization(randomizationItem: DropdownItem) {
        viewBinding.scenarioActionRandomization.setSelectedItem(randomizationItem)
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

    private fun updateEndConditionOperator(operatorItem: DropdownItem) {
        viewBinding.endConditionsOperatorField.setSelectedItem(operatorItem)
    }

    private fun updateEndConditions(endConditions: List<EndConditionListItem>) {
        viewBinding.apply {
            if (endConditions.isEmpty()) {
                endConditionsList.visibility = View.GONE
                endConditionsNoEvents.visibility = View.VISIBLE
            } else {
                endConditionsList.visibility = View.VISIBLE
                endConditionsNoEvents.visibility = View.GONE
            }
        }

        endConditionAdapter.submitList(endConditions)
    }

    private fun onAddEndConditionClicked() {
        viewModel.createNewEndCondition().let { endCondition ->
            dialogViewModel.requestSubOverlay(
                NavigationRequest(
                    EndConditionConfigDialog(
                        context = context,
                        endCondition = endCondition,
                        onConfirmClicked = { newEndCondition -> viewModel.addEndCondition(newEndCondition) },
                        onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
                    )
                )
            )
        }
    }

    private fun onEndConditionClicked(endCondition: EditedEndCondition) {
        dialogViewModel.requestSubOverlay(
            NavigationRequest(
                EndConditionConfigDialog(
                    context = context,
                    endCondition = endCondition,
                    onConfirmClicked = { newEndCondition -> viewModel.updateEndCondition(newEndCondition) },
                    onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
                )
            )
        )
    }
}
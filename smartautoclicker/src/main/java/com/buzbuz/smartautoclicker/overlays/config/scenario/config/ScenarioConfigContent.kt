/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.scenario.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentScenarioConfigBinding
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.base.bindings.addOnCheckedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonsText
import com.buzbuz.smartautoclicker.overlays.base.bindings.setChecked
import com.buzbuz.smartautoclicker.overlays.config.endcondition.EndConditionConfigDialog
import com.buzbuz.smartautoclicker.overlays.config.scenario.ScenarioDialogViewModel
import com.buzbuz.smartautoclicker.baseui.OnAfterTextChangedListener
import com.buzbuz.smartautoclicker.overlays.base.utils.setError

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

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setScenario(dialogViewModel.configuredScenario)

        viewBinding = ContentScenarioConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            scenarioNameInputEditText.apply {
                addTextChangedListener(OnAfterTextChangedListener {
                    viewModel.setScenarioName(it.toString())
                })
            }

            textSpeed.setOnClickListener { viewModel.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel.increaseDetectionQuality() }
            seekbarQuality.addOnChangeListener { _, value, fromUser ->
                if (fromUser) viewModel.setDetectionQuality(value.roundToInt())
            }

            endConditionsOperatorButton.apply {
                setButtonsText(R.string.dialog_button_condition_and, R.string.dialog_button_condition_or)
                addOnCheckedListener { checkedId ->
                    when (checkedId) {
                        R.id.left_button -> viewModel.setConditionOperator(AND)
                        R.id.right_button -> viewModel.setConditionOperator(OR)
                    }
                }
            }

            endConditionAdapter = EndConditionAdapter(
                addEndConditionClickedListener = ::onAddEndConditionClicked,
                endConditionClickedListener = ::onEndConditionClicked,
            )
            endConditionsList.adapter = endConditionAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.scenarioName.collect(::updateScenarioName) }
                launch { viewModel.scenarioNameError.collect(viewBinding.scenarioNameInputLayout::setError) }
                launch { viewModel.detectionQuality.collect(::updateQuality) }
                launch { viewModel.endConditionOperator.collect(::updateEndConditionOperator) }
                launch { viewModel.endConditions.collect(::updateEndConditions) }
            }
        }
    }

    private fun updateScenarioName(name: String?) {
        viewBinding.scenarioNameInputEditText.setText(name)
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

    private fun updateEndConditionOperator(@ConditionOperator operator: Int?) {
        viewBinding.endConditionsOperatorButton.apply {
            when (operator) {
                AND -> setChecked(R.id.left_button, R.string.condition_operator_and_desc)
                OR -> setChecked(R.id.right_button, R.string.condition_operator_or_desc)
            }
        }
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
                        endConditions = viewModel.getConfiguredEndConditionsList(),
                        onConfirmClicked = { newEndCondition -> viewModel.addEndCondition(newEndCondition) },
                        onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
                    )
                )
            )
        }
    }

    private fun onEndConditionClicked(endCondition: EndCondition, index: Int) {
        dialogViewModel.requestSubOverlay(
            NavigationRequest(
                EndConditionConfigDialog(
                    context = context,
                    endCondition = endCondition,
                    endConditions = viewModel.getConfiguredEndConditionsList(),
                    onConfirmClicked = { newEndCondition -> viewModel.updateEndCondition(newEndCondition, index) },
                    onDeleteClicked = { viewModel.deleteEndCondition(endCondition) }
                )
            )
        )
    }
}
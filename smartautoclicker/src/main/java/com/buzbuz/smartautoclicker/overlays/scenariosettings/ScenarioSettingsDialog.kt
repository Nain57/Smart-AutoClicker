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
package com.buzbuz.smartautoclicker.overlays.scenariosettings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.databinding.DialogScenarioSettingsBinding
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.overlays.scenariosettings.endcondition.EndConditionConfigDialog

import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying the scenario settings.
 **
 * @param context the Android Context for the dialog shown by this controller.
 * @param scenarioId the scenario to display the settings of.
 */
class ScenarioSettingsDialog(
    context: Context,
    scenarioId: Long,
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: ScenarioSettingsModel? = ScenarioSettingsModel(context).apply {
        attachToLifecycle(this@ScenarioSettingsDialog)
        setScenario(scenarioId)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogScenarioSettingsBinding

    /** Adapter displaying all actions for the event displayed by this dialog. */
    private val endConditionsAdapter = EndConditionAdapter(
        addEndConditionClickedListener = {
            viewModel?.createNewEndCondition()?.let { endCondition ->
                showSubOverlay(EndConditionConfigDialog(
                    context = context,
                    endCondition = endCondition,
                    endConditions = viewModel?.configuredEndConditions?.value ?: emptyList(),
                    onConfirmClicked = { newEndCondition -> viewModel?.addEndCondition(newEndCondition) },
                    onDeleteClicked = { viewModel?.deleteEndCondition(endCondition) }
                ))
            }
        },
        endConditionClickedListener = { endCondition, index ->
            showSubOverlay(EndConditionConfigDialog(
                context = context,
                endCondition = endCondition,
                endConditions = viewModel?.configuredEndConditions?.value ?: emptyList(),
                onConfirmClicked = { newEndCondition -> viewModel?.updateEndCondition(newEndCondition, index) },
                onDeleteClicked = { viewModel?.deleteEndCondition(endCondition) }
            ))
        }
    )

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogScenarioSettingsBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_scenario_settings_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ -> onOkClicked() }
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.apply {
            textSpeed.setOnClickListener { viewModel?.decreaseDetectionQuality() }
            textPrecision.setOnClickListener { viewModel?.increaseDetectionQuality() }

            seekbarQuality.apply {
                max = SEEK_BAR_QUALITY_MAX
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        viewModel?.setDetectionQuality(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            textEndConditionOperatorDesc.setOnClickListener {
                viewModel?.toggleEndConditionOperator()
            }

            listEndConditions.adapter = endConditionsAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.detectionQuality?.collect { detectionQuality ->
                        viewBinding.textQualityValue.text = detectionQuality.toString()
                        if (viewBinding.seekbarQuality.progress == 0) {
                            viewBinding.seekbarQuality.progress =
                                (detectionQuality ?: 0) - DETECTION_QUALITY_MIN.toInt()
                        }
                    }
                }

                launch {
                    viewModel?.endConditionOperator?.collect { conditionOperator ->
                        viewBinding.textEndConditionOperatorDesc.apply {
                            when (conditionOperator) {
                                AND -> {
                                    setLeftRightCompoundDrawables(R.drawable.ic_all_conditions, R.drawable.ic_chevron)
                                    text = context.getString(R.string.condition_operator_and)
                                }
                                OR -> {
                                    setLeftRightCompoundDrawables(R.drawable.ic_one_condition, R.drawable.ic_chevron)
                                    text = context.getString(R.string.condition_operator_or)
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel?.endConditions?.collect { endConditions ->
                        viewBinding.apply {
                            if (endConditions.isEmpty()) {
                                layoutEndConditions.visibility = View.GONE
                                textEndConditionNoEvent.visibility = View.VISIBLE
                            } else {
                                layoutEndConditions.visibility = View.VISIBLE
                                textEndConditionNoEvent.visibility = View.GONE
                            }
                        }
                        endConditionsAdapter.submitList(endConditions)
                    }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }

    /** Called when the user press the OK button. */
    private fun onOkClicked() {
        viewModel?.saveModifications()
        dismiss()
    }
}
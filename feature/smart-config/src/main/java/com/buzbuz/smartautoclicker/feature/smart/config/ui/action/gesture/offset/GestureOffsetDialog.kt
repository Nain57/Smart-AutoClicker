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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.offset

import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigGestureOffsetBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.enableClearCounterOnType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.setPositionValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch


class GestureOffsetDialog(
    private val currentOffset: GesturePosition.Coordinates,
    private val onNewOffset: (GesturePosition.Coordinates) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: GestureOffsetViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { gestureOffsetViewModel() },
    )

    private lateinit var viewBinding: DialogConfigGestureOffsetBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigGestureOffsetBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.field_click_offset_title)
                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener {
                        viewModel.getCurrentOffset()?.let { onNewOffset(it) }
                        back()
                    }
                }
            }

            fieldX.apply {
                setup(
                    label = R.string.field_title_position_x,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { editable ->
                    viewModel.setXLiteral(editable.toString().toIntOrNull() ?: 0)
                }
                setOnCheckboxClickedListener { showCounterSelectionDialog(isX = true) }
            }

            fieldY.apply {
                setup(
                    label = R.string.field_title_position_y,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { editable ->
                    viewModel.setYLiteral(editable.toString().toIntOrNull() ?: 0)
                }
                setOnCheckboxClickedListener { showCounterSelectionDialog(isX = false) }
            }

            viewClickOffset.apply {
                setImageResource(R.drawable.ic_image_condition_big)
                onOffsetChangedListener = { offsetF -> viewModel.setOffsetFromView(offsetF) }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.initialize(currentOffset)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }
    }

    private fun updateUiState(state: GestureOffsetUiState) {
        if (state.updateFrom != GestureOffsetUpdateSource.TEXT_INPUT) {
            updateFieldX(state.xState)
            updateFieldY(state.yState)
        }
        if (state.updateFrom != GestureOffsetUpdateSource.VIEW) {
            updateClickOffsetView(state.xState, state.yState)
        }
    }

    private fun updateFieldX(xState: GestureOffsetValueUiState) {
        viewBinding.fieldX.textField.apply {
            when (xState) {
                is GestureOffsetValueUiState.Literal ->
                    viewBinding.fieldX.setPositionValue(xState.displayText)

                is GestureOffsetValueUiState.Counter ->
                    viewBinding.fieldX.setPositionValue(xState.displayText)
            }
        }
    }

    private fun updateFieldY(yState: GestureOffsetValueUiState) {
        viewBinding.fieldX.textField.apply {
            when (yState) {
                is GestureOffsetValueUiState.Literal ->
                    viewBinding.fieldY.setPositionValue(yState.displayText)

                is GestureOffsetValueUiState.Counter ->
                    viewBinding.fieldY.setPositionValue(yState.displayText)
            }
        }
    }

    private fun updateClickOffsetView(xState: GestureOffsetValueUiState, yState: GestureOffsetValueUiState) {
        val xLiteral = (xState as? GestureOffsetValueUiState.Literal)?.displayText?.toFloatOrNull() ?: return
        val yLiteral = (yState as? GestureOffsetValueUiState.Literal)?.displayText?.toFloatOrNull() ?: return
        viewBinding.viewClickOffset.offsetValue = PointF(xLiteral, yLiteral)
    }

    private fun showCounterSelectionDialog(isX: Boolean) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog { counterName ->
                if (isX) viewModel.setXCounter(counterName)
                else viewModel.setYCounter(counterName)
            },
            hideCurrent = false,
        )
    }
}

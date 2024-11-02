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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click

import android.graphics.Bitmap
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigClickOnConditionOffsetBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch


class ClickOffsetDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ClickOffsetViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickOffsetViewModel() },
    )

    private lateinit var viewBinding: DialogConfigClickOnConditionOffsetBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigClickOnConditionOffsetBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.field_click_offset_title)

                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener {
                        viewModel.saveChanges()
                        back()
                    }
                }
            }

            fieldX.apply {
                textField.filters = viewModel.getOffsetMaxBoundsX().let { xBounds ->
                    arrayOf(MinMaxInputFilter(xBounds.first, xBounds.last))
                }
                setLabel(R.string.field_click_offset_x)
                setOnTextChangedListener {
                    it.getOffsetValue()?.let { xOffset ->
                        viewModel.setClickOffsetX(xOffset, ClickOffsetUpdateType.TEXT_INPUT)
                    }
                }
            }

            fieldY.apply {
                textField.filters = viewModel.getOffsetMaxBoundsY().let { yBounds ->
                    arrayOf(MinMaxInputFilter(yBounds.first, yBounds.last))
                }
                setLabel(R.string.field_click_offset_y)
                setOnTextChangedListener {
                    it.getOffsetValue()?.let { yOffset ->
                        viewModel.setClickOffsetY(yOffset, ClickOffsetUpdateType.TEXT_INPUT)
                    }
                }
            }

            viewClickOffset.apply {
                onOffsetChangedListener = { offset ->
                    viewModel.setClickOffset(offset.toPoint(), from = ClickOffsetUpdateType.VIEW)
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.clickOffset.collect(::updateClickOffset) }
                launch { viewModel.conditionImage.collect(::updateConditionImage) }
            }
        }
    }

    private fun updateClickOffset(offsetState: ClickOffsetState) {
        viewBinding.apply {
            if (offsetState.updateFrom != ClickOffsetUpdateType.VIEW) {
                viewClickOffset.offsetValue = offsetState.offset.toPointF()
            }

            if (offsetState.updateFrom != ClickOffsetUpdateType.TEXT_INPUT) {
                viewBinding.fieldX.setText(
                    offsetState.offset.x.toString(),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
                )
                viewBinding.fieldY.setText(
                    offsetState.offset.y.toString(),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
                )
            }
        }
    }

    private fun updateConditionImage(bitmap: Bitmap?) {
        viewBinding.viewClickOffset.apply {
            if (bitmap != null) setImageBitmap(bitmap)
            else setImageResource(R.drawable.ic_image_condition_big)
        }
    }
}

private fun Editable.getOffsetValue(): Int? =
    try { toString().toInt() }
    catch (nfEx: NumberFormatException) { null }
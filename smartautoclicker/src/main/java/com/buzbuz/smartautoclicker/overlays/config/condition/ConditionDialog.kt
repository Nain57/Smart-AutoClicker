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
package com.buzbuz.smartautoclicker.overlays.config.condition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigConditionBinding
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.overlays.base.bindings.addOnCheckedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonsText
import com.buzbuz.smartautoclicker.overlays.base.bindings.setChecked
import com.buzbuz.smartautoclicker.overlays.base.bindings.setError
import com.buzbuz.smartautoclicker.overlays.base.bindings.setLabel
import com.buzbuz.smartautoclicker.overlays.base.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.setText

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ConditionDialog(
    context: Context,
    private val condition: Condition,
    private val onConfirmClicked: (Condition) -> Unit,
    private val onDeleteClicked: () -> Unit
) : OverlayDialogController(context, R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: ConditionViewModel by lazy {
        ViewModelProvider(this).get(ConditionViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfigCondition(condition)

        viewBinding = DialogConfigConditionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_condition_title)

                buttonDismiss.setOnClickListener { destroy() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        onConfirmClicked(viewModel.getConfiguredCondition())
                        destroy()
                    }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        onDeleteClicked()
                        destroy()
                    }
                }
            }

            editNameLayout.apply {
                setLabel(R.string.dialog_event_config_name_title)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
            }

            conditionShouldAppearButton.apply {
                setButtonsText(
                    R.string.dialog_button_should_be_detected,
                    R.string.dialog_button_should_not_be_detected,
                )
                addOnCheckedListener { viewModel.setShouldBeDetected(it == R.id.left_button) }
            }

            conditionDetectionTypeButton.apply {
                setButtonsText(
                    R.string.dialog_button_detection_type_exact,
                    R.string.dialog_button_detection_type_screen,
                )
                descriptionText.setLines(2)
                addOnCheckedListener { checkedId ->
                    when (checkedId) {
                        R.id.left_button -> viewModel.setDetectionType(EXACT)
                        R.id.right_button -> viewModel.setDetectionType(WHOLE_SCREEN)
                    }
                }
            }

            seekbarDiffThreshold.addOnChangeListener {  _, value, fromUser ->
                if (fromUser) viewModel.setThreshold(value.roundToInt())
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateConditionName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError) }
                launch { viewModel.conditionBitmap.collect(::updateConditionBitmap) }
                launch { viewModel.shouldBeDetected.collect(::updateShouldBeDetected) }
                launch { viewModel.detectionType.collect(::updateConditionType) }
                launch { viewModel.threshold.collect(::updateThreshold) }
                launch { viewModel.isValidCondition.collect(::updateSaveButton) }
            }
        }
    }

    private fun updateConditionName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateConditionBitmap(newBitmap: Bitmap?) {
        if (newBitmap != null) {
            viewBinding.imageCondition.setImageBitmap(newBitmap)
        } else {
            viewBinding.imageCondition.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                    setTint(Color.RED)
                }
            )
        }
    }

    private fun updateShouldBeDetected(newValue: Boolean) {
        viewBinding.conditionShouldAppearButton.apply {
            if (newValue) setChecked(R.id.left_button, R.string.dialog_desc_should_be_detected)
            else setChecked(R.id.right_button, R.string.dialog_desc_should_not_be_detected)
        }
    }

    private fun updateConditionType(@DetectionType newType: Int) {
        viewBinding.conditionDetectionTypeButton.apply {
            when (newType) {
                EXACT -> setChecked(
                    R.id.left_button,
                    context.getString(
                        R.string.dialog_desc_detection_type_exact,
                        condition.area.left,
                        condition.area.top,
                        condition.area.right,
                        condition.area.bottom,
                    )
                )

                WHOLE_SCREEN -> setChecked(
                    R.id.right_button,
                    R.string.dialog_desc_detection_type_screen,
                )
            }
        }
    }

    private fun updateThreshold(newThreshold: Int) {
        viewBinding.apply {
            textDiffThreshold.text = context.getString(
                R.string.dialog_condition_threshold_value,
                newThreshold,
            )

            val isNotInitialized = seekbarDiffThreshold.value == 0f
            seekbarDiffThreshold.value = newThreshold.toFloat()

            if (isNotInitialized) {
                seekbarDiffThreshold.valueFrom = 0f
                seekbarDiffThreshold.valueTo = MAX_THRESHOLD
            }
        }
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }
}
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.click

import android.graphics.Point
import android.graphics.PointF
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MAX_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MIN_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MAX_MS
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MIN_MS
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setNumericValue
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.DialogConfigDumbActionClickBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class DumbClickDialog(
    private val dumbClick: DumbAction.DumbClick,
    private val onConfirmClicked: (DumbAction.DumbClick) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbClick) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: DumbClickViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbClickViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigDumbActionClickBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbClick(dumbClick)

        viewBinding = DialogConfigDumbActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.item_title_dumb_click)

                buttonDismiss.setDebouncedOnClickListener { onDismissButtonClicked()}
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteButtonClicked() }
                }
            }

            fieldName.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldName.textField)

            fieldDuration.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, GESTURE_DURATION_MAX_VALUE.toInt()))
                setLabel(R.string.input_field_label_click_press_duration)
                setOnTextChangedListener {
                    viewModel.setPressDurationMs(if (it.isNotEmpty()) it.toString().toLong() else 0)
                }
            }
            hideSoftInputOnFocusLoss(fieldDuration.textField)

            fieldRepeat.apply {
                textField.filters = arrayOf(MinMaxInputFilter(
                    REPEAT_COUNT_MIN_VALUE,
                    REPEAT_COUNT_MAX_VALUE
                ))
                setup(R.string.input_field_label_repeat_count, R.drawable.ic_infinite, disableInputWithCheckbox = true)
                setOnTextChangedListener {
                    viewModel.setRepeatCount(if (it.isNotEmpty()) it.toString().toInt() else 0)
                }
                setOnCheckboxClickedListener(viewModel::toggleInfiniteRepeat)
            }
            hideSoftInputOnFocusLoss(fieldRepeat.textField)

            fieldRepeatDelay.apply {
                textField.filters = arrayOf(MinMaxInputFilter(
                    REPEAT_DELAY_MIN_MS.toInt(),
                    REPEAT_DELAY_MAX_MS.toInt(),
                ))
                setLabel(R.string.input_field_label_repeat_delay)
                setOnTextChangedListener {
                    viewModel.setRepeatDelay(if (it.isNotEmpty()) it.toString().toLong() else 0)
                }
            }
            hideSoftInputOnFocusLoss(fieldRepeatDelay.textField)

            fieldSelectionPosition.apply {
                setTitle(context.getString(R.string.field_click_position_title))
                setOnClickListener { debounceUserInteraction { onPositionCardClicked() } }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isValidDumbClick.collect(::updateSaveButton) }
                launch { viewModel.name.collect(viewBinding.fieldName::setText) }
                launch { viewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { viewModel.pressDuration.collect(::updateDumbClickPressDuration) }
                launch { viewModel.pressDurationError.collect(viewBinding.fieldDuration::setError)}
                launch { viewModel.repeatCount.collect(viewBinding.fieldRepeat::setNumericValue) }
                launch { viewModel.repeatCountError.collect(viewBinding.fieldRepeat::setError) }
                launch { viewModel.repeatInfiniteState.collect(viewBinding.fieldRepeat::setChecked) }
                launch { viewModel.repeatDelay.collect(::updateDumbClickRepeatDelay) }
                launch { viewModel.repeatDelayError.collect(viewBinding.fieldRepeatDelay::setError)}
                launch { viewModel.clickPositionText.collect(::updateFieldPosition) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbClick()?.let { editedDumbClick ->
            viewModel.saveLastConfig(context)
            onConfirmClicked(editedDumbClick)
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        viewModel.getEditedDumbClick()?.let { editedAction ->
            onDeleteClicked(editedAction)
            back()
        }
    }

    private fun onDismissButtonClicked() {
        onDismissClicked()
        back()
    }

    private fun updateDumbClickPressDuration(duration: String) {
        viewBinding.fieldDuration.setText(duration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateDumbClickRepeatDelay(delay: String) {
        viewBinding.fieldRepeatDelay.setText(delay, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun updateFieldPosition(positionText: String) {
        viewBinding.fieldSelectionPosition.setDescription(positionText)
    }

    private fun onPositionCardClicked() {
        viewModel.getEditedDumbClick()?.let { dumbClick ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    itemBriefDescription = ClickDescription(
                        position = dumbClick.position.toEditionPosition(),
                        pressDurationMs = dumbClick.pressDurationMs,
                    ),
                    onConfirm = { description ->
                        viewModel.setPosition((description as? ClickDescription)?.position?.toPoint())
                    }
                ),
                hideCurrent = true,
            )
        }
    }

    private fun Point.toEditionPosition(): PointF? =
        if (x == 0 && y == 0) null
        else toPointF()

}
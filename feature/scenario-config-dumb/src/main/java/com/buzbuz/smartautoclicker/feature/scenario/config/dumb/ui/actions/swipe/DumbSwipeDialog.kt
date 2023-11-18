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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.swipe

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
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.DialogConfigDumbActionSwipeBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setError
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setInfiniteState
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setOnInfiniteButtonClickedListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.setRepeatCount

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class DumbSwipeDialog(
    private val dumbSwipe: DumbAction.DumbSwipe,
    private val onConfirmClicked: (DumbAction.DumbSwipe) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbSwipe) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.DumbScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: DumbSwipeViewModel by viewModels()
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigDumbActionSwipeBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbSwipe(dumbSwipe)

        viewBinding = DialogConfigDumbActionSwipeBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.item_title_dumb_swipe)

                buttonDismiss.setOnClickListener { onDismissButtonClicked()}
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editNameLayout.textField)

            editSwipeDurationLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, GESTURE_DURATION_MAX_VALUE.toInt()))
                setLabel(R.string.input_field_label_swipe_duration)
                setOnTextChangedListener {
                    viewModel.setPressDurationMs(if (it.isNotEmpty()) it.toString().toLong() else 0)
                }
            }
            hideSoftInputOnFocusLoss(editSwipeDurationLayout.textField)

            editRepeatLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(
                    REPEAT_COUNT_MIN_VALUE,
                    REPEAT_COUNT_MAX_VALUE
                ))
                setLabel(R.string.input_field_label_repeat_count)
                setOnTextChangedListener {
                    viewModel.setRepeatCount(if (it.isNotEmpty()) it.toString().toInt() else 0)
                }
                setOnInfiniteButtonClickedListener(viewModel::toggleInfiniteRepeat)
            }

            editRepeatDelay.apply {
                textField.filters = arrayOf(MinMaxInputFilter(
                    REPEAT_DELAY_MIN_MS.toInt(),
                    REPEAT_DELAY_MAX_MS.toInt(),
                ))
                setLabel(R.string.input_field_label_repeat_delay)
                setOnTextChangedListener {
                    viewModel.setRepeatDelay(if (it.isNotEmpty()) it.toString().toLong() else 0)
                }
            }
            hideSoftInputOnFocusLoss(editRepeatDelay.textField)

            cardSwipePosition.setOnClickListener { onPositionCardClicked() }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isValidDumbSwipe.collect(::updateSaveButton) }
                launch { viewModel.name.collect(viewBinding.editNameLayout::setText) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.swipeDuration.collect(::updateDumbSwipePressDuration) }
                launch { viewModel.swipeDurationError.collect(viewBinding.editSwipeDurationLayout::setError)}
                launch { viewModel.repeatCount.collect(viewBinding.editRepeatLayout::setRepeatCount) }
                launch { viewModel.repeatCountError.collect(viewBinding.editRepeatLayout::setError) }
                launch { viewModel.repeatInfiniteState.collect(viewBinding.editRepeatLayout::setInfiniteState) }
                launch { viewModel.repeatDelay.collect(::updateDumbSwipeRepeatDelay) }
                launch { viewModel.repeatDelayError.collect(viewBinding.editRepeatDelay::setError)}
                launch { viewModel.swipePositionText.collect(viewBinding.swipeSelectorSubtext::setText) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbSwipe()?.let { editedDumbSwipe ->
            debounceUserInteraction {
                viewModel.saveLastConfig(context)
                onConfirmClicked(editedDumbSwipe)
                back()
            }
        }
    }

    private fun onDeleteButtonClicked() {
        viewModel.getEditedDumbSwipe()?.let { editedAction ->
            debounceUserInteraction {
                onDeleteClicked(editedAction)
                back()
            }
        }
    }

    private fun onDismissButtonClicked() {
        debounceUserInteraction {
            onDismissClicked()
            back()
        }
    }

    private fun onPositionCardClicked() {
        viewModel.getEditedDumbSwipe()?.let { swipe ->
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    actionDescription = SwipeDescription(
                        from = swipe.fromPosition.toEditionPosition(),
                        to = swipe.toPosition.toEditionPosition(),
                        swipeDurationMs = swipe.swipeDurationMs,
                    ),
                    onConfirm = { swipeDesc ->
                        (swipeDesc as? SwipeDescription)?.let {
                            viewModel.setPositions(it.from?.toPoint(), it.to?.toPoint())
                        }
                    }
                ),
                hideCurrent = true,
            )
        }

    }

    private fun updateDumbSwipePressDuration(duration: String) {
        viewBinding.editSwipeDurationLayout.setText(duration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateDumbSwipeRepeatDelay(delay: String) {
        viewBinding.editRepeatDelay.setText(delay, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun Point.toEditionPosition(): PointF? =
        if (x == 0 && y == 0) null
        else toPointF()
}
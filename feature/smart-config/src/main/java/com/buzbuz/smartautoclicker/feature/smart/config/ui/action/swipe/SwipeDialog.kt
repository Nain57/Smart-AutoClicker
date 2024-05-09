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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe

import android.graphics.Point
import android.graphics.PointF
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toPoint

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.base.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setError
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionSwipeBinding
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class SwipeDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: SwipeViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { swipeViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionSwipeBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionSwipeBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_swipe)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        listener.onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
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

            fieldSwipeDuration.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, GESTURE_DURATION_MAX_VALUE.toInt()))
                setLabel(R.string.input_field_label_swipe_duration)
                setOnTextChangedListener {
                    viewModel.setSwipeDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }
            hideSoftInputOnFocusLoss(fieldSwipeDuration.textField)

            fieldSelectionSwipePosition.apply {
                setTitle(context.getString(R.string.field_title_select_swipe_positions))
                setOnClickListener { debounceUserInteraction { showPositionSelector() } }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingAction.collect(::onActionEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { viewModel.swipeDuration.collect(::updateSwipeDuration) }
                launch { viewModel.swipeDurationError.collect(viewBinding.fieldSwipeDuration::setError)}
                launch { viewModel.positions.collect(::updateSwipePositionsField) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        debounceUserInteraction {
            viewModel.saveLastConfig()
            listener.onConfirmClicked()
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        debounceUserInteraction {
            listener.onDeleteClicked()
            back()
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.fieldName.setText(newName)
    }

    private fun updateSwipeDuration(newDuration: String?) {
        viewBinding.fieldSwipeDuration.setText(newDuration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSwipePositionsField(positions: Pair<Point, Point>?) {
        viewBinding.fieldSelectionSwipePosition.setDescription(
            if (positions != null)
                context.getString(
                    R.string.item_desc_swipe_positions,
                    positions.first.x,
                    positions.first.y,
                    positions.second.x,
                    positions.second.y,
                )
            else context.getString(R.string.item_desc_position_select)
        )
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        viewModel.getEditedSwipe()?.let { swipe ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    actionDescription = SwipeDescription(
                        from = swipe.getEditionFromPosition(),
                        to = swipe.getEditionToPosition(),
                        swipeDurationMs = swipe.swipeDuration ?: 250L,
                    ),
                    onConfirm = { description ->
                        (description as SwipeDescription).let { swipeDesc ->
                            viewModel.setPositions(swipeDesc.from!!.toPoint(), swipeDesc.to!!.toPoint())
                        }
                    },
                ),
                hideCurrent = true,
            )
        }
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ClickDialog because there is no action edited")
            finish()
        }
    }

    private fun Action.Swipe.getEditionFromPosition(): PointF? =
        if (fromX == null || fromY == null) null
        else PointF(fromX!!.toFloat(), fromY!!.toFloat())

    private fun Action.Swipe.getEditionToPosition(): PointF? =
        if (toX == null || toY == null) null
        else PointF(toX!!.toFloat(), toY!!.toFloat())
}

private const val TAG = "SwipeDialog"
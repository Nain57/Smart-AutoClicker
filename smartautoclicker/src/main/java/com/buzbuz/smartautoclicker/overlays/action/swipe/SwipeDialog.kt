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
package com.buzbuz.smartautoclicker.overlays.action.swipe

import android.content.Context
import android.graphics.Point
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogSwipeConfigBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.action.CoordinatesSelector
import com.buzbuz.smartautoclicker.overlays.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class SwipeDialog(
    context: Context,
    private val swipe: Action.Swipe,
    private val onDeleteClicked: (Action.Swipe) -> Unit,
    private val onConfirmClicked: (Action.Swipe) -> Unit,
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private val viewModel: SwipeViewModel by lazy {
        ViewModelProvider(this).get(SwipeViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogSwipeConfigBinding

    override fun onCreateDialog(): BottomSheetDialog {
        viewModel.setConfiguredSwipe(swipe)

        viewBinding = DialogSwipeConfigBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_action_type_swipe)

                buttonDismiss.setOnClickListener { dismiss() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setName(s.toString())
                }
            })

            editSwipeDurationText.apply {
                filters = arrayOf(DurationInputFilter())
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setSwipeDuration(if (!s.isNullOrEmpty()) s.toString().toLong() else null)
                    }
                })
            }

            onPositionSelectButton.setOnClickListener { showPositionSelector() }
        }

        return BottomSheetDialog(context).apply {
            setContentView(viewBinding.root)
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.swipeDuration.collect(::updateSwipeDuration) }
                launch { viewModel.positions.collect(::updateSwipePositionsButtonText) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredSwipe())
        dismiss()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(swipe)
        dismiss()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updateSwipeDuration(newDuration: String?) {
        viewBinding.editSwipeDurationText.setText(newDuration)
    }

    private fun updateSwipePositionsButtonText(positions: Pair<Point, Point>?) {
        if (positions == null) {
            viewBinding.onPositionSelectButton.setText(R.string.dialog_swipe_config_positions_select)
            return
        }

        viewBinding.onPositionSelectButton.text = context.getString(
            R.string.dialog_action_config_swipe_position,
            positions.first.x,
            positions.first.y,
            positions.second.x,
            positions.second.y,
        )
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        showSubOverlay(
            overlayController = ClickSwipeSelectorMenu(
                context = context,
                selector = CoordinatesSelector.Two(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.Two).let {
                        viewModel.setPositions(it.coordinates1!!, it.coordinates2!!)
                    }
                },
            ),
            hideCurrent = true,
        )
    }
}
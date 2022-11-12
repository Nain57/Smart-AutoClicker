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
package com.buzbuz.smartautoclicker.overlays.config.action.click

import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.DurationInputFilter
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionClickBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.config.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.config.action.CoordinatesSelector
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.overlays.base.bindings.setChecked
import com.buzbuz.smartautoclicker.overlays.base.utils.setError
import com.buzbuz.smartautoclicker.baseui.OnAfterTextChangedListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ClickDialog(
    context: Context,
    private val click: Action.Click,
    private val onDeleteClicked: (Action.Click) -> Unit,
    private val onConfirmClicked: (Action.Click) -> Unit,
) : OverlayDialogController(context, R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: ClickViewModel by lazy {
        ViewModelProvider(this).get(ClickViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionClickBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredClick(click)

        viewBinding = DialogConfigActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_action_type_click)

                buttonDismiss.setOnClickListener { destroy() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameText.addTextChangedListener(OnAfterTextChangedListener {
                viewModel.setName(it.toString())
            })

            editPressDurationText.apply {
                filters = arrayOf(DurationInputFilter())
                addTextChangedListener(OnAfterTextChangedListener {
                    viewModel.setPressDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                })
            }

            clickOnButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                viewModel.setClickOnCondition(checkedId == R.id.on_condition_button)
            }

            onPositionSelectButton.setOnClickListener { showPositionSelector() }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.pressDuration.collect(::updateClickDuration) }
                launch { viewModel.pressDurationError.collect(viewBinding.editPressDurationLayout::setError)}
                launch { viewModel.clickOnCondition.collect(::updateClickType) }
                launch { viewModel.position.collect(::updateClickOnPositionButtonText) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredClick())
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(click)
        destroy()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updateClickDuration(newDuration: String?) {
        viewBinding.editPressDurationText.setText(newDuration)
    }

    private fun updateClickType(clickOnCondition: Boolean) {
        viewBinding.apply {
            if (clickOnCondition) {
                clickOnButtonGroup.setChecked(onConditionButton.id)
                onConditionDesc.visibility = View.VISIBLE
                onPositionSelectButton.visibility = View.GONE
            } else {
                clickOnButtonGroup.setChecked(onPositionButton.id)
                onConditionDesc.visibility = View.GONE
                onPositionSelectButton.visibility = View.VISIBLE
            }
        }
    }

    private fun updateClickOnPositionButtonText(position: Point?) {
        if (position == null) {
            viewBinding.onPositionSelectButton.setText(R.string.dialog_click_config_on_position_select)
            return
        }

        viewBinding.onPositionSelectButton.text = context.getString(
            R.string.dialog_action_config_click_position,
            position.x,
            position.y
        )
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        showSubOverlay(
            overlayController = ClickSwipeSelectorMenu(
                context = context,
                selector = CoordinatesSelector.One(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.One).coordinates?.let { viewModel.setPosition(it) }
                },
            ),
            hideCurrent = true,
        )
    }
}
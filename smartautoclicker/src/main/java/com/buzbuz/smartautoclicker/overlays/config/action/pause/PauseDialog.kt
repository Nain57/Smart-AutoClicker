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
package com.buzbuz.smartautoclicker.overlays.config.action.pause

import android.content.Context
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
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionPauseBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.baseui.OnAfterTextChangedListener
import com.buzbuz.smartautoclicker.overlays.base.utils.setError

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class PauseDialog(
    context: Context,
    private val pause: Action.Pause,
    private val onDeleteClicked: (Action.Pause) -> Unit,
    private val onConfirmClicked: (Action.Pause) -> Unit,
) : OverlayDialogController(context, R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: PauseViewModel by lazy {
        ViewModelProvider(this).get(PauseViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionPauseBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredSwipe(pause)

        viewBinding = DialogConfigActionPauseBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_action_type_pause)

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

            editPauseDurationText.apply {
                filters = arrayOf(DurationInputFilter())
                addTextChangedListener(OnAfterTextChangedListener {
                    viewModel.setPauseDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                })
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.pauseDuration.collect(::updatePauseDuration) }
                launch { viewModel.pauseDurationError.collect(viewBinding.editPauseDurationLayout::setError)}
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredPause())
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(pause)
        destroy()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updatePauseDuration(newDuration: String?) {
        viewBinding.editPauseDurationText.setText(newDuration)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ListAdapter

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.ActionBriefMenu
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayGestureCaptureMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter.ChangeCounterDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.copy.ActionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.IntentDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause.PauseDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe.SwipeDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.ToggleEventDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.actions.ActionTypeChoice

import kotlinx.coroutines.launch


class SmartActionsBriefMenu(private val onConfigComplete: () -> Unit) : ActionBriefMenu(
    theme = R.style.ScenarioConfigTheme,
    noActionsStringRes = R.string.quick_config_empty_actions,
) {

    /** The view model for this dialog. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    private lateinit var viewBinding: OverlayGestureCaptureMenuBinding
    private lateinit var actionsBriefAdapter: ActionListAdapter

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isGestureCaptureStarted.collect(::updateRecordingState) }
                launch { viewModel.actionBriefList.collect(::updateActionList) }
                launch { viewModel.actionVisualization.collect(::updateActionVisualisation) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayGestureCaptureMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateAdapter(): ListAdapter<*, *> {
        actionsBriefAdapter = ActionListAdapter(displayMetrics) { clickedItem ->
            showActionConfigDialog(clickedItem.action)
        }
        return actionsBriefAdapter
    }

    override fun onMenuItemClicked(viewId: Int) {
        debounceUserInteraction {
            when (viewId) {
                R.id.btn_save -> {
                    onConfigComplete()
                    back()
                }
                R.id.btn_record -> onRecordClicked()
                R.id.btn_add_other -> showNewActionDialog()
                R.id.btn_copy -> showActionCopyDialog()
            }
        }
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(viewBinding.btnRecord, isVisible)
    }

    override fun onMoveItem(from: Int, to: Int) {
        viewModel.moveAction(from, to)
    }

    override fun onDeleteItem(index: Int) {
        viewModel.deleteAction(index)
    }

    override fun onPlayItem(index: Int) {
        updateReplayingState(true)
        viewModel.playAction(context, index) {
            updateReplayingState(false)
        }
    }

    private fun onRecordClicked() {
        if (isGestureCaptureStarted()) {
            viewModel.cancelGestureCaptureState()
            stopGestureCapture()
            return
        }

        viewModel.startGestureCaptureState()
        startGestureCapture { gesture, isFinished ->
            if (gesture == null || !isFinished) return@startGestureCapture

            prepareItemInsertion()
            viewModel.endGestureCaptureState(context, gesture)
        }
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedActionIndex(index)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        if (isRecording) {
            setMenuItemViewEnabled(viewBinding.btnSave, false)
            setMenuItemViewEnabled(viewBinding.btnAddOther, false)
            setMenuItemViewEnabled(viewBinding.btnCopy, false)
            setMenuItemViewEnabled(viewBinding.btnHideOverlay, false)
            setMenuItemViewEnabled(viewBinding.btnMove, true)
            setMenuItemViewEnabled(viewBinding.btnRecord, true)
        } else {
            setMenuItemViewEnabled(viewBinding.btnSave, true)
            setMenuItemViewEnabled(viewBinding.btnAddOther, true)
            setMenuItemViewEnabled(viewBinding.btnCopy, true)
            setMenuItemViewEnabled(viewBinding.btnHideOverlay, true)
            setMenuItemViewEnabled(viewBinding.btnMove, true)
            setMenuItemViewEnabled(viewBinding.btnRecord, true)
        }
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        setOverlayViewVisibility(!isReplaying)
        setMenuItemViewEnabled(viewBinding.btnSave, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnAddOther, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnCopy, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnHideOverlay, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnMove, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnRecord, !isReplaying)
    }

    private fun updateActionVisualisation(visualization: ActionDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun showNewActionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ActionTypeSelectionDialog(
                choices = buildList {
                    add(ActionTypeChoice.Click)
                    add(ActionTypeChoice.Swipe)
                    add(ActionTypeChoice.Pause)
                    add(ActionTypeChoice.ChangeCounter)
                    add(ActionTypeChoice.ToggleEvent)
                    add(ActionTypeChoice.Intent)
                },
                onChoiceSelectedListener = { choiceClicked ->
                    showActionConfigDialog(viewModel.createAction(context, choiceClicked), isNewAction = true)
                },
            ),
        )
    }

    private fun showActionCopyDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ActionCopyDialog(
                onActionSelected = { newCopyAction ->
                    showActionConfigDialog(viewModel.createNewActionFrom(newCopyAction), isNewAction = true)
                }
            ),
        )
    }

    private fun showActionConfigDialog(action: Action, isNewAction: Boolean = false) {
        viewModel.startActionEdition(action)
        val actionConfigDialogListener: OnActionConfigCompleteListener by lazy {
            object : OnActionConfigCompleteListener {
                override fun onConfirmClicked() {
                    if (isNewAction) prepareItemInsertion()
                    viewModel.upsertEditedAction()
                }
                override fun onDeleteClicked() { viewModel.removeEditedAction() }
                override fun onDismissClicked() { viewModel.dismissEditedAction() }
            }
        }

        val overlay = when (action) {
            is Action.Click -> ClickDialog(actionConfigDialogListener)
            is Action.Swipe -> SwipeDialog(actionConfigDialogListener)
            is Action.Pause -> PauseDialog(actionConfigDialogListener)
            is Action.Intent -> IntentDialog(actionConfigDialogListener)
            is Action.ToggleEvent -> ToggleEventDialog(actionConfigDialogListener)
            is Action.ChangeCounter -> ChangeCounterDialog(actionConfigDialogListener)
            else -> throw IllegalArgumentException("Not yet supported")
        }


        overlayManager.navigateTo(
            context = context,
            newOverlay = overlay,
            hideCurrent = true,
        )
    }
}

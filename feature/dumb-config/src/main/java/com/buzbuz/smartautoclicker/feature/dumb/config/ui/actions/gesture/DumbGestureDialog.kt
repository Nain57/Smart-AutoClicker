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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.DialogConfigDumbActionGestureBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.editor.DumbGestureEditorOverlayMenu

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class DumbGestureDialog(
    private val dumbGesture: DumbAction.DumbGesture,
    private val onConfirmClicked: (DumbAction.DumbGesture) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbGesture) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: DumbGestureViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbGestureViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigDumbActionGestureBinding

    private val adapter: DumbGestureDialogAdapter = DumbGestureDialogAdapter(
        onNameChanged = { name -> viewModel.setActionName(name) },
        onRepeatCountChanged = { newValue -> viewModel.setRepeatCount(newValue.toIntOrNull() ?: 0) },
        onRepeatInfiniteClicked = { viewModel.toggleInfiniteRepeat() },
        onRepeatDelayChanged = { newValue -> viewModel.setRepeatDelay(newValue.toLongOrNull() ?: 0) },
        onStartTimeChanged = { newValue -> viewModel.setFingerStartTime(newValue) },
        onFingerClicked = { fingerIndex -> viewModel.setSelectedFinger(fingerIndex) },
        onFingerAddClicked = { viewModel.addFinger() },
        onFingerDeleteClicked = { viewModel.deleteFinger() },
        onFingerRecordClicked = { showUpdateGestureScreen() },
        onExpandCollapseClicked = { item -> viewModel.expandCollapseItem(item) },
        onAddMoveClicked = { viewModel.addMove() },
        onAddWaitClicked = { viewModel.addWait() },
        onDeleteMotionClicked = { item -> viewModel.deleteMotion(item) },
        onMoveMotionUpClicked = { item -> viewModel.moveMotionUp(item) },
        onMoveMotionDownClicked = { item -> viewModel.moveMotionDown(item) },
        onDurationChanged = { item, newValue -> viewModel.setItemDurationMs(item, newValue) },
        onXValueChanged = { item, newValue -> viewModel.setItemXPosition(item, newValue) },
        onYValueChanged = { item, newValue -> viewModel.setItemYPosition(item, newValue) },
        onSelectPositionClicked = { item -> debounceUserInteraction { showPositionSelector(item) } },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbGesture(dumbGesture)

        viewBinding = DialogConfigDumbActionGestureBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_gesture)

                buttonDismiss.setDebouncedOnClickListener { onDismissButtonClicked() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteButtonClicked() }
                }
            }

            dialogContent.adapter = adapter
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbGesture()?.let { editedGesture ->
            onConfirmClicked(editedGesture)
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        viewModel.getEditedDumbGesture()?.let { editedGesture ->
            onDeleteClicked(editedGesture)
            back()
        }
    }

    private fun onDismissButtonClicked() {
        onDismissClicked()
        back()
    }

    private fun updateUiState(state: DumbGestureDialogUiState) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)
        adapter.submitList(state.uiItems)
    }

    private fun showUpdateGestureScreen() {
        val gesture = viewModel.getEditedDumbGesture() ?: return
        overlayManager.navigateTo(
            context = context,
            newOverlay = DumbGestureEditorOverlayMenu(
                gesture = gesture,
                mode = DumbGestureEditorOverlayMenu.DumbGestureEditorMode.AllMotions(
                    fingerIndex = viewModel.selectedFinger.value,
                    onGestureEdited = { viewModel.updateGesture(it) },
                ),
            ),
            hideCurrent = true,
        )
    }

    private fun showPositionSelector(item: DumbGestureDialogUiStateItem) {
        val motionId = (item as? DumbGestureDialogUiStateItem.Collapsable)?.id ?: return
        val gesture = viewModel.getEditedDumbGesture() ?: return

        overlayManager.navigateTo(
            context = context,
            newOverlay = DumbGestureEditorOverlayMenu(
                gesture = gesture,
                mode = DumbGestureEditorOverlayMenu.DumbGestureEditorMode.SingleMotion(
                    motionId = motionId,
                    onPositionConfirmed = { position -> viewModel.setItemPosition(item, position) },
                ),
            ),
            hideCurrent = true,
        )
    }
}

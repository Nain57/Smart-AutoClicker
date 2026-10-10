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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture

import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionGestureBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.editor.GestureEditorOverlayMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.offset.GestureOffsetDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue

class GestureDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.GESTURE.name

    /** The view model for this dialog. */
    private val viewModel: GestureDialogViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { gestureDialogViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionGestureBinding

    private val adapter: GestureDialogAdapter = GestureDialogAdapter(
        bitmapProvider = { condition, onBitmapLoaded -> viewModel.getConditionBitmap(condition, onBitmapLoaded) },
        onNameChanged = { name -> viewModel.setActionName(name) },
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
        onReleaseWhenSelected = { item, whenItem -> viewModel.setItemReleaseWhen(item, whenItem) },
        onXValueChanged = { item, newValue -> viewModel.setItemXPosition(item, newValue) },
        onYValueChanged = { item, newValue -> viewModel.setItemYPosition(item, newValue) },
        onSetCounterXClicked = { item ->
            showCounterSelectionDialog { counterName -> viewModel.setItemXCounter(item, counterName) }
        },
        onSetCounterYClicked = { item ->
            showCounterSelectionDialog { counterName -> viewModel.setItemYCounter(item, counterName) }
        },
        onSelectPositionClicked = { item ->
            showPositionSelector(item) { position -> viewModel.setItemPosition(item, position) }
        },
        onOffsetFieldClicked = { item ->
            showClickOffsetDialog(item) { offset -> viewModel.setItemOffset(item, offset) }
        },
        onSelectConditionClicked = { item ->
            if (viewModel.shouldSelectCondition())
                showConditionSelector { condition -> viewModel.setItemCondition(item, condition) }
            else viewModel.setItemCondition(item, null)
        },
        onRemoveConditionClicked = { item -> viewModel.removeItemCondition(item) },
    )

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionGestureBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_gesture)

                buttonDismiss.setDebouncedOnClickListener { back() }
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
            repeatOnLifecycle(Lifecycle.State.CREATED) {

            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }

        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveButtonClicked() {
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun updateUiState(state: GestureDialogUiState) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)
        adapter.submitList(state.uiItems)
    }

    private fun showUpdateGestureScreen() {
        val gesture = viewModel.getCurrentGesture() ?: return
        overlayManager.navigateTo(
            context = context,
            newOverlay = GestureEditorOverlayMenu(
                gesture = gesture,
                mode = GestureEditorOverlayMenu.GestureEditorMode.AllMotions(
                    fingerIndex = viewModel.selectedFinger.value,
                    onGestureEdited = { viewModel.updateGesture(it) },
                ),
            ),
            hideCurrent = true,
        )
    }

    private fun showPositionSelector(item: GestureDialogUiStateItem, onPositionSelected: (Point) -> Unit) {
        val motionId = (item as? GestureDialogUiStateItem.Collapsable)?.id ?: return
        val gesture = viewModel.getCurrentGesture() ?: return

        overlayManager.navigateTo(
            context = context,
            newOverlay = GestureEditorOverlayMenu(
                gesture = gesture,
                mode = GestureEditorOverlayMenu.GestureEditorMode.SingleMotion(
                    motionId = motionId,
                    onPositionConfirmed = onPositionSelected,
                ),
            ),
            hideCurrent = true,
        )
    }

    private fun showCounterSelectionDialog(onCounterSelected: (String) -> Unit) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog(onCounterSelected),
            hideCurrent = false,
        )
    }

    private fun showConditionSelector(onConditionSelected: (ScreenCondition) -> Unit) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScreenConditionSelectionDialog(
                conditionList = viewModel.getConditionsForPosition(),
                onConditionSelected = onConditionSelected,
            ),
            hideCurrent = false,
        )
    }

    private fun showClickOffsetDialog(item: GestureDialogUiStateItem, onOffsetSelected: (Position.Coordinates) -> Unit) {
        val currentOffset = viewModel.getItemOffset(item) ?: return
        overlayManager.navigateTo(
            context = context,
            newOverlay = GestureOffsetDialog(
                currentOffset = currentOffset,
                onNewOffset = onOffsetSelected,
            ),
            hideCurrent = false,
        )
    }
}
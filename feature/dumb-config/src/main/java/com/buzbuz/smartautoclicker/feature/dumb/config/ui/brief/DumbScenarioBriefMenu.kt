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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.brief

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MoveToDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.OverlayDumbScenarioBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionEditionUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionDetails

import kotlinx.coroutines.launch

class DumbScenarioBriefMenu(
    private val onConfigSaved: () -> Unit,
) : ItemBriefMenu(
    theme = R.style.AppTheme,
    noItemText = R.string.message_dumb_brief_empty_action_list,
) {

    /** The view model for this menu. */
    private val viewModel: DumbScenarioBriefViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioBriefViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var menuViewBinding: OverlayDumbScenarioBriefMenuBinding

    private lateinit var dumbActionCreator: DumbActionCreator
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isGestureCaptureStarted.collect(::updateRecordingState) }
                launch { viewModel.dumbActionsBriefList.collect(::updateItemList) }
                launch { viewModel.dumbActionVisualization.collect(::updateDumbActionVisualisation) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = { position -> viewModel.createNewDumbClick(context, position) },
            createNewDumbSwipe = { from, to -> viewModel.createNewDumbSwipe(context, from, to) },
            createNewDumbPause = { viewModel.createNewDumbPause(context) },
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = { action -> viewModel.addNewDumbAction(action, getFocusedItemIndex() + 1) },
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )

        menuViewBinding = OverlayDumbScenarioBriefMenuBinding.inflate(layoutInflater)
        return menuViewBinding.root
    }

    override fun onCreateBriefItemViewHolder(parent: ViewGroup, viewType: Int, orientation: Int): DumbActionBriefViewHolder =
        DumbActionBriefViewHolder(LayoutInflater.from(parent.context), orientation, parent)

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(menuViewBinding.btnRecord, isVisible)
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedDumbActionIndex(index)
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapDumbActions(from, to)
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteDumbAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        updateReplayingState(true)
        viewModel.playAction(index) {
            updateReplayingState(false)
        }
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showDumbActionEditionUiFlow((item.data as DumbActionDetails).action)
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopAction()) {
                    keyDownHandled = true
                    return true
                }
            }

            KeyEvent.ACTION_UP -> {
                if (keyDownHandled) {
                    keyDownHandled = false
                    return true
                }
            }
        }

        return false
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_back -> onBackClicked()
            R.id.btn_record -> onRecordClicked()
            R.id.btn_add -> onCreateDumbActionClicked()
        }
    }

    private fun onBackClicked() {
        if (isGestureCaptureStarted()) {
            viewModel.cancelGestureCaptureState()
            stopGestureCapture()
            return
        }

        if (viewModel.stopAction()) return

        onConfigSaved()
        back()
    }

    private fun onRecordClicked() {
        if (isGestureCaptureStarted()) return

        viewModel.startGestureCaptureState()
        startGestureCapture { gesture, isFinished ->
            if (gesture == null || !isFinished) return@startGestureCapture
            viewModel.endGestureCaptureState(context, gesture)
        }
    }

    private fun onCreateDumbActionClicked() {
        hidePanel()
        showDumbActionCreationUiFlow()
    }

    private fun updateDumbActionVisualisation(details: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(details, true)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        if (isRecording) {
            setMenuItemViewEnabled(menuViewBinding.btnBack, true)
            setMenuItemViewEnabled(menuViewBinding.btnAdd, false)
            setMenuItemViewEnabled(menuViewBinding.btnHideOverlay, false)
            setMenuItemViewEnabled(menuViewBinding.btnMove, true)
            setMenuItemViewEnabled(menuViewBinding.btnRecord, false)
        } else {
            setMenuItemViewEnabled(menuViewBinding.btnBack, true)
            setMenuItemViewEnabled(menuViewBinding.btnAdd, true)
            setMenuItemViewEnabled(menuViewBinding.btnHideOverlay, true)
            setMenuItemViewEnabled(menuViewBinding.btnMove, true)
            setMenuItemViewEnabled(menuViewBinding.btnRecord, true)
        }
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        setOverlayViewVisibility(!isReplaying)
        setMenuItemViewEnabled(menuViewBinding.btnBack, true)
        setMenuItemViewEnabled(menuViewBinding.btnAdd, !isReplaying)
        setMenuItemViewEnabled(menuViewBinding.btnHideOverlay, !isReplaying)
        setMenuItemViewEnabled(menuViewBinding.btnMove, !isReplaying)
        setMenuItemViewEnabled(menuViewBinding.btnRecord, !isReplaying)
    }

    private fun showDumbActionCreationUiFlow(): Unit =
        overlayManager.startDumbActionCreationUiFlow(
            context = context,
            creator = dumbActionCreator,
            listener = createCopyActionUiFlowListener,
        )

    private fun showDumbActionEditionUiFlow(action: DumbAction): Unit =
        overlayManager.startDumbActionEditionUiFlow(
            context = context,
            dumbAction = action,
            listener = updateActionUiFlowListener,
        )

    private fun showMoveToDialog(index: Int, itemCount: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.AppTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                onValueSelected = { value ->
                    if (value - 1 == index) return@MoveToDialog
                    viewModel.moveDumbAction(index, value - 1)
                }
            ),
        )
    }
}

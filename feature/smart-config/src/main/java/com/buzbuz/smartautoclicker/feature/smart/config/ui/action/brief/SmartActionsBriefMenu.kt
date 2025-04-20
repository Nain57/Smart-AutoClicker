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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MoveToDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayActionsBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.UiAction

import kotlinx.coroutines.launch


class SmartActionsBriefMenu(initialItemIndex: Int) : ItemBriefMenu(
    theme = R.style.ScenarioConfigTheme,
    noItemText = R.string.brief_empty_actions,
    initialItemIndex = initialItemIndex,
) {

    /** The view model for this dialog. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    private lateinit var viewBinding: OverlayActionsBriefMenuBinding

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
                launch { viewModel.actionBriefList.collect(::updateItemList) }
                launch { viewModel.actionVisualization.collect(::updateActionVisualisation) }
                launch { viewModel.isTutorialModeEnabled.collect(::updateTutorialModeState) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayActionsBriefMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateBriefItemViewHolder(parent: ViewGroup, viewType: Int, orientation: Int): SmartActionBriefViewHolder =
        SmartActionBriefViewHolder(LayoutInflater.from(parent.context), orientation, parent)

    override fun onBriefItemViewBound(index: Int, itemView: View?) {
        if (index != 0) return

        if (itemView != null) viewModel.monitorBriefFirstItemView(itemView)
        else viewModel.stopBriefFirstItemMonitoring()
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            createMenuButton = viewBinding.btnAddOther,
            saveMenuButton = viewBinding.btnBack,
        )
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopAllViewMonitoring()
    }

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showActionConfigDialog((item.data as UiAction).action)
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopAction()) {
                    keyDownHandled = true
                    updateReplayingState(false)
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
            R.id.btn_add_other -> showNewActionDialog()
        }
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(viewBinding.btnRecord, isVisible)
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapActions(from, to)
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        updateReplayingState(true)
        viewModel.playAction(context, index) {
            updateReplayingState(false)
        }
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    private fun onBackClicked() {
        if (isGestureCaptureStarted()) {
            viewModel.cancelGestureCaptureState()
            stopGestureCapture()
            return
        }

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

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedActionIndex(index)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        if (isRecording) {
            setMenuItemViewEnabled(viewBinding.btnBack, true)
            setMenuItemViewEnabled(viewBinding.btnAddOther, false)
            setMenuItemViewEnabled(viewBinding.btnHideOverlay, false)
            setMenuItemViewEnabled(viewBinding.btnMove, true)
            setMenuItemViewEnabled(viewBinding.btnRecord, false)
        } else {
            setMenuItemViewEnabled(viewBinding.btnBack, true)
            setMenuItemViewEnabled(viewBinding.btnAddOther, true)
            setMenuItemViewEnabled(viewBinding.btnHideOverlay, true)
            setMenuItemViewEnabled(viewBinding.btnMove, true)
            setMenuItemViewEnabled(viewBinding.btnRecord, true)
        }
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        setOverlayViewVisibility(!isReplaying)
        setMenuItemViewEnabled(viewBinding.btnBack, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnAddOther, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnHideOverlay, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnMove, !isReplaying)
        setMenuItemViewEnabled(viewBinding.btnRecord, !isReplaying)
    }

    private fun updateActionVisualisation(visualization: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun updateTutorialModeState(isTutorialEnabled: Boolean) {
        setBriefPanelAutoHide(!isTutorialEnabled)
    }

    private fun showMoveToDialog(index: Int, itemCount: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.ScenarioConfigTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                onValueSelected = { value ->
                    if (value - 1 == index) return@MoveToDialog
                    viewModel.moveAction(index, value - 1)
                }
            ),
        )
    }

    private fun showNewActionDialog() {
        showActionTypeSelectionDialog(viewModel)
    }

    private fun showActionConfigDialog(action: Action) {
        showActionConfigDialog(viewModel, action)
    }
}

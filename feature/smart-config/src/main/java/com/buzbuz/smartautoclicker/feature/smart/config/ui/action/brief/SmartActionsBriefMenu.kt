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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.common.navigation.TutorialNavigator
import com.buzbuz.smartautoclicker.core.common.navigation.getTutorialNavigator
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MoveToDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.Tip
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayActionsBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.UiAction

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType


class SmartActionsBriefMenu(initialItemIndex: Int) : ItemBriefMenu(
    theme = R.style.ScenarioConfigTheme,
    noItemText = R.string.brief_empty_actions,
    initialItemIndex = initialItemIndex,
) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU.name

    /** The view model for this dialog. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    private val tutorialNavigator: TutorialNavigator by lazy {
        context.getTutorialNavigator()
    }

    private lateinit var viewBinding: OverlayActionsBriefMenuBinding

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false

    /** Last mode applied to the menu; overlay visibility must only be touched on actual mode transitions. */
    private var currentMode: SmartActionsBriefMode? = null


    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.filterNotNull().collect(::updateUiState)
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayActionsBriefMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateBriefItemViewHolder(parent: ViewGroup, orientation: Int): SmartActionBriefViewHolder =
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
        currentMode = null
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
            R.id.btn_help -> showHelpDialog()
        }
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        updateRecordButtonState(isOverlayVisible = isVisible)
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapActions(from, to)
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        viewModel.playAction(context, index)
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    private fun onBackClicked() {
        if (viewModel.isGestureCaptureStarted()) {
            viewModel.cancelGestureCapture()
            stopGestureCapture()
            return
        }

        back()
    }

    private fun onRecordClicked() {
        if (viewModel.isGestureCaptureStarted()) return

        viewModel.startGestureCapture()
        startGestureCapture { gesture, isFinished ->
            if (!isFinished) return@startGestureCapture
            viewModel.endGestureCapture(context, gesture)
        }
    }

    override fun onGestureCaptureCancelled() {
        viewModel.cancelGestureCapture()
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedActionIndex(index)
    }

    private fun updateUiState(uiState: SmartActionsBriefUiState) {
        updateItemList(uiState.actionBriefs)
        updateMode(uiState.mode)
        if (!uiState.isRecordedGestureDisplayed) {
            briefViewBinding.viewBrief.setDescription(uiState.focusedActionDescription, true)
        }
        setBriefPanelAutoHide(uiState.isBriefPanelAutoHideEnabled)
    }

    private fun updateMode(mode: SmartActionsBriefMode) {
        val previousMode = currentMode
        if (mode == previousMode) return
        currentMode = mode

        val isTesting = mode == SmartActionsBriefMode.ACTION_TESTING
        val isIdle = mode == SmartActionsBriefMode.IDLE

        setMenuItemViewEnabled(viewBinding.btnBack, !isTesting)
        setMenuItemViewEnabled(viewBinding.btnMove, !isTesting)
        setMenuItemViewEnabled(viewBinding.btnAddOther, isIdle)
        setMenuItemViewEnabled(viewBinding.btnHideOverlay, isIdle)

        // Showing the overlay also re-shows the brief panel, which must stay hidden while capturing a gesture.
        // Only touch it when the testing state changes (or on a restart), the record button is refreshed either way.
        val isTestingTransition = isTesting || previousMode == SmartActionsBriefMode.ACTION_TESTING
        if (isTestingTransition || previousMode == null) setOverlayViewVisibility(!isTesting)
        else updateRecordButtonState(isOverlayVisible = screenOverlayView?.isVisible == true)
    }

    /** Recording needs both an idle menu and a visible overlay, whichever of the two changed last. */
    private fun updateRecordButtonState(isOverlayVisible: Boolean) {
        setMenuItemViewEnabled(
            viewBinding.btnRecord,
            isOverlayVisible && currentMode == SmartActionsBriefMode.IDLE,
        )
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

    private fun showHelpDialog() {
        tutorialNavigator.showTipDialog(context, Tip.ACTIONS_BRIEF)
    }
}

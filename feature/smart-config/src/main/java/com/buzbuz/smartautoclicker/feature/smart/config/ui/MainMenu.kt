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
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.DialogInterface
import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.AnimatedStatesImageButtonController
import com.buzbuz.smartautoclicker.core.ui.utils.getDynamicColorsContext
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.languages.LanguageFilesDownloadDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.newRestartMediaProjectionStarterOverlay
import com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.ScenarioDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.DebugModel
import com.buzbuz.smartautoclicker.feature.tutorial.ui.dialogs.createStopWithVolumeDownTutorialDialog

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [OverlayMenu] implementation for displaying the main menu overlay.
 *
 * This is the menu displayed once the service is started via the [com.buzbuz.smartautoclicker.scenarios.ScenarioActivity]
 * once the user has selected a scenario to be used. It allows the user to start the detection on the currently loaded
 * scenario, as well as editing the attached list of events.
 *
 * There is no overlay views attached to this overlay menu, meaning that the user will always be able to clicks on the
 * Activities displayed below it.
 */
class MainMenu(private val onStopClicked: () -> Unit) : OverlayMenu() {

    /** The view model for this menu. */
    private val viewModel: MainMenuModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { mainMenuViewModel() },
    )
    /** The view model for the debugging features. */
    private val debuggingViewModel: DebugModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugModel() },
    )

    private var isHiddenForPaywall: Boolean = false

    /** View binding for the content of the overlay. */
    private lateinit var viewBinding: OverlayMenuBinding
    /** Controls the animations of the play/pause button. */
    private lateinit var playPauseButtonController: AnimatedStatesImageButtonController

    /** The coroutine job for the observable used in debug mode. Null when not in debug mode. */
    private var debugObservableJob: Job? = null

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        playPauseButtonController = AnimatedStatesImageButtonController(
            context = context,
            state1StaticRes = R.drawable.ic_play_arrow,
            state2StaticRes = R.drawable.ic_pause,
            state1to2AnimationRes = R.drawable.anim_play_pause,
            state2to1AnimationRes = R.drawable.anim_pause_play,
        )
        viewBinding = OverlayMenuBinding.inflate(layoutInflater)
        playPauseButtonController.attachView(viewBinding.btnPlay)

        return viewBinding.root
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure the debug view state is correct
        viewBinding.layoutDebug.visibility = View.GONE
        setOverlayViewVisibility(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.paywallIsVisible.collect(::updateVisibilityForPaywall) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isStartButtonEnabled.collect(::updatePlayPauseButtonEnabledState) }
                launch { viewModel.isMediaProjectionStarted.collect(::updateProjectionErrorBadge) }
                launch { viewModel.detectionState.collect(::updateDetectionState) }
                launch { viewModel.nativeLibError.collect(::showNativeLibErrorDialogIfNeeded) }
                launch { debuggingViewModel.isDebugging.collect(::updateDebugOverlayViewVisibility) }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.monitorViews(
            playMenuButton = viewBinding.btnPlay,
            configMenuButton = viewBinding.btnClickList,
        )

        // Start loading advertisement if needed
        viewModel.loadAdIfNeeded(context)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        playPauseButtonController.detachView()
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopDetection()) {
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
            R.id.btn_play -> onPlayPauseClicked()
            R.id.btn_click_list -> onConfigureClicked()
            R.id.btn_stop -> onStopClicked()
        }
    }

    override fun getWindowMaximumSize(backgroundView: ViewGroup): Size {
        val bgSize = super.getWindowMaximumSize(backgroundView)
        return Size(
            bgSize.width + context.resources.getDimensionPixelSize(R.dimen.overlay_debug_panel_width),
            bgSize.height,
        )
    }

    fun onMediaProjectionLost() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return

        overlayManager.navigateUpToRoot(context)
        viewModel.cancelScenarioChanges()
    }

    private fun onConfigureClicked() {
        if (viewModel.shouldRestartMediaProjection()) {
            showRestartMediaProjectionScreen()
            return
        }

        viewModel.startScenarioEdition {
            showScenarioConfigDialog()
        }
    }

    private fun onPlayPauseClicked() {
        if (viewModel.shouldRestartMediaProjection()) {
            showRestartMediaProjectionScreen()
            return
        }

        if (viewModel.shouldDownloadTextLanguageFiles()) {
            showDownloadTextLanguageFileDialog()
            return
        }

        if (viewModel.shouldShowStopVolumeDownTutorialDialog()) {
            showStopVolumeDownTutorialDialog()
            return
        }

        viewModel.toggleDetection(context)
    }

    /** Refresh the play menu item according to the scenario state. */
    private fun updatePlayPauseButtonEnabledState(canStartDetection: Boolean) =
        setMenuItemViewEnabled(viewBinding.btnPlay, canStartDetection)

    /** Refresh the menu layout according to the detection state. */
    private fun updateDetectionState(newState: UiState) {
        val currentState = viewBinding.btnPlay.tag
        if (currentState == newState) return

        viewBinding.btnPlay.tag = newState
        when (newState) {
            UiState.Idle -> {
                if (currentState == null) {
                    playPauseButtonController.toState1(false)
                } else {
                    animateLayoutChanges {
                        setMenuItemVisibility(viewBinding.btnStop, true)
                        setMenuItemVisibility(viewBinding.btnClickList, true)
                        playPauseButtonController.toState1(true)
                    }
                }
            }

            UiState.Detecting -> {
                if (currentState == null) {
                    playPauseButtonController.toState2(false)
                } else {
                    animateLayoutChanges {
                        setMenuItemVisibility(viewBinding.btnStop, false)
                        setMenuItemVisibility(viewBinding.btnClickList, false)
                        playPauseButtonController.toState2(true)
                    }
                }
            }
        }
    }

    private fun updateVisibilityForPaywall(isHidden: Boolean) {
        if (isHidden) {
            isHiddenForPaywall = true
            hide()
        } else if (isHiddenForPaywall) {
            isHiddenForPaywall = false
            show()
        }
    }

    /**
     * Change the debug state of this UI.
     * @param isVisible true when the debug view should be shown, false to hide it.
     */
    private fun updateDebugOverlayViewVisibility(isVisible: Boolean) {
        if (isVisible && debugObservableJob == null) {
            viewBinding.layoutDebug.visibility = View.VISIBLE
            debugObservableJob = observeDebugValues()

        } else if (!isVisible && debugObservableJob != null) {
            debugObservableJob?.cancel()
            debugObservableJob = null

            viewBinding.debugEventName.text = null
            viewBinding.debugConditionName.text = null
            viewBinding.debugConfidenceRate.text = null
            viewBinding.layoutDebug.visibility = View.GONE
        }
    }

    private fun updateProjectionErrorBadge(isProjectionStarted: Boolean) {
        viewBinding.errorBadge.visibility = if (isProjectionStarted) View.GONE else View.VISIBLE
    }

    /**
     * Observe the values for the debug and update the debug views.
     * @return the coroutine job for the observable. Can be cancelled to stop the observation.
     */
    private fun observeDebugValues() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                debuggingViewModel.debugLastPositive.collect { debugInfo ->
                    viewBinding.debugEventName.text = debugInfo.eventName
                    viewBinding.debugConditionName.text = debugInfo.conditionName
                    viewBinding.debugConfidenceRate.text = debugInfo.confidenceRateText
                }
            }
        }
    }

    private fun showScenarioConfigDialog() =
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScenarioDialog(
                onConfigDiscarded = viewModel::cancelScenarioChanges,
                onConfigSaved = { viewModel.saveScenarioChanges { success -> if (!success) showScenarioSaveErrorDialog() } },
            ),
            hideCurrent = true,
        )

    private fun showScenarioSaveErrorDialog() {
        MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.error_dialog_message_scenario_saving)
            .setPositiveButton(R.string.generic_modify) { _: DialogInterface, _: Int ->
                showScenarioConfigDialog()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int ->
                viewModel.cancelScenarioChanges()
            }
            .create()
            .showAsOverlay()
    }

    private fun showStopVolumeDownTutorialDialog() {
        context.createStopWithVolumeDownTutorialDialog(
            theme = R.style.AppTheme,
            onDismissed = { showAgain ->
                if (!showAgain) viewModel.setStopWithVolumeDownDontShowAgain()
                viewModel.toggleDetection(context)
            }
        ).showAsOverlay()
    }

    private fun showNativeLibErrorDialogIfNeeded(haveError: Boolean) {
        if (!haveError) return

        MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.error_dialog_message_error_native_lib)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onStopClicked()
            }
            .create()
            .showAsOverlay()
    }

    private fun showRestartMediaProjectionScreen() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = newRestartMediaProjectionStarterOverlay(context),
            hideCurrent = true,
        )
    }

    private fun showDownloadTextLanguageFileDialog() {
        val scenarioId = viewModel.getScenarioId() ?: return

        overlayManager.navigateTo(
            context = context,
            newOverlay = LanguageFilesDownloadDialog(
                scenarioDbId = scenarioId,
                onDownloadsCompleted = ::onPlayPauseClicked,
            ),
            hideCurrent = true,
        )
    }
}
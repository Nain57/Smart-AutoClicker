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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.overlay

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.AnimatedStatesImageButtonController
import com.buzbuz.smartautoclicker.feature.scenario.debugging.R
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.OverlayTryElementMenuBinding

import kotlinx.coroutines.launch

class TryElementOverlayMenu(
    private val scenario: Scenario,
    private val triedElement: Any,
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: TryElementViewModel by viewModels()

    private lateinit var viewBinding: OverlayTryElementMenuBinding
    /** Controls the animations of the play/pause button. */
    private lateinit var playPauseButtonController: AnimatedStatesImageButtonController

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewModel.setTriedElement(scenario, triedElement)

        viewBinding = OverlayTryElementMenuBinding.inflate(LayoutInflater.from(context))
        playPauseButtonController = AnimatedStatesImageButtonController(
            context = context,
            state1StaticRes = R.drawable.ic_play_arrow,
            state2StaticRes = R.drawable.ic_pause,
            state1to2AnimationRes = R.drawable.anim_play_pause,
            state2to1AnimationRes = R.drawable.anim_pause_play,
        )
        playPauseButtonController.attachView(viewBinding.btnPlay)

        return viewBinding.root
    }

    override fun onCreateOverlayView(): DebugOverlayView = DebugOverlayView(context)

    override fun onStart() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canPlay.collect(::updatePlayPauseEnabledState) }
                launch { viewModel.isPlaying.collect(::updateDetectionState) }
                launch { viewModel.detectionResults.collect(::updateDetectionResults) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playPauseButtonController.detachView()
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_play ->
                viewModel.toggleTryState(context)

            R.id.btn_back -> {
                viewModel.stopTry()
                back()
            }
        }
    }

    private fun updatePlayPauseEnabledState(isEnabled: Boolean) {
        setMenuItemViewEnabled(viewBinding.btnPlay, isEnabled)
    }

    private fun updateDetectionState(isDetecting: Boolean) {
        val currentState = viewBinding.btnPlay.tag
        if (currentState == isDetecting) return
        viewBinding.btnPlay.tag = isDetecting

        if (!isDetecting) playPauseButtonController.toState1(currentState != null)
        else playPauseButtonController.toState2(currentState != null)
    }

    private fun updateDetectionResults(results: List<DetectionResultInfo>) {
        (screenOverlayView as? DebugOverlayView)?.setResults(results)
    }
}
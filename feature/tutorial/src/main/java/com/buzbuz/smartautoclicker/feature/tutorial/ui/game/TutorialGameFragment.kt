/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialGameBinding
import com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay.TutorialFullscreenOverlay

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TutorialGameFragment : Fragment() {

    /** ViewModel providing the state of the UI. */
    private val viewModel: TutorialGameViewModel by viewModels()
    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentTutorialGameBinding
    /** Tells if the time blinking animation is started or not. */
    private var isTimeAnimationStarted: Boolean = false

    @Inject lateinit var overlayManager: OverlayManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTutorialGameBinding.inflate(inflater, container, false).apply {
            blueTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.BLUE) }
            redTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.RED) }

            val targetSize = root.context.resources.getDimensionPixelSize(R.dimen.tutorial_game_target_size)
            buttonStartRetry.setOnClickListener { viewModel.startGame(gameArea.area(), targetSize) }
        }

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lockMenuPosition()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUi) }
                launch { viewModel.shouldDisplayStepOverlay.collect(::showHideStepOverlay) }
                launch { viewModel.shouldDisplayFloatingUi.collect(::showHideFloatingUi) }
                launch {
                    viewModel.shouldStopGame.collect { shouldStop ->
                        if (shouldStop) findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (viewModel.shouldDisplayFloatingUi.value && overlayManager.isOverlayStackHidden()) {
            overlayManager.restoreVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopDetection()

        if (viewModel.shouldDisplayFloatingUi.value) {
            overlayManager.hideAll()
        }
        overlayManager.removeTopOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTutorial()
    }

    private fun updateUi(uiState: TutorialGameUiState?) {
        uiState ?: return

        viewBinding.apply {
            textInstructions.text = requireContext().getString(uiState.instructionsResId)
            textHighScore.text = requireContext()
                .getString(R.string.message_high_score, uiState.highScore)
            footer.textTimeLeft.text = requireContext()
                .getString(R.string.message_time_left, uiState.timerValue)
            textScore.text = requireContext()
                .getString(R.string.message_score, uiState.gameScore)

            if (!uiState.isGameStarted) {
                buttonStartRetry.visibility = View.VISIBLE
                blueTarget.visibility = View.GONE
                redTarget.visibility = View.GONE
            } else {
                buttonStartRetry.visibility = View.GONE

                blueTarget.updateTargetState(uiState.targets[TutorialGameTargetType.BLUE])
                redTarget.updateTargetState(uiState.targets[TutorialGameTargetType.RED])
                gameArea.forceLayout()
            }

            if (uiState.isGameStarted && !isTimeAnimationStarted) {
                isTimeAnimationStarted = true
                footer.textTimeLeft.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.anim_timer_blink)
                )
            } else if (!uiState.isGameStarted && isTimeAnimationStarted) {
                isTimeAnimationStarted = false
                footer.textTimeLeft.clearAnimation()
            }
        }
    }

    private fun showHideStepOverlay(show: Boolean) {
        if (overlayManager.isOverlayStackVisible()) {
            viewBinding.spaceOverlayMenu.visibility = View.INVISIBLE
        } else {
            viewBinding.spaceOverlayMenu.visibility = View.VISIBLE
        }

        overlayManager.apply {
            if (show) setTopOverlay(TutorialFullscreenOverlay())
            else removeTopOverlay()
        }
    }

    private fun showHideFloatingUi(show: Boolean) {
        overlayManager.apply {
            if (show) restoreVisibility()
            else hideAll()
        }
    }

    private fun lockMenuPosition() {
        val location = IntArray(2)
        viewBinding.spaceOverlayMenu.getLocationInWindow(location)

        overlayManager.lockMenuPosition(
            Point(
                viewBinding.spaceOverlayMenu.marginStart + location[0],
                viewBinding.spaceOverlayMenu.marginTop + location[1],
            )
        )
    }
}

private fun AppCompatImageView.updateTargetState(position: PointF?) {
    if (position == null) {
        visibility = View.GONE
        return
    }

    visibility = View.VISIBLE
    x = position.x
    y = position.y
}

private fun FrameLayout.area(): Rect =
    Rect(0, 0, width, height)

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
import androidx.navigation.fragment.navArgs

import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialGameBinding
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGame
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings.setHeaderInfo
import com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings.setScore
import com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay.TutorialFullscreenOverlay

import kotlinx.coroutines.launch

class TutorialGameFragment : Fragment() {

    /** ViewModel providing the state of the UI. */
    private val viewModel: TutorialGameViewModel by viewModels()
    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentTutorialGameBinding
    /** Start arguments for this fragment. */
    private val args: TutorialGameFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startTutorial(args.gameIndex)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTutorialGameBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lockMenuPosition()
        OverlayManager.getInstance(requireContext()).hideAll()

        viewBinding.apply {
            blueTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.BLUE) }
            redTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.RED) }

            val targetSize = root.context.resources.getDimensionPixelSize(R.dimen.tutorial_game_target_size)
            buttonStartRetry.setOnClickListener { viewModel.startGame(gameArea.area(), targetSize) }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentGame.collect(::onGameUpdated) }
                launch { viewModel.isStarted.collect(::onGameStartedUpdated) }
                launch { viewModel.gameTimerValue.collect(::onTimerUpdated) }
                launch { viewModel.gameScore.collect(::onScoreUpdated) }
                launch { viewModel.playRetryBtnVisibility.collect(::onPlayRetryButtonStateUpdated) }
                launch { viewModel.gameTargets.collect(::onTargetsUpdated) }
                launch { viewModel.shouldDisplayStepOverlay.collect(::showHideStepOverlay) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val overlayManager = OverlayManager.getInstance(requireContext())
        overlayManager.removeTopOverlay()
        overlayManager.navigateUpToRoot(requireContext()) {
            overlayManager.unlockMenuPosition()
            viewModel.stopTutorial()
        }
    }

    private fun onGameUpdated(tutorialGame: TutorialGame?) {
        tutorialGame ?: return
        viewBinding.header.setHeaderInfo(tutorialGame.instructionsResId, tutorialGame.highScore)
    }

    private fun onGameStartedUpdated(isStarted: Boolean) {
        if (isStarted) {
            viewBinding.footer.textTimeLeft.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.anim_timer_blink)
            )
        } else {
            viewBinding.footer.textTimeLeft.clearAnimation()
        }
    }

    private fun onTimerUpdated(timerValue: Int) {
        viewBinding.footer.textTimeLeft.text = requireContext().getString(R.string.message_time_left, timerValue)
    }

    private fun onScoreUpdated(score: Int) {
        viewBinding.header.setScore(score)
    }

    private fun onPlayRetryButtonStateUpdated(isVisible: Boolean) {
        if (isVisible) {
            viewBinding.buttonStartRetry.visibility = View.VISIBLE
            viewBinding.blueTarget.visibility = View.GONE
            viewBinding.redTarget.visibility = View.GONE
        } else {
            viewBinding.buttonStartRetry.visibility = View.GONE
        }
    }

    private fun onTargetsUpdated(targets: Map<TutorialGameTargetType, PointF>) {
        viewBinding.blueTarget.updateTargetState(targets[TutorialGameTargetType.BLUE])
        viewBinding.redTarget.updateTargetState(targets[TutorialGameTargetType.RED])
        viewBinding.gameArea.forceLayout()
    }

    private fun showHideStepOverlay(show: Boolean) {
        if (OverlayManager.getInstance(requireContext()).isOverlayStackVisible()) {
            viewBinding.spaceOverlayMenu.visibility = View.INVISIBLE
        } else {
            viewBinding.spaceOverlayMenu.visibility = View.VISIBLE
        }

        OverlayManager.getInstance(requireContext()).apply {
            if (show) setTopOverlay(TutorialFullscreenOverlay())
            else removeTopOverlay()
        }
    }

    private fun lockMenuPosition() {
        val location = IntArray(2)
        viewBinding.spaceOverlayMenu.getLocationInWindow(location)

        OverlayManager.getInstance(requireContext())
            .lockMenuPosition(
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

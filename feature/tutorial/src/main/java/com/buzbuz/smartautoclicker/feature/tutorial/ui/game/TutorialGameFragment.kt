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
import android.widget.FrameLayout

import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.marginStart
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
import com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings.setNextLevelBtnVisibility
import com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings.setScore
import com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings.setTimeLeft
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

        viewBinding.apply {
            blueTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.BLUE) }
            redTarget.setOnClickListener { viewModel.onTargetHit(TutorialGameTargetType.RED) }

            val targetSize = root.context.resources.getDimensionPixelSize(R.dimen.tutorial_game_target_size)
            buttonStartRetry.setOnClickListener { viewModel.startGame(gameArea.area(), targetSize) }
            footer.buttonNextLevel.setOnClickListener { viewModel.stopTutorial() }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentGame.collect(::onGameUpdated) }
                launch { viewModel.gameTimerValue.collect(::onTimerUpdated) }
                launch { viewModel.gameScore.collect(::onScoreUpdated) }
                launch { viewModel.playRetryBtnState.collect(::onPlayRetryButtonStateUpdated) }
                launch { viewModel.nextGameBtnVisibility.collect(::onNextLevelButtonVisibilityUpdated) }
                launch { viewModel.gameTargets.collect(::onTargetsUpdated) }
                launch { viewModel.shouldDisplayStepOverlay.collect(::showHideStepOverlay) }
                launch { viewModel.showOverlayMenu.collect(::showHideOverlayMenu) }
            }
        }
    }

    override fun onDestroy() {
        viewModel.stopTutorial()
        OverlayManager.getInstance(requireContext()).apply {
            removeTopOverlay()
            restoreVisibility()
            unlockMenuPosition()
        }

        super.onDestroy()
    }

    private fun onGameUpdated(tutorialGame: TutorialGame?) {
        tutorialGame ?: return
        viewBinding.header.setHeaderInfo(tutorialGame.instructionsResId, tutorialGame.highScore)
    }

    private fun onTimerUpdated(timerValue: Int) {
        viewBinding.footer.setTimeLeft(timerValue)
    }

    private fun onScoreUpdated(score: Int) {
        viewBinding.header.setScore(score)
    }

    private fun onPlayRetryButtonStateUpdated(state: PlayRetryButtonState) {
        when (state) {
            PlayRetryButtonState.GONE -> viewBinding.buttonStartRetry.visibility = View.GONE
            PlayRetryButtonState.RETRY -> setPlayRetryButtonVisible(R.drawable.ic_game_retry)
            PlayRetryButtonState.PLAY -> setPlayRetryButtonVisible(R.drawable.ic_play_arrow)
        }
    }

    private fun onNextLevelButtonVisibilityUpdated(isVisible: Boolean) {
        viewBinding.footer.setNextLevelBtnVisibility(isVisible)
    }

    private fun onTargetsUpdated(targets: Map<TutorialGameTargetType, PointF>) {
        viewBinding.blueTarget.updateTargetState(targets[TutorialGameTargetType.BLUE])
        viewBinding.redTarget.updateTargetState(targets[TutorialGameTargetType.RED])
    }

    private fun setPlayRetryButtonVisible(@DrawableRes iconRes: Int) {
        viewBinding.blueTarget.visibility = View.GONE
        viewBinding.redTarget.visibility = View.GONE

        viewBinding.buttonStartRetry.apply {
            setIconResource(iconRes)
            visibility = View.VISIBLE
        }
    }

    private fun showHideStepOverlay(show: Boolean) {
        OverlayManager.getInstance(requireContext()).apply {
            if (show) setTopOverlay(TutorialFullscreenOverlay())
            else removeTopOverlay()
        }
    }

    private fun showHideOverlayMenu(show: Boolean) {
        OverlayManager.getInstance(requireContext()).apply {
            if (show) restoreVisibility()
            else hideAll()
        }
    }

    private fun lockMenuPosition() {
        val location = IntArray(2)
        viewBinding.spaceOverlayMenu.getLocationInWindow(location)

        OverlayManager.getInstance(requireContext())
            .lockMenuPosition(Point(viewBinding.spaceOverlayMenu.marginStart + location[0], location[1]))
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

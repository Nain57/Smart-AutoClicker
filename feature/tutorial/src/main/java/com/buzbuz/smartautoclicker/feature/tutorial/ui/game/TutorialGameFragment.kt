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
import com.buzbuz.smartautoclicker.core.ui.utils.getDynamicColorsContext
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.DialogTutorialSuccessBinding
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialGameBinding
import com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay.TutorialFullscreenOverlay

import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            TutorialGameTargetType.entries.forEach { targetType ->
                getTargetView(targetType).setOnClickListener {
                    viewModel.onTargetHit(targetType)
                }
            }

            buttonStartRetry.setOnClickListener { viewModel.startGame(gameArea.area()) }
        }

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.gameArea.forceLayout()
        lockMenuPosition()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUi) }
                launch { viewModel.shouldDisplayStepOverlay.collect(::showHideStepOverlay) }
                launch { viewModel.shouldDisplayFloatingUi.collect(::showHideFloatingUi) }
                launch { viewModel.shouldDisplayCompletionDialog.collect(::showCompletionDialog) }
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
                updateTargetsState(emptyMap())
            } else {
                buttonStartRetry.visibility = View.GONE
                updateTargetsState(uiState.targets)
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

    private fun showCompletionDialog(show: Boolean) {
        if (!show) return

        val dialogContext = requireContext().getDynamicColorsContext(R.style.AppTheme)
        val dialogViewBinding = DialogTutorialSuccessBinding.inflate(LayoutInflater.from(dialogContext))
        val dialog = MaterialAlertDialogBuilder(dialogContext)
            .setView(dialogViewBinding.root)
            .create()

        dialogViewBinding.apply {
            buttonKeepPlaying.setOnClickListener { dialog.dismiss() }
            buttonClose.setOnClickListener {
                dialog.dismiss()
                findNavController().navigateUp()
            }
        }

        dialog.show()
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

    private fun updateTargetsState(state: Map<TutorialGameTargetType, PointF>) {
        TutorialGameTargetType.entries.forEach { targetType ->
            val targetView = viewBinding.getTargetView(targetType)
            val position = state[targetType]

            if (position == null) {
                targetView.visibility = View.GONE
            } else {
                targetView.x = position.x - targetView.width / 2f
                targetView.y = position.y - targetView.height / 2f
                targetView.visibility = View.VISIBLE
            }
        }
    }

    private fun FragmentTutorialGameBinding.getTargetView(type: TutorialGameTargetType): View =
        when (type) {
            TutorialGameTargetType.IMAGE_BLUE -> blueTarget
            TutorialGameTargetType.IMAGE_RED -> redTarget
            TutorialGameTargetType.TEXT_DAY -> dayTarget
            TutorialGameTargetType.TEXT_GOODBYE -> goodbyeTarget
            TutorialGameTargetType.TEXT_HELLO -> helloTarget
            TutorialGameTargetType.TEXT_NIGHT -> nightTarget
        }
}

private fun FrameLayout.area(): Rect =
    Rect(0, 0, width, height)

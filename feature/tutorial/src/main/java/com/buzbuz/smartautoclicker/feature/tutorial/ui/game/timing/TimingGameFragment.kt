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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game.timing

import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.utils.getDynamicColorsContext
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.DialogTutorialSuccessBinding
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTimingGameBinding
import com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay.TutorialFullscreenOverlay

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimingGameFragment : Fragment() {

    private val viewModel: TimingGameViewModel by viewModels()
    private lateinit var viewBinding: FragmentTimingGameBinding

    @Inject lateinit var overlayManager: OverlayManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTimingGameBinding.inflate(inflater, container, false).apply {
            buttonRetry.setOnClickListener { viewModel.resetGame() }
            buttonTiming.setOnClickListener {
                viewModel.onTimingButtonHit()
            }
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

    private fun updateUi(uiState: TimingGameUiState?) {
        uiState ?: return

        viewBinding.apply {
            textInstructions.text = requireContext().getString(uiState.instructionsResId)
            textScore.text = requireContext().getString(R.string.message_click_count, uiState.clickCount, uiState.targetClickCount)
            textTargetDiff.text = requireContext().getString(R.string.message_target_diff, uiState.targetTotalDiffMs)
            textTotalDiff.text = requireContext().getString(R.string.message_total_diff, uiState.cumulativeTimeDiffMs.toSignedString())
            textLast.text = requireContext().getString(R.string.message_last_diff, uiState.lastTimeDiffMs.toSignedString())

            buttonTiming.isEnabled = uiState.isWon == null
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

    private fun Long.toSignedString(): String =
        if (this >= 0) "+$this" else "$this"

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

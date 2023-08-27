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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay

import android.view.LayoutInflater
import android.view.View

import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.contains
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.overlays.FullscreenOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.IncludeTutorialInstructionsBinding
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.OverlayTutorialBinding

import kotlinx.coroutines.launch

class TutorialFullscreenOverlay : FullscreenOverlay(theme = R.style.AppTheme) {

    /** The view model for this overlay. */
    private val viewModel: TutorialOverlayViewModel by viewModels()

    /** ViewBinding containing the views for this overlay. */
    private lateinit var viewBinding: OverlayTutorialBinding
    /** ViewBinding containing the instructions. */
    private lateinit var instructionsViewBinding: IncludeTutorialInstructionsBinding

    override fun onCreateView(layoutInflater: LayoutInflater): View {
        viewBinding = OverlayTutorialBinding.inflate(layoutInflater).apply {
            buttonSkipAll.setOnClickListener { onSkipAllClicked() }
            buttonNext.setOnClickListener { viewModel.toNextTutorialStep() }
            tutorialBackground.onMonitoredViewClickedListener = viewModel::toNextTutorialStep
        }

        instructionsViewBinding = IncludeTutorialInstructionsBinding.inflate(layoutInflater)

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun onSkipAllClicked() {
        OverlayManager.getInstance(context).restoreVisibility()
        viewModel.toLastTutorialStep()
    }

    private fun updateUiState(uiState: UiTutorialOverlayState?) {
        uiState ?: return

        when(uiState.exitButton) {
            TutorialExitButton.Next -> updateUiStateWithNextButton(uiState)
            is TutorialExitButton.MonitoredView -> updateUiStateWithMonitoredViewHole(uiState)
            else -> updateUiStateWithoutButton(uiState)
        }
    }

    private fun updateUiStateWithNextButton(uiState: UiTutorialOverlayState) {
        viewBinding.apply {
            buttonNext.visibility = View.VISIBLE
            tutorialBackground.expectedViewPosition = null
        }

        // Next button is in the bottom of the screen, always display instructions on top
        setInstructions(uiState)
    }

    private fun updateUiStateWithMonitoredViewHole(uiState: UiTutorialOverlayState) {
        val exitButton = uiState.exitButton as TutorialExitButton.MonitoredView

        viewBinding.apply {
            buttonNext.visibility = View.GONE
            tutorialBackground.expectedViewPosition = exitButton.position
        }

        // Depending on the monitored view position, use the correct instructions position
        setInstructions(uiState)
    }

    private fun updateUiStateWithoutButton(uiState: UiTutorialOverlayState) {
        viewBinding.apply {
            buttonNext.visibility = View.GONE
            tutorialBackground.expectedViewPosition = null
        }

        // No buttons is shown, always display instructions on top
        setInstructions(uiState)
    }

    private fun setInstructions(uiState: UiTutorialOverlayState) {
        instructionsViewBinding.apply {
            textInstructions.setText(uiState.instructionsResId)

            if (uiState.image != null) {
                layoutImageInstructions.visibility = View.VISIBLE
                imageInstructions.setImageResource(uiState.image.imageResId)
                textImageInstructionsDescription.setText(uiState.image.imageDescResId)
            } else {
                layoutImageInstructions.visibility = View.GONE
            }
        }

        addInstructionViewIfNeeded()

        if (uiState.isDisplayedInTopHalf) setInstructionsToTopPosition()
        else setInstructionsToBottomPosition()
    }

    private fun addInstructionViewIfNeeded() {
        if (viewBinding.root.contains(instructionsViewBinding.root)) return

        val margin = context.resources.getDimensionPixelSize(R.dimen.tutorial_instructions_horizontal_margin)
        viewBinding.root.addView(
            instructionsViewBinding.root,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(margin, 0, margin, 0) },
        )
    }

    private fun setInstructionsToTopPosition() {
        ConstraintSet().apply {
            clone(viewBinding.root)
            connectTopToBottom(instructionsViewBinding.cardInstructions.id, viewBinding.buttonSkipAll.id)
            connectBottomToTop(instructionsViewBinding.cardInstructions.id, viewBinding.guidelineVerticalCenter.id)
            connectStartEndToParent(instructionsViewBinding.cardInstructions.id)
            applyTo(viewBinding.root)
        }
    }

    private fun setInstructionsToBottomPosition() {
        ConstraintSet().apply {
            clone(viewBinding.root)
            connectTopToBottom(instructionsViewBinding.cardInstructions.id, viewBinding.guidelineVerticalCenter.id)
            connectBottomToParentBottom(instructionsViewBinding.cardInstructions.id)
            connectStartEndToParent(instructionsViewBinding.cardInstructions.id)
            applyTo(viewBinding.root)
        }
    }
    
    private fun ConstraintSet.connectTopToBottom(@IdRes startId: Int, @IdRes endInd: Int): Unit =
        connect(startId, ConstraintSet.TOP, endInd, ConstraintSet.BOTTOM)

    private fun ConstraintSet.connectBottomToTop(@IdRes startId: Int, @IdRes endInd: Int): Unit =
        connect(startId, ConstraintSet.BOTTOM, endInd, ConstraintSet.TOP)

    private fun ConstraintSet.connectBottomToParentBottom(@IdRes viewId: Int): Unit =
        connect(viewId, ConstraintSet.BOTTOM, 0, ConstraintSet.BOTTOM)

    private fun ConstraintSet.connectStartEndToParent(@IdRes viewId: Int) {
        connect(viewId, ConstraintSet.START, 0, ConstraintSet.START)
        connect(viewId, ConstraintSet.END, 0, ConstraintSet.END)
    }
}
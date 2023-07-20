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

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.overlays.FullscreenOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.OverlayTutorialBinding

import kotlinx.coroutines.launch

class TutorialFullscreenOverlay : FullscreenOverlay(theme = R.style.AppTheme) {

    /** The view model for this dialog. */
    private val viewModel: TutorialOverlayViewModel by viewModels()
    /** ViewBinding containing the views for this overlay. */
    private lateinit var viewBinding: OverlayTutorialBinding

    override fun onCreateView(layoutInflater: LayoutInflater): View {
        viewBinding = OverlayTutorialBinding.inflate(layoutInflater).apply {
            buttonSkipAll.setOnClickListener { viewModel.skipAllTutorialSteps() }
            buttonNext.setOnClickListener { viewModel.toNextTutorialStep() }
            tutorialBackground.onMonitoredViewClickedListener = viewModel::toNextTutorialStep
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
                launch { viewModel.monitoredViewPosition.collect(::updateExpectedViewPosition) }
            }
        }
    }

    private fun updateUiState(uiState: UiTutorialOverlayState?) {
        uiState ?: return

        viewBinding.apply {
            textInstructions.setText(uiState.instructionsResId)

            when(uiState.exitButton) {
                TutorialExitButton.Next -> {
                    tutorialBackground.expectedViewPosition = null
                    buttonNext.visibility = View.VISIBLE
                }

                is TutorialExitButton.MonitoredView -> {
                    buttonNext.visibility = View.GONE
                }
            }
        }
    }

    private fun updateExpectedViewPosition(position: Rect?) {
        viewBinding.tutorialBackground.expectedViewPosition = position
    }
}
/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.OverlayTryImageConditionMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class TryScreenConditionOverlayMenu(
    private val scenario: Scenario,
    private val imageCondition: ScreenCondition,
    private val onNewThresholdSelected: (Int) -> Unit,
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: TryScreenConditionViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { tryScreenConditionViewModel() },
    )

    private lateinit var viewBinding: OverlayTryImageConditionMenuBinding

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewModel.setScreenConditionElement(scenario, imageCondition)

        viewBinding = OverlayTryImageConditionMenuBinding.inflate(LayoutInflater.from(context)).apply {
            sliderThreshold.apply {
                valueFrom = MIN_THRESHOLD
                valueTo = MAX_THRESHOLD
                value = imageCondition.threshold.toFloat()

                addOnChangeListener(Slider.OnChangeListener { _, sliderValue, fromUser ->
                    if (fromUser) viewModel.setThreshold(sliderValue.toInt())
                })
            }
        }

        return viewBinding.root
    }

    override fun onCreateOverlayView(): DebugOverlayView = DebugOverlayView(context)

    override fun onStart() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.displayResults.collect(::updateDetectionResults) }
                launch { viewModel.thresholdText.collect(viewBinding.valueThreshold::setText) }
            }
        }

        viewModel.startTry(context)
    }

    override fun onStop() {
        viewModel.stopTry()
        onNewThresholdSelected(viewModel.getSelectedThreshold())
    }

    override fun getWindowMaximumSize(backgroundView: ViewGroup): Size {
        val bgSize = super.getWindowMaximumSize(backgroundView)
        return Size(
            bgSize.width + context.resources.getDimensionPixelSize(R.dimen.overlay_debug_text_width),
            bgSize.height,
        )
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_back -> {
                viewModel.stopTry()
                back()
            }
        }
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            viewModel.stopTry()
            back()
        }

        return true
    }

    private fun updateDetectionResults(results: ImageConditionResultsDisplay?) {
        (screenOverlayView as? DebugOverlayView)?.setResults(results?.let { listOf(it.detectionResults) } ?: emptyList())
        viewBinding.valueResult.text = results?.resultText
    }
}
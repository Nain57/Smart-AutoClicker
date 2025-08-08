
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.isStopScenarioKey
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.OverlayTryEventMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.di.DebuggingViewModelsEntryPoint

import kotlinx.coroutines.launch

class TryEventOverlayMenu(
    private val scenario: Scenario,
    private val triedElement: ImageEvent,
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: TryElementViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { tryElementViewModel() },
    )

    private lateinit var viewBinding: OverlayTryEventMenuBinding

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewModel.setTriedElement(scenario, triedElement)

        viewBinding = OverlayTryEventMenuBinding.inflate(LayoutInflater.from(context))

        return viewBinding.root
    }

    override fun onCreateOverlayView(): DebugOverlayView = DebugOverlayView(context)

    override fun onStart() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.displayResults.collect(::updateDetectionResults) }
            }
        }

        viewModel.startTry(context)
    }

    override fun onStop() {
        viewModel.stopTry()
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

    private fun updateDetectionResults(results: ImageEventResultsDisplay?) {
        (screenOverlayView as? DebugOverlayView)?.setResults(results?.detectionResults ?: emptyList())
        viewBinding.textResult.text = results?.resultText
    }
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture

import android.content.res.Configuration
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.pixelselector.PixelSelectorView
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayColorCaptureMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.core.ui.utils.updateColorIndicatorDrawableColor
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeCardZoomedViewBinding

import kotlinx.coroutines.launch
import kotlin.getValue
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType


class ColorCaptureMenu (
    private val defaultPosition: PointF? = null,
    private val onColorSelected: (position: PointF, colorInt: Int) -> Unit,
) : OverlayMenu(theme = R.style.AppTheme, recreateOverlayViewOnRotation = true) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COLOR_CAPTURE_MENU.name

    /** The view model for this menu. */
    private val viewModel: ColorCaptureViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { colorCaptureViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayColorCaptureMenuBinding
    /** The view binding for the zoomed view. */
    private lateinit var overlayView: OverlayColorCaptureZoomViewBinding
    /** The view displaying the screenshot and the selector for the capture. */
    private lateinit var selectorView: PixelSelectorView

    /** Orientation of the device. */
    private var orientation: Int = Configuration.ORIENTATION_PORTRAIT


    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayColorCaptureMenuBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }

        return viewBinding.root
    }

    override fun onCreateOverlayView(): View {
        selectorView = PixelSelectorView(
            context = context,
            displayConfigManager = displayConfigManager,
            onSelectedPositionChanged = viewModel::updateSelectedPosition,
        )

        orientation = displayConfigManager.displayConfig.orientation
        overlayView = OverlayColorCaptureZoomViewBinding.inflate(LayoutInflater.from(context), orientation).apply {
            root.addView(selectorView)

            zoomCardPrimary.viewZoom.onPixelSelected = { x, y ->
                viewModel.updateSelectedPosition(PointF(x, y))
            }
            zoomCardSecondary.viewZoom.onPixelSelected = { x, y ->
                viewModel.updateSelectedPosition(PointF(x, y))
            }
        }

        return overlayView.root
    }

    override fun onMenuItemClicked(viewId: Int) {
        val captureStep = viewModel.uiState.value.captureStep

        when (viewId) {
            R.id.btn_confirm -> when (captureStep) {
                ColorCaptureMenuStep.SCREENSHOT_SELECTION -> viewModel.captureScreen(defaultPosition)
                ColorCaptureMenuStep.PIXEL_SELECTION -> {
                    viewModel.getPixelSelection()?.let { (position, color) ->
                        back()
                        onColorSelected(position, color)
                    }
                }
                ColorCaptureMenuStep.CAPTURING -> return
            }

            R.id.btn_cancel -> when (captureStep) {
                ColorCaptureMenuStep.SCREENSHOT_SELECTION -> back()
                ColorCaptureMenuStep.PIXEL_SELECTION -> viewModel.cancelCapture()
                ColorCaptureMenuStep.CAPTURING -> return
            }
        }
    }

    private fun updateUiState(uiState: ColorCaptureUiState) {
       updateMenu(uiState)

        if (uiState.pixelSelectionUiState == null) {
            setOverlayViewVisibility(false)
            return
        }

        setOverlayViewVisibility(true)
        updateOverlay(uiState.pixelSelectionUiState)
    }

    private fun updateMenu(uiState: ColorCaptureUiState) {
        setMenuVisibility(if (uiState.menuVisibility) View.VISIBLE else View.GONE)

        viewBinding.btnConfirm.setImageResource(uiState.topButtonIcon)
        setMenuItemViewEnabled(viewBinding.btnConfirm, uiState.topButtonEnabled)
        setMenuItemViewEnabled(viewBinding.btnHideOverlay, uiState.showHideButtonEnabled)
    }

    private fun updateOverlay(uiState: PixelSelectionUiState) {
        selectorView.updateCapture(uiState.screenshot)
        uiState.selectedPosition?.let { selectorView.updatePixelPosition(it.x, it.y) }

        val visibleZoomLayout = getVisibleZoomView(uiState.selectedPosition)
        if (visibleZoomLayout == null) {
            overlayView.zoomCardPrimary.root.visibility = View.GONE
            overlayView.zoomCardSecondary.root.visibility = View.GONE
            return
        }

        overlayView.zoomCardPrimary.root.visibility =
            if (visibleZoomLayout == overlayView.zoomCardPrimary) View.VISIBLE else View.GONE
        overlayView.zoomCardSecondary.root.visibility =
            if (visibleZoomLayout == overlayView.zoomCardSecondary) View.VISIBLE else View.GONE

        visibleZoomLayout.viewZoom.setImageBitmap(uiState.screenshot)
        visibleZoomLayout.viewZoom.setZoomPosition(uiState.selectedPosition!!)
        visibleZoomLayout.textColorValue.text = uiState.selectedColorDisplayText
        visibleZoomLayout.iconColorValue.updateColorIndicatorDrawableColor(uiState.selectedColor ?: 0)
    }

    private fun getVisibleZoomView(selectedPosition: PointF?): IncludeCardZoomedViewBinding? {
        if (selectedPosition == null) return null

        return when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                if (selectedPosition.x < overlayView.root.width / 2) overlayView.zoomCardSecondary
                else overlayView.zoomCardPrimary

            Configuration.ORIENTATION_PORTRAIT ->
                if (selectedPosition.y < overlayView.root.height / 2) overlayView.zoomCardSecondary
                else overlayView.zoomCardPrimary

            else -> null
        }
    }
}

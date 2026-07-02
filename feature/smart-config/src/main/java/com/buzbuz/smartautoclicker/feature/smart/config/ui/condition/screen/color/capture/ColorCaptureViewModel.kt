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

import android.graphics.Bitmap
import android.graphics.PointF
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.get
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.extensions.toRgbaHexString
import com.buzbuz.smartautoclicker.feature.smart.debugging.R

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

class ColorCaptureViewModel @Inject constructor(
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val displayConfigManager: DisplayConfigManager,
    private val displayRecorder: DisplayRecorder,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    private var screenshotJob: Job? = null

    private val _uiState: MutableStateFlow<ColorCaptureUiState> = MutableStateFlow(screenshotSelectionState())
    val uiState: StateFlow<ColorCaptureUiState> = _uiState


    fun getPixelSelection(): Pair<PointF, Int>? {
        val pixelSelectionState = uiState.value.pixelSelectionUiState ?: return null
        val selectedPosition = pixelSelectionState.selectedPosition ?: return null
        val selectedColor = pixelSelectionState.selectedColor ?: return null

        return selectedPosition to selectedColor
    }

    fun captureScreen(initialFocusPosition: PointF?) {
        _uiState.update { capturingState() }

        screenshotJob = viewModelScope.launch(ioDispatcher) {
            delay(200L) // Wait a bit to ensure menu is effectively invisible and a new screen frame is available

            val screenshot = displayRecorder.takeScreenshot()
            _uiState.update {
                if (screenshot == null) capturingState() else {
                    withContext(mainDispatcher) {
                        monitoredViewsManager.notifyClick(MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE)
                    }
                    pixelSelectionState(screenshot, initialFocusPosition)
                }
            }
        }
    }

    fun cancelCapture() {
        _uiState.update { screenshotSelectionState() }
    }

    fun updateSelectedPosition(position: PointF?) {
        _uiState.update { currentState ->
            val currentPixelSelectionState = currentState.pixelSelectionUiState
            if (currentState.captureStep != ColorCaptureMenuStep.PIXEL_SELECTION || currentPixelSelectionState == null)
                currentState
            else {
                val selectorColor = currentState.pixelSelectionUiState.screenshot.getPixelColor(position)
                currentState.copy(
                    topButtonEnabled = position != null,
                    pixelSelectionUiState = currentState.pixelSelectionUiState.copy(
                        selectedPosition = position,
                        selectedColor = selectorColor,
                        selectedColorDisplayText = selectorColor.toRgbaHexString(),
                    )
                )
            }
        }
    }

    private fun screenshotSelectionState(): ColorCaptureUiState =
        ColorCaptureUiState(
            captureStep = ColorCaptureMenuStep.SCREENSHOT_SELECTION,
            menuVisibility = true,
            topButtonIcon = R.drawable.ic_capture,
            topButtonEnabled = true,
            showHideButtonEnabled = false,
        )

    private fun capturingState(): ColorCaptureUiState =
        ColorCaptureUiState(
            captureStep = ColorCaptureMenuStep.CAPTURING,
            menuVisibility = false,
            topButtonIcon = R.drawable.ic_color_validate,
            topButtonEnabled = false,
            showHideButtonEnabled = false,
        )

    private fun pixelSelectionState(screenshot: Bitmap, initialFocusPosition: PointF?): ColorCaptureUiState {
        val displaySize = displayConfigManager.displayConfig.sizePx
        val position = initialFocusPosition ?: PointF(displaySize.x / 2f, displaySize.y / 2f)
        val selectorColor = screenshot.getPixelColor(position)

        return ColorCaptureUiState(
            captureStep = ColorCaptureMenuStep.PIXEL_SELECTION,
            menuVisibility = true,
            topButtonIcon = R.drawable.ic_color_validate,
            topButtonEnabled = true,
            showHideButtonEnabled = true,
            pixelSelectionUiState = PixelSelectionUiState(
                screenshot = screenshot,
                selectedPosition = position,
                selectedColor = selectorColor,
                selectedColorDisplayText = selectorColor.toRgbaHexString(),
            ),
        )
    }

    /** @return the color of the pixel in the screenshot as a [ColorInt]. */
    @ColorInt
    private fun Bitmap.getPixelColor(position: PointF?): Int =
        if (position == null) 0
        else get(
            position.x.toInt().coerceIn(0, width - 1),
            position.y.toInt().coerceIn(0, height - 1),
        )
}


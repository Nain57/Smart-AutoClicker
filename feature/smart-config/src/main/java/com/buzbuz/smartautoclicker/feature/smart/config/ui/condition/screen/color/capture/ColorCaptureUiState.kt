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
import androidx.annotation.ColorInt


data class ColorCaptureUiState(
    val captureStep: ColorCaptureMenuStep,
    val menuVisibility: Boolean,
    val topButtonIcon: Int,
    val topButtonEnabled: Boolean,
    val showHideButtonEnabled: Boolean,
    val pixelSelectionUiState: PixelSelectionUiState? = null,
)

data class PixelSelectionUiState(
    val screenshot: Bitmap,
    val selectedPosition: PointF? = null,
    @param:ColorInt val selectedColor: Int? = null,
    val selectedColorDisplayText: String? = null,
)

enum class ColorCaptureMenuStep {
    /** User is selecting the screenshot to take. */
    SCREENSHOT_SELECTION,
    /** User have clicked on the capture button and we are waiting for the screenshot result. */
    CAPTURING,
    /** User is selecting the pixel on the capture. */
    PIXEL_SELECTION,
}

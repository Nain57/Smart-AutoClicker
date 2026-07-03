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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay

import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType


data class TutorialFullscreenUiState(
    @field:StringRes val instructionsResId: Int,
    val image: TutorialStepImageUiState? = null,
    val exitButton: TutorialExitButtonUiState? = null,
    val isDisplayedInTopHalf: Boolean = true,
)

data class TutorialStepImageUiState(
    @field:DrawableRes val imageResId: Int,
    @field:StringRes val imageDescResId: Int,
)

sealed class TutorialExitButtonUiState {
    object Next : TutorialExitButtonUiState()
    data class MonitoredView(val type: MonitoredViewType, val position: Rect) : TutorialExitButtonUiState()
}
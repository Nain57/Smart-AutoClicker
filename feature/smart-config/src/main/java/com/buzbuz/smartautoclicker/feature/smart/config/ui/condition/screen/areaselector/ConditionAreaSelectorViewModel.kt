/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.areaselector

import android.graphics.Rect

import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

import javax.inject.Inject

class ConditionAreaSelectorViewModel @Inject constructor(
    editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {


    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }

    /** The position at which the selector should be initialized. */
    val initialArea: Flow<SelectorUiState> = configuredCondition
        .mapNotNull { condition -> condition.toSelectorUiState() }

    private fun ScreenCondition.toSelectorUiState(): SelectorUiState? =
        when (this) {
            is ScreenCondition.Color ->
                SelectorUiState(
                    initialArea = detectionArea,
                    minimalArea = Rect(0, 0, 1, 1),
                )

            is ScreenCondition.Image ->
                if (detectionType != IN_AREA) null
                else SelectorUiState(
                    initialArea = detectionArea ?: area,
                    minimalArea = area,
                )

            is ScreenCondition.Number -> TODO()

            is ScreenCondition.Text -> {
                val screenSize = displayConfigManager.displayConfig.sizePx
                SelectorUiState(
                    initialArea =
                        if (!detectionArea.isEmpty) detectionArea
                        else Rect(
                            (screenSize.x / 2) - 64,
                            (screenSize.y / 2) - 64,
                            (screenSize.x / 2) + 64,
                            (screenSize.y / 2) + 64,
                        ),
                    minimalArea = Rect(0, 0, MIN_TEXT_DETECTION_WIDTH, MIN_TEXT_DETECTION_HEIGHT),
                )
            }
        }
}

data class SelectorUiState(
    val initialArea: Rect,
    val minimalArea: Rect,
)

private const val MIN_TEXT_DETECTION_WIDTH = 128
private const val MIN_TEXT_DETECTION_HEIGHT = 64
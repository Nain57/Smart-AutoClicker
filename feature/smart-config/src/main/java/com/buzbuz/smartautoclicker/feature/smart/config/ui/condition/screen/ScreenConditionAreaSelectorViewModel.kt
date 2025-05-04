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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen

import android.content.Context
import android.graphics.Rect
import android.util.Size

import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

import javax.inject.Inject

class ImageConditionAreaSelectorViewModel @Inject constructor(
    @ApplicationContext context: Context,
    editionRepository: EditionRepository,
    displayConfigManager: DisplayConfigManager,
) : ViewModel()  {


    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }

    private val minTextAreaSize: Size = Size(
        context.resources.getDimensionPixelSize(R.dimen.overlay_condition_selector_width),
        context.resources.getDimensionPixelSize(R.dimen.overlay_condition_selector_height),
    )

    /** The position at which the selector should be initialized. */
    val initialArea: Flow<SelectorUiState> = configuredCondition
        .mapNotNull { condition ->
            when {
                condition.detectionType != IN_AREA -> null

                condition is ImageCondition -> SelectorUiState(
                    initialArea = condition.detectionArea ?: condition.captureArea,
                    minimalArea = condition.captureArea,
                )

                condition is TextCondition -> SelectorUiState(
                    initialArea = condition.detectionArea ?: displayConfigManager.getScreenCenteredArea(minTextAreaSize),
                    minimalArea = displayConfigManager.getScreenCenteredArea(Size(100, 50))
                )

                else -> null
            }
        }
}

data class SelectorUiState(
    val initialArea: Rect,
    val minimalArea: Rect,
)

private fun DisplayConfigManager.getScreenCenteredArea(areaSize: Size): Rect =
    displayConfig.let { config ->
        val screenCenterX = config.sizePx.x / 2
        val screenCenterY = config.sizePx.y / 2
        val xOffset = areaSize.width / 2
        val yOffset = areaSize.height / 2

        Rect(
            screenCenterX - xOffset,
            screenCenterY - yOffset,
            screenCenterX + xOffset,
            screenCenterY + yOffset,
        )
    }

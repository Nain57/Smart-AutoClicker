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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.app.Application
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class ConditionAreaSelectorViewModel(application: Application) : AndroidViewModel(application)  {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedConditionState
        .mapNotNull { it.value }

    /** The position at which the selector should be initialized. */
    val initialArea: Flow<SelectorUiState> = configuredCondition
        .mapNotNull { condition ->
            if (condition.detectionType != IN_AREA) null
            else SelectorUiState(
                initialArea = condition.detectionArea ?: condition.area,
                minimalArea = condition.area,
            )
        }
}

data class SelectorUiState(
    val initialArea: Rect,
    val minimalArea: Rect,
)
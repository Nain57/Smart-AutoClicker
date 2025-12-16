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
package com.buzbuz.smartautoclicker.feature.smart.debugging.uistate.mapping

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.utils.formatDebugConfidenceRate
import com.buzbuz.smartautoclicker.feature.smart.debugging.uistate.ImageConditionResultUiState


internal fun DebugLiveImageEventOccurrence.toConditionResultsUiState(): List<ImageConditionResultUiState> =
    imageConditionsResults.map { result -> result.toUiState() }

internal fun DebugLiveImageConditionResult.toUiState(): ImageConditionResultUiState =
    ImageConditionResultUiState(
        positive = isFulfilled,
        coordinates = detectionArea ?: Rect(),
        confidenceRate = confidenceRate,
        resultText = confidenceRate.formatDebugConfidenceRate(),
    )
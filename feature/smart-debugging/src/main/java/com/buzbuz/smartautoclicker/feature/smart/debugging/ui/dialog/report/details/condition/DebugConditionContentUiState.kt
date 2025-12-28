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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition

import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition


sealed interface DebugConditionContentUiState {

    data object Loading : DebugConditionContentUiState
    data class Available(
        val items: List<EventOccurrenceItem>,
    ) : DebugConditionContentUiState
}

sealed interface EventOccurrenceItem {

    data class Header(
        val conditionOperatorValueText: String,
    ) : EventOccurrenceItem

    data class Image(
        val id: Long,
        val conditionName: String,
        val durationText: String,
        val condition: ImageCondition,
        val confidenceText: String,
        val confidenceValid: Boolean,
        val shouldDetectedValue: Boolean,
        val isFulfilledValue: Boolean,
    ) : EventOccurrenceItem

    data class Trigger(
        val id: Long,
        val conditionName: String,
        @field:DrawableRes val iconRes: Int,
        val description: String,
    ) : EventOccurrenceItem
}

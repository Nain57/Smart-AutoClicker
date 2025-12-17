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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.mapping

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.utils.formatConditionResultsDisplayText
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.ImageEventResultUiState

internal fun DebugLiveImageEventOccurrence.toUiState(): ImageEventResultUiState =
    ImageEventResultUiState(
        eventText = event.name,
        conditionsText = formatConditionResultsDisplayText(),
        detectionResults = toConditionResultsUiState(),
    )

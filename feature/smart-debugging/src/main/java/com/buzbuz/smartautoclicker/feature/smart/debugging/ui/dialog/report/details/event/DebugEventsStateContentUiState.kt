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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.event

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class DebugEventsStateContentUiState {

    data object Loading : DebugEventsStateContentUiState()

    data object Empty : DebugEventsStateContentUiState()

    data class Available(
        val eventsState: List<DebugEventStateItem>,
    ) : DebugEventsStateContentUiState()
}

sealed class DebugEventStateItem {

    data class Header(
        @field:StringRes val title: Int,
        @field:DrawableRes val icon: Int,
    ) : DebugEventStateItem()

    data class EventState(
        val eventId: String,
        val eventName: String,
        val isEnabled: Boolean,
        val haveChanged: Boolean,
    ) : DebugEventStateItem()
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.debugging

import androidx.annotation.DrawableRes

/**
 * State of the live debugging view on the main menu.
 * This shows the state of the last/currently executed event in the scenario.
 *
 * @param eventIcon resource of the icon indicating the type of Event.
 * @param eventName the name of the event.
 * @param eventFulfilledCount the number of times the event has been fulfilled.
 * @param eventDuration the duration of the event. This accounts for detection time (if any) and actions completion.
 * @param actions the list of icons for the actions being executed.
 */
data class LiveDebuggingUiState(
    @field:DrawableRes val eventIcon: Int,
    val eventName: String,
    val eventFulfilledCount: String,
    val eventDuration: String,
    val actions: List<LiveDebuggingActionsItem>,
)

/** Icons for the actions of the event being executed.*/
data class LiveDebuggingActionsItem(
    @field:DrawableRes val icon: Int,
)
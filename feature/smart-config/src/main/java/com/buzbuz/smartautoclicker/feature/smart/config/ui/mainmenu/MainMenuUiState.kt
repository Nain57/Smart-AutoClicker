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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu

/**
 * State of the [MainMenu].
 *
 * @param isDetecting true if the detection is running, false if not.
 * @param isPlayButtonEnabled true if the play/pause button can be clicked.
 * @param isProjectionErrorVisible true if the media projection is not started and the error badge should be shown.
 * @param isDebugPanelVisible true if the live debugging panel should be displayed next to the menu items.
 */
data class MainMenuUiState(
    val isDetecting: Boolean,
    val isPlayButtonEnabled: Boolean,
    val isProjectionErrorVisible: Boolean,
    val isDebugPanelVisible: Boolean,
)

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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription

data class SmartActionsBriefUiState(
    val actionBriefs: List<ItemBrief>,
    /** Null when no action is focused, or while [isRecordedGestureDisplayed]. */
    val focusedActionDescription: ItemBriefDescription?,
    /** The brief view displays the gesture being recorded, or the recorded one until its action is focused. */
    val isRecordedGestureDisplayed: Boolean,
    val mode: SmartActionsBriefMode,
    val isBriefPanelAutoHideEnabled: Boolean,
)

enum class SmartActionsBriefMode {
    IDLE,
    GESTURE_CAPTURE,
    ACTION_TESTING,
}

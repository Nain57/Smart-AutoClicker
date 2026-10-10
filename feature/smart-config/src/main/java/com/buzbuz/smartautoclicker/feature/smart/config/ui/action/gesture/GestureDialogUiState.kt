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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition

data class GestureDialogUiState(
    val canBeSaved: Boolean = false,
    val hasChanged: Boolean = false,
    val uiItems: List<GestureDialogUiStateItem> = listOf(GestureDialogUiStateItem.Header()),
)

sealed interface GestureDialogUiStateItem {

    data class Header(
        val actionName: String = "",
        val fingerIndex: Int = 0,
        val fingerCount: Int = 1,
        val fingerErrorState: Map<Int, Boolean> = emptyMap(),
        val currentFingerStartTimeMs: Long = 0,
        val fingerGestureDurationMs: Long = 0,
        val maxFingerGestureDurationsMs: Long = Gesture.GESTURE_MAXIMUM_DURATION_MS,
    ) : GestureDialogUiStateItem

    data class Add(
        val fingerIndex: Int,
    ) : GestureDialogUiStateItem

    data class Release(
        val fingerIndex: Int,
    ) : GestureDialogUiStateItem

    data class Press(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        val position: UiGesturePosition,
        val offsetPosition: UiGesturePosition
    ) : Collapsable

    data class Move(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        override val moveableUp: Boolean,
        override val moveableDown: Boolean,
        val position: UiGesturePosition,
        val offsetPosition: UiGesturePosition,
        val durationMs: Long,
    ) : Collapsable, Moveable

    data class Wait(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        override val moveableUp: Boolean,
        override val moveableDown: Boolean,
        val durationMs: Long,
    ) : Collapsable, Moveable

    sealed interface Collapsable : GestureDialogUiStateItem {
        val id: Identifier
        val isCollapsed: Boolean
    }

    interface Moveable {
        val moveableUp: Boolean
        val moveableDown: Boolean
    }
}

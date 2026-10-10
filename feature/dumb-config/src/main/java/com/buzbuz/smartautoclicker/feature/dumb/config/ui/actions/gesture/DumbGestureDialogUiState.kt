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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture

import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction

data class DumbGestureDialogUiState(
    val canBeSaved: Boolean = false,
    val uiItems: List<DumbGestureDialogUiStateItem> = emptyList(),
)

sealed interface DumbGestureDialogUiStateItem {

    data class Header(
        val actionName: String,
        val nameError: Boolean,
        val repeatCount: Int,
        val repeatCountError: Boolean,
        val isRepeatInfinite: Boolean,
        val repeatDelayMs: Long,
        val repeatDelayError: Boolean,
        val fingerIndex: Int,
        val fingerCount: Int,
        val fingerErrorState: Map<Int, Boolean>,
        val currentFingerStartTimeMs: Long,
        val fingerGestureDurationMs: Long,
        val maxFingerGestureDurationsMs: Long = DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS,
    ) : DumbGestureDialogUiStateItem

    data class Add(
        val fingerIndex: Int,
    ) : DumbGestureDialogUiStateItem

    data class Release(
        val fingerIndex: Int,
    ) : DumbGestureDialogUiStateItem

    data class Press(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        val position: Point,
    ) : Collapsable

    data class Move(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        override val moveableUp: Boolean,
        override val moveableDown: Boolean,
        val position: Point,
        val durationMs: Long,
    ) : Collapsable, Moveable

    data class Wait(
        override val id: Identifier,
        override val isCollapsed: Boolean,
        override val moveableUp: Boolean,
        override val moveableDown: Boolean,
        val durationMs: Long,
    ) : Collapsable, Moveable

    sealed interface Collapsable : DumbGestureDialogUiStateItem {
        val id: Identifier
        val isCollapsed: Boolean
    }

    interface Moveable {
        val moveableUp: Boolean
        val moveableDown: Boolean
    }
}

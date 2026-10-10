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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.offset

data class GestureOffsetUiState(
    val xState: GestureOffsetValueUiState,
    val yState: GestureOffsetValueUiState,
    val updateFrom: GestureOffsetUpdateSource,
)

sealed interface GestureOffsetValueUiState {
    data class Literal(val displayText: String) : GestureOffsetValueUiState
    data class Counter(val displayText: String) : GestureOffsetValueUiState
}

enum class GestureOffsetUpdateSource { TEXT_INPUT, VIEW, OTHER }
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.click

import android.graphics.Point

data class DumbClickDialogUiState(
    val canBeSaved: Boolean = false,
    val name: String = "",
    val nameError: Boolean = false,
    val pressDurationMs: Long = 0,
    val pressDurationError: Boolean = false,
    val repeatCount: Int = 0,
    val repeatCountError: Boolean = false,
    val isRepeatInfinite: Boolean = false,
    val repeatDelayMs: Long = 0,
    val repeatDelayError: Boolean = false,
    val position: Point = Point(),
)

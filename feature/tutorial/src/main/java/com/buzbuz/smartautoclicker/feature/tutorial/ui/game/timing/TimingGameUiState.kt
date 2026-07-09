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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game.timing

import androidx.annotation.StringRes

data class TimingGameUiState(
    @field:StringRes val instructionsResId: Int,
    val isStarted: Boolean,
    val isWon: Boolean?,
    val clickCount: Int,
    val targetClickCount: Int,
    val targetTotalDiffMs: Long,
    val cumulativeTimeDiffMs: Long,
    val lastTimeDiffMs: Long,
)

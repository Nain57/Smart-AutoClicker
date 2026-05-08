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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color

import android.graphics.PointF
import androidx.annotation.ColorInt

data class ColorConditionUiState(
    val conditionName: String,
    val conditionNameError: Boolean,
    val conditionPosition: PointF,
    @param:ColorInt val conditionColor: Int,
    val conditionColorText: String,
    val redValue: Int,
    val greenValue: Int,
    val blueValue: Int,
    val shouldBeDetectedChecked: Boolean,
    val detectionThreshold: Int,
)
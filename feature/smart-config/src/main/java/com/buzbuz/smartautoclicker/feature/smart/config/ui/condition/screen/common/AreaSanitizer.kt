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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common

import android.graphics.Rect
import kotlin.math.max


internal fun Rect.sanitizeAreaForCondition(conditionArea: Rect = Rect()): Rect {
    val left = max(left, 0)
    val top = max(top, 0)
    val width = max(right - left, conditionArea.width())
    val height = max(bottom - top, conditionArea.height())

    return Rect(
        left,
        top,
        left + width,
        top + height,
    )
}
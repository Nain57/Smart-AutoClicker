/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model

import androidx.annotation.IntDef

/** Defines the detection type to apply to a condition. */
@IntDef(EXACT, WHOLE_SCREEN, IN_AREA)
@Retention(AnnotationRetention.SOURCE)
annotation class DetectionType
/** The condition must be detected at the exact same position. */
const val EXACT = 1
/** The condition can be detected anywhere on the screen. */
const val WHOLE_SCREEN = 2
/** The condition can be detected only in a given area. */
const val IN_AREA = 3

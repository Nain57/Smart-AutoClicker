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

/** Defines the operators to be applied between the conditions of an event. */
@IntDef(AND, OR)
@Retention(AnnotationRetention.SOURCE)
annotation class ConditionOperator
/** All conditions must be fulfilled. */
const val AND = 1
/** Only one of the conditions must be fulfilled. */
const val OR = 2

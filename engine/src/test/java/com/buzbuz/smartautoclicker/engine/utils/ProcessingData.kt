/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.engine.utils

import android.graphics.Rect
import com.buzbuz.smartautoclicker.domain.*

/** Test data and helpers for the detection tests. */
internal object ProcessingData {

    /** Instantiates a new event with only the useful values for the tests. */
    fun newEvent(
        id: Long = 1L,
        @ConditionOperator operator: Int = AND,
        actions: List<Action> = emptyList(),
        conditions: List<Condition> = emptyList(),
        stopAfter: Int? = null,
    ) = Event(
        id,
        1L,
        "TOTO",
        operator,
        0,
        actions.toMutableList(),
        conditions.toMutableList(),
        stopAfter
    )

    /** Instantiates a new condition with only the useful values for the tests. */
    fun newCondition(
        path: String,
        area: Rect,
        threshold: Int,
        @DetectionType detectionType: Int,
        shouldBeDetected: Boolean = true,
    ) = Condition(
        1L,
        1L,
        "TOTO",
        path,
        area,
        threshold,
        detectionType,
        shouldBeDetected,
        null
    )
}
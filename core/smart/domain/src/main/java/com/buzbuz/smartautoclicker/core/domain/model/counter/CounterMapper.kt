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
package com.buzbuz.smartautoclicker.core.domain.model.counter

import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation


internal fun ComparisonOperation.toEntity(): CounterComparisonOperation =
    when (this) {
        ComparisonOperation.EQUALS -> CounterComparisonOperation.EQUALS
        ComparisonOperation.LOWER -> CounterComparisonOperation.LOWER
        ComparisonOperation.LOWER_OR_EQUALS -> CounterComparisonOperation.LOWER_OR_EQUALS
        ComparisonOperation.GREATER -> CounterComparisonOperation.GREATER
        ComparisonOperation.GREATER_OR_EQUALS -> CounterComparisonOperation.GREATER_OR_EQUALS
    }

internal fun CounterComparisonOperation.toDomain(): ComparisonOperation =
    when (this) {
        CounterComparisonOperation.EQUALS -> ComparisonOperation.EQUALS
        CounterComparisonOperation.LOWER -> ComparisonOperation.LOWER
        CounterComparisonOperation.LOWER_OR_EQUALS -> ComparisonOperation.LOWER_OR_EQUALS
        CounterComparisonOperation.GREATER -> ComparisonOperation.GREATER
        CounterComparisonOperation.GREATER_OR_EQUALS -> ComparisonOperation.GREATER_OR_EQUALS
    }
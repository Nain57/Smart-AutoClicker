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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.feature.smart.config.R

@StringRes
internal fun ComparisonOperation.toName(): Int =
    when (this) {
        ComparisonOperation.GREATER -> R.string.comparison_operator_greater
        ComparisonOperation.GREATER_OR_EQUALS -> R.string.comparison_operator_greater_or_equals
        ComparisonOperation.EQUALS -> R.string.comparison_operator_equals
        ComparisonOperation.LOWER_OR_EQUALS -> R.string.comparison_operator_lower_or_equals
        ComparisonOperation.LOWER -> R.string.comparison_operator_lower
    }

@StringRes
internal fun ComparisonOperation.toFullName(): Int =
    when (this) {
        ComparisonOperation.GREATER -> R.string.comparison_operator_greater_full
        ComparisonOperation.GREATER_OR_EQUALS -> R.string.comparison_operator_greater_or_equals_full
        ComparisonOperation.EQUALS -> R.string.comparison_operator_equals_full
        ComparisonOperation.LOWER_OR_EQUALS -> R.string.comparison_operator_lower_or_equals_full
        ComparisonOperation.LOWER -> R.string.comparison_operator_lower_full
    }
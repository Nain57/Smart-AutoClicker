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

/** Type of counter comparison. */
enum class ComparisonOperation {
    /** The counter value is strictly equals to the value. */
    EQUALS,
    /** The counter value is strictly lower than the value. */
    LOWER,
    /** The counter value is lower or equals to the value */
    LOWER_OR_EQUALS,
    /** The counter value is strictly greater than the value. */
    GREATER,
    /** The counter value is greater or equals to the value. */
    GREATER_OR_EQUALS;
}

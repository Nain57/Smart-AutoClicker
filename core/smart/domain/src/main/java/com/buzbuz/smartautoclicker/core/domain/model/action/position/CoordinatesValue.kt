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
package com.buzbuz.smartautoclicker.core.domain.model.action.position


/** A single coordinate of a [Position.Coordinates]. */
sealed interface CoordinatesValue {

    /**
     * A fixed coordinate.
     *
     * @param value the coordinate in px.
     */
    data class Literal(val value: Int) : CoordinatesValue

    /**
     * A coordinate read from a scenario counter at execution time.
     *
     * @param counterName the name of the counter holding the coordinate in px.
     */
    data class Counter(val counterName: String) : CoordinatesValue
}
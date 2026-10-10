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

import com.buzbuz.smartautoclicker.core.database.entity.CoordinatesType


internal fun Position.xCoordinatesType(): CoordinatesType = when (this) {
    is Position.Condition -> CoordinatesType.CONDITION
    is Position.Coordinates -> x.toCoordinatesType()
}

internal fun Position.yCoordinatesType(): CoordinatesType = when (this) {
    is Position.Condition -> CoordinatesType.CONDITION
    is Position.Coordinates -> y.toCoordinatesType()
}

internal fun Position.xLiteral(): Int? =
    (this as? Position.Coordinates)?.x?.literal()

internal fun Position.xCounter(): String? =
    (this as? Position.Coordinates)?.x?.counter()

internal fun Position.yLiteral(): Int? =
    (this as? Position.Coordinates)?.y?.literal()

internal fun Position.yCounter(): String? =
    (this as? Position.Coordinates)?.y?.counter()

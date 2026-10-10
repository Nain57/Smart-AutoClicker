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
package com.buzbuz.smartautoclicker.core.domain.model.action.gesture

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * A position on the screen for a gesture or a click.
 *
 * The actual screen point is only known at execution time: it is either taken from a detection result or computed
 * from coordinates that can themselves depend on the scenario counters.
 */
sealed interface GesturePosition {

    /**
     * The position where a screen condition of the event has been detected.
     *
     * @param conditionId the identifier of the condition to use. Only relevant when the event conditions operator is
     * AND; with OR, the first detected condition is used instead. Null if the user haven't selected it yet.
     */
    data class Condition(
        val conditionId: Identifier?,
    ) : GesturePosition

    /**
     * A position defined by its coordinates.
     *
     * @param x the horizontal coordinate in px.
     * @param y the vertical coordinate in px.
     */
    data class Coordinates(
        val x: GestureCoordinatesValue,
        val y: GestureCoordinatesValue,
    ) : GesturePosition
}

/** A single coordinate of a [GesturePosition.Coordinates]. */
sealed interface GestureCoordinatesValue {

    /**
     * A fixed coordinate.
     *
     * @param value the coordinate in px.
     */
    data class Literal(val value: Int) : GestureCoordinatesValue

    /**
     * A coordinate read from a scenario counter at execution time.
     *
     * @param counterName the name of the counter holding the coordinate in px.
     */
    data class Counter(val counterName: String) : GestureCoordinatesValue
}
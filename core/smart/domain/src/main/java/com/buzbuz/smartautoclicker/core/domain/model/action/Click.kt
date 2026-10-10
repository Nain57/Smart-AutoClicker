/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.action.position.literal

/**
 * Click action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param pressDuration the duration between the click down and up in milliseconds.
 * @param position the position to click on, null if the user haven't selected it yet.
 * @param offset the offset in px from the [position] to apply when clicking.
 */
data class Click(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val pressDuration: Long? = null,
    val position: Position? = null,
    val offset: Position? = null,
) : Action() {

    override fun isComplete(): Boolean =
        super.isComplete() && pressDuration != null && position != null

    override fun hashCodeNoIds(): Int =
        name.hashCode() + pressDuration.hashCode() + position.hashCode() + offset.hashCode()


    override fun deepCopy(): Click = copy(name = "" + name)

    /** The click position, or null if it is not a pair of literal coordinates. */
    fun getLiteralPosition(): Point? {
        val coordinates = position as? Position.Coordinates ?: return null
        val literalX = coordinates.x.literal() ?: return null
        val literalY = coordinates.y.literal() ?: return null
        return Point(literalX, literalY)
    }

    /** The identifier of the condition to click on, or null if the click is not on a condition. */
    fun getClickOnConditionId(): Identifier? =
        (position as? Position.Condition)?.conditionId

    fun isClickOnConditionValid(): Boolean =
        position !is Position.Condition || position.conditionId != null
}

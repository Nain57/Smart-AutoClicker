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
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType

/**
 * Click action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param pressDuration the duration between the click down and up in milliseconds.
 * @param positionType the type of click position.
 * @param position the position of the click. Not null only if [positionType] is [PositionType.USER_SELECTED].
 * @param clickOnConditionId the condition to click on. Not null only if [positionType] is [PositionType.ON_DETECTED_CONDITION].
 * @param clickOffset the offset in px from the detected condition center to apply when clicking. Not null only if
 * [positionType] is [PositionType.ON_DETECTED_CONDITION].
 */
data class Click(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val pressDuration: Long? = null,
    val positionType: PositionType,
    val position: Point? = null,
    val clickOnConditionId: Identifier? = null,
    val clickOffset: Point? = null,
) : Action() {

    /**
     * Types of click positions a [Click].
     * Keep the same names as the db ones.
     */
    enum class PositionType {
        /** The user must manually select a position to be clicked. */
        USER_SELECTED,
        /**
         * Click on the detected condition.
         * When the condition operator is AND, click on the condition specified by the user.
         * When the condition operator is OR, click on the condition detected condition.
         */
        ON_DETECTED_CONDITION;

        fun toEntity(): ClickPositionType = ClickPositionType.valueOf(name)
    }

    override fun isComplete(): Boolean =
        super.isComplete() && pressDuration != null && isPositionValid()

    override fun hashCodeNoIds(): Int =
        name.hashCode() + pressDuration.hashCode() + positionType.hashCode() + position.hashCode() +
                clickOnConditionId.hashCode() + clickOffset.hashCode()


    override fun deepCopy(): Click = copy(name = "" + name)

    private fun isPositionValid(): Boolean =
        (positionType == PositionType.USER_SELECTED && position != null) || positionType == PositionType.ON_DETECTED_CONDITION

    fun isClickOnConditionValid(): Boolean =
        (positionType == PositionType.ON_DETECTED_CONDITION && clickOnConditionId != null) || positionType == PositionType.USER_SELECTED
}
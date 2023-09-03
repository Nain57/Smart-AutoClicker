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
package com.buzbuz.smartautoclicker.feature.scenario.config.utils

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

internal fun Action.isValidInEvent(event: Event?): Boolean {
    event ?: return false

    return if (event.conditionOperator == AND && this is Action.Click && positionType == Action.Click.PositionType.ON_DETECTED_CONDITION) {
        clickOnConditionId != null && isComplete()
    } else isComplete()
}

internal fun Action.isClickOnCondition(): Boolean =
    this is Action.Click && this.positionType == Action.Click.PositionType.ON_DETECTED_CONDITION

/** Check if this list does not already contains the provided action */
internal fun List<Action>.doesNotContainAction(action: Action): Boolean =
    find { item -> item.id == action.id } == null
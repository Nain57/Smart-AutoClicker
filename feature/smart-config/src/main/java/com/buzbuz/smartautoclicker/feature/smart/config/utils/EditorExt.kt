
package com.buzbuz.smartautoclicker.feature.smart.config.utils

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent

internal fun Action.isValidInEvent(event: ImageEvent?): Boolean {
    event ?: return false

    return if (event.conditionOperator == AND && this is Click && positionType == Click.PositionType.ON_DETECTED_CONDITION) {
        clickOnConditionId != null && isComplete()
    } else isComplete()
}

internal fun Action.isClickOnCondition(): Boolean =
    this is Click && this.positionType == Click.PositionType.ON_DETECTED_CONDITION

/** Check if this list does not already contains the provided action */
internal fun List<Action>.doesNotContainAction(action: Action): Boolean =
    find { item -> item.id == action.id } == null
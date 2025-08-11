
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getClickIconRes(): Int =
    R.drawable.ic_click

internal fun Click.getDescription(context: Context, parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    if (positionType == Click.PositionType.ON_DETECTED_CONDITION) {
        val condition = parent.conditions.find { it.id == clickOnConditionId }
        if (condition != null) {
            return context.getString(
                R.string.item_click_details_on_condition,
                formatDuration(pressDuration ?: 1),
                condition.name,
            )
        }
    }

    return context.getString(
        R.string.item_click_details_at_position,
        formatDuration(pressDuration!!),
    )
}
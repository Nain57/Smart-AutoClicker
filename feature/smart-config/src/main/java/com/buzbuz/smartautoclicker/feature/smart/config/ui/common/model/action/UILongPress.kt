package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.LongPress
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getLongPressIconRes(): Int = R.drawable.ic_click

internal fun LongPress.getDescription(context: Context, parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    if (positionType == Click.PositionType.ON_DETECTED_CONDITION) {
        val condition = parent.conditions.find { it.id == onConditionId }
        if (condition != null) {
            return context.getString(
                R.string.item_long_press_details_on_condition,
                formatDuration(holdDuration ?: 600),
                condition.name,
            )
        }
    }
    return context.getString(
        R.string.item_long_press_details_at_position,
        formatDuration(holdDuration ?: 600),
    )
}
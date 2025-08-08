
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R


@DrawableRes
internal fun getToggleEventIconRes(): Int = R.drawable.ic_toggle_event

internal fun ToggleEvent.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_toggle_event_details_error)

    toggleAll -> when (toggleAllType) {
        ToggleEvent.ToggleType.ENABLE -> context.getString(R.string.item_toggle_event_details_enable_all)
        ToggleEvent.ToggleType.TOGGLE -> context.getString(R.string.item_toggle_event_details_invert_all)
        ToggleEvent.ToggleType.DISABLE -> context.getString(R.string.item_toggle_event_details_disable_all)
        null -> throw IllegalArgumentException("Invalid toggle event type")
    }

    else -> context.getString(
        R.string.item_toggle_event_details_manual,
        eventToggles.size,
    )
}
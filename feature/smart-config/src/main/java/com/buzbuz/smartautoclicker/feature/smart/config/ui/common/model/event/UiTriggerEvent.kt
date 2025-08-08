
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R


data class UiTriggerEvent(
    override val event: TriggerEvent,
    val name: String,
    val conditionsCountText: String,
    val actionsCountText: String,
    @StringRes val enabledOnStartTextRes: Int,
    @DrawableRes val enabledOnStartIconRes: Int,
    val haveError: Boolean,
) : UiEvent()

fun TriggerEvent.toUiTriggerEvent(inError: Boolean): UiTriggerEvent {
    @StringRes val enabledOnStartTextRes: Int
    @DrawableRes val enabledOnStartIconRes: Int
    if (enabledOnStart) {
        enabledOnStartTextRes = R.string.item_event_desc_enabled_children
        enabledOnStartIconRes = R.drawable.ic_confirm
    } else {
        enabledOnStartTextRes = R.string.item_event_desc_disabled_children
        enabledOnStartIconRes = R.drawable.ic_cancel
    }

    return UiTriggerEvent(
        event = this,
        name = name,
        conditionsCountText = conditions.size.toString(),
        actionsCountText = actions.size.toString(),
        enabledOnStartTextRes = enabledOnStartTextRes,
        enabledOnStartIconRes = enabledOnStartIconRes,
        haveError = inError,
    )
}

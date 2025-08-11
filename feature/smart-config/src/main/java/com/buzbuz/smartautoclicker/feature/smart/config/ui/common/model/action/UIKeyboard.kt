package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideMethod
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ShowKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes internal fun getHideKeyboardIconRes(): Int = R.drawable.ic_keyboard_hide
@DrawableRes internal fun getShowKeyboardIconRes(): Int = R.drawable.ic_keyboard_show

private fun Context.methodLabel(method: HideMethod): String = when (method) {
    HideMethod.BACK -> getString(R.string.method_back)
    HideMethod.TAP_OUTSIDE -> getString(R.string.method_tap_outside)
    HideMethod.BACK_THEN_TAP_OUTSIDE -> getString(R.string.method_back_then_tap_outside)
}

internal fun HideKeyboard.getDescription(context: Context, @Suppress("UNUSED_PARAMETER") parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)
    return context.getString(R.string.item_hide_keyboard_details, context.methodLabel(method))
}

internal fun ShowKeyboard.getDescription(context: Context, parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    if (positionType == Click.PositionType.ON_DETECTED_CONDITION) {
        val condition = parent.conditions.find { it.id == onConditionId }
        if (condition != null) {
            return context.getString(R.string.item_show_keyboard_details_on_condition, condition.name)
        }
    }
    return context.getString(R.string.item_show_keyboard_details_at_position)
}
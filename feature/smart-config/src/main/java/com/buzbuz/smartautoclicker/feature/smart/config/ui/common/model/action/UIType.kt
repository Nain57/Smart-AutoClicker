package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.TypeText
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getTypeTextIconRes(): Int = R.drawable.ic_type_text

private fun preview(text: String?, max: Int = 24): String {
    val t = text.orEmpty()
    if (t.length <= max) return t
    return t.substring(0, max - 1) + "…"
}

internal fun TypeText.getDescription(context: Context, @Suppress("UNUSED_PARAMETER") parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)
    return context.getString(R.string.item_type_text_details, preview(text))
}
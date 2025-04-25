package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.PressBack
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getPressBackIconRes(): Int = R.drawable.ic_press_back

internal fun PressBack.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_error_action_invalid_generic)
    else -> context.getString(R.string.item_press_back_desc_simple)
}
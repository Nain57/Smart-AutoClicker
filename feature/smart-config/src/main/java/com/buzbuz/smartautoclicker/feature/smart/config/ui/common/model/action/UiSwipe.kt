
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R


@DrawableRes
internal fun getSwipeIconRes(): Int =
    R.drawable.ic_swipe

internal fun Swipe.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_error_action_invalid_generic)

    else -> context.getString(
        R.string.item_swipe_details,
        formatDuration(swipeDuration ?: 1),
    )
}
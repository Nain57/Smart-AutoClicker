
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R


@DrawableRes
internal fun getPauseIconRes(): Int =
    R.drawable.ic_wait

internal fun Pause.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_error_action_invalid_generic)

    else -> context.getString(
        R.string.item_pause_details,
        formatDuration(pauseDuration ?: 1)
    )
}
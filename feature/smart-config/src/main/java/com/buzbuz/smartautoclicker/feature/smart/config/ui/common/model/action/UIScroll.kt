package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Axis
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Scroll
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R
import kotlin.math.roundToInt

@DrawableRes
internal fun getScrollIconRes(): Int = R.drawable.ic_scroll

private fun Context.axisLabel(axis: Axis?): String = when (axis) {
    Axis.UP -> getString(R.string.axis_up)
    Axis.DOWN -> getString(R.string.axis_down)
    Axis.LEFT -> getString(R.string.axis_left)
    Axis.RIGHT -> getString(R.string.axis_right)
    else -> getString(R.string.axis_down)
}

internal fun Scroll.getDescription(context: Context, @Suppress("UNUSED_PARAMETER") parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)
    val pct = ((distancePercent ?: 0.6f) * 100f).roundToInt()
    return context.getString(
        R.string.item_scroll_details,
        context.axisLabel(axis),
        pct,
        formatDuration(duration ?: 350),
    )
}
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Screenshot
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getScreenshotIconRes(): Int = R.drawable.ic_screenshot

internal fun Screenshot.getDescription(context: Context, @Suppress("UNUSED_PARAMETER") parent: Event, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)
    val roi = roi
    return if (roi != null) {
        context.getString(
            R.string.item_screenshot_details_roi,
            roi.width, roi.height, roi.left, roi.top,
        )
    } else {
        context.getString(R.string.item_screenshot_details_full)
    }
}
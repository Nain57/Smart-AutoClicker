
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.feature.smart.config.R


@DrawableRes
internal fun getNotificationIconRes(): Int = R.drawable.ic_action_notification

internal fun Notification.getDescription(context: Context, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    return when (messageType) {
        Notification.MessageType.TEXT ->
            context.getString(R.string.item_notification_details_text, messageText)
        Notification.MessageType.COUNTER_VALUE ->
            context.getString(R.string.item_notification_details_counter, messageCounterName)
    }
}
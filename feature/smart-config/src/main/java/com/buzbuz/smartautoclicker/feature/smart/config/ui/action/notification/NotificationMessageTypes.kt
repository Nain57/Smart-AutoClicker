
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class NotificationMessageTypeItem(
    @StringRes title: Int,
) : DropdownItem(title) {

    data object Text : NotificationMessageTypeItem(
        title = R.string.field_dropdown_item_notification_message_type_text,
    )

    data object Counter : NotificationMessageTypeItem(
        title = R.string.field_dropdown_item_notification_message_type_counter,
    )
}

internal val notificationMessageTypeItems: List<NotificationMessageTypeItem>
    get() = listOf(
        NotificationMessageTypeItem.Text,
        NotificationMessageTypeItem.Counter,
    )

internal fun Notification.MessageType.toTypeItem(): NotificationMessageTypeItem =
    when (this) {
        Notification.MessageType.TEXT -> NotificationMessageTypeItem.Text
        Notification.MessageType.COUNTER_VALUE -> NotificationMessageTypeItem.Counter
    }

internal fun NotificationMessageTypeItem.toNotificationMessageType(): Notification.MessageType =
    when (this) {
        NotificationMessageTypeItem.Text -> Notification.MessageType.TEXT
        NotificationMessageTypeItem.Counter -> Notification.MessageType.COUNTER_VALUE
    }
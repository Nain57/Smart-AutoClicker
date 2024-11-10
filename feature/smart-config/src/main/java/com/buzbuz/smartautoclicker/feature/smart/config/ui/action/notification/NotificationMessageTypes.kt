/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
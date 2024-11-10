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
import androidx.core.app.NotificationManagerCompat
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class NotificationImportanceItem(
    @StringRes title: Int,
    @StringRes helperText: Int,
) : DropdownItem(title, helperText) {

    data object Low : NotificationImportanceItem(
        title = R.string.field_dropdown_item_notification_importance_type_low,
        helperText = R.string.field_dropdown_item_notification_importance_type_low_desc,
    )

    data object Default : NotificationImportanceItem(
        title = R.string.field_dropdown_item_notification_importance_type_default,
        helperText = R.string.field_dropdown_item_notification_importance_type_default_desc,
    )

    data object High : NotificationImportanceItem(
        title = R.string.field_dropdown_item_notification_importance_type_high,
        helperText = R.string.field_dropdown_item_notification_importance_type_high_desc,
    )
}

internal val notificationImportanceItems: List<NotificationImportanceItem>
    get() = listOf(
        NotificationImportanceItem.High,
        NotificationImportanceItem.Default,
        NotificationImportanceItem.Low,
    )

internal fun Int.toImportanceItem(): NotificationImportanceItem =
    when (this) {
        NotificationManagerCompat.IMPORTANCE_LOW -> NotificationImportanceItem.Low
        NotificationManagerCompat.IMPORTANCE_DEFAULT -> NotificationImportanceItem.Default
        NotificationManagerCompat.IMPORTANCE_HIGH -> NotificationImportanceItem.High
        else -> throw UnsupportedOperationException("Value is not a valid notification importance")
    }

internal fun NotificationImportanceItem.toNotificationImportance(): Int =
    when (this) {
        NotificationImportanceItem.Low  -> NotificationManagerCompat.IMPORTANCE_LOW
        NotificationImportanceItem.Default  -> NotificationManagerCompat.IMPORTANCE_DEFAULT
        NotificationImportanceItem.High -> NotificationManagerCompat.IMPORTANCE_HIGH
    }
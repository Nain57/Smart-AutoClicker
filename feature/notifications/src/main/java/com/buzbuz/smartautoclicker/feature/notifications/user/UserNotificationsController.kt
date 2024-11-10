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
package com.buzbuz.smartautoclicker.feature.notifications.user

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build

import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.domain.model.NotificationRequest
import com.buzbuz.smartautoclicker.feature.notifications.R
import com.buzbuz.smartautoclicker.feature.notifications.common.getUserScenarioNotificationChannelId


class UserNotificationsController(context: Context) {

    private val notifier: UserNotificationNotifier =
        UserNotificationNotifier(context)

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, notificationRequest: NotificationRequest) {
        if (!PermissionPostNotification().checkIfGranted(context)) return

        val channelId = getUserScenarioNotificationChannelId(notificationRequest.importance)
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationRequest.title)
            .setContentText(notificationRequest.message)
            .setSmallIcon(notificationIconResId())
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .setGroup(notificationRequest.groupName)

        notifier.notify(notificationRequest.id, builder.build())
    }

    fun clearAll() {
        notifier.clearAllPostedNotifications()
    }

    @DrawableRes
    private fun notificationIconResId(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.drawable.ic_notification_vector
        else R.drawable.ic_action_notification
}
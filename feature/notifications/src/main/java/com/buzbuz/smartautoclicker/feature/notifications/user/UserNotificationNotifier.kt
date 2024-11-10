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

import android.Manifest
import android.app.Notification
import android.content.Context
import android.os.Build

import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat

import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelDefault
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelGroup
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelHigh
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelLow

internal class UserNotificationNotifier(context: Context) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.apply {
                createUserScenarioNotificationChannelGroup(context)
                createUserScenarioNotificationChannelLow(context)
                createUserScenarioNotificationChannelDefault(context)
                createUserScenarioNotificationChannelHigh(context)
            }
        }
    }

    private val postedIds: MutableSet<Int> = mutableSetOf()

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notify(id: Long, notification: Notification) {
        val notificationId = id.hashCode()

        postedIds.add(notificationId)
        notificationManager.notify(notificationId, notification)
    }

    fun clearAllPostedNotifications() {
        postedIds.forEach(notificationManager::cancel)
        postedIds.clear()
    }
}
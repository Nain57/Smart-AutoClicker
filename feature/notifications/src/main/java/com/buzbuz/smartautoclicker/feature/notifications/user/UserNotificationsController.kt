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

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.domain.model.NotificationRequest
import com.buzbuz.smartautoclicker.feature.notifications.common.NotificationIds
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelDefault
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelGroup
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelHigh
import com.buzbuz.smartautoclicker.feature.notifications.common.createUserScenarioNotificationChannelLow
import com.buzbuz.smartautoclicker.feature.notifications.common.getUserScenarioNotificationChannelId
import com.buzbuz.smartautoclicker.feature.notifications.common.notificationIconResId
import com.buzbuz.smartautoclicker.feature.notifications.user.group.UserNotificationGroups

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserNotificationsController @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val notificationIds: NotificationIds,
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    private val notificationGroups: UserNotificationGroups =
        UserNotificationGroups(notificationIds)
    private val notificationScheduler: UserNotificationScheduler =
        UserNotificationScheduler(ioDispatcher, notificationManager)

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

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, notificationRequest: NotificationRequest) {
        if (!PermissionPostNotification().checkIfGranted(context)) return

        val channelId = getUserScenarioNotificationChannelId(notificationRequest.importance)
        val notificationGroup = notificationGroups.getGroup(context, notificationRequest)
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationRequest.title)
            .setContentText(notificationRequest.message)
            .setSmallIcon(notificationIconResId())
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .setGroup(notificationGroup.groupName)

        notificationScheduler.schedule(
            id = notificationIds.getUserNotificationId(notificationRequest.actionId),
            notification = builder.build(),
            group = notificationGroup,
        )
    }

    fun clearAll() {
        notificationScheduler.clear()
        // We don't want to remove another notification like the foreground service one
        notificationIds.resetDynamicIdsCache().forEach { notificationId ->
            notificationManager.cancel(notificationId)
        }
    }
}
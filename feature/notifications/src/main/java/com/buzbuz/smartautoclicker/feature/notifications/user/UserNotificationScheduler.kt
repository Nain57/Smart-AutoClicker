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
import androidx.annotation.RequiresPermission

import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.NotificationWithIdAndTag
import com.buzbuz.smartautoclicker.feature.notifications.user.group.UserNotificationGroup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


internal class UserNotificationScheduler(
    coroutineDispatcher: CoroutineDispatcher,
    private val notificationManager: NotificationManagerCompat,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineDispatcher.limitedParallelism(1))

    private val scheduledNotifications: MutableMap<Int, Notification> = mutableMapOf()

    private var schedulingJob: Job? = null
    private var lastNotifyTimestampMs: Long = 0L


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun schedule(id: Int, notification: Notification, group: UserNotificationGroup) {
        coroutineScope.launch {
            scheduledNotifications[id] = notification
            scheduledNotifications[group.summaryId] = group.summaryBuilder.build()

            if (System.currentTimeMillis() > lastNotifyTimestampMs + NOTIFICATIONS_UPDATE_SPACING_MS) {
                postNotifications()
                return@launch
            }

            if (schedulingJob != null) return@launch
            schedulingJob = launch {
                delay(NOTIFICATIONS_UPDATE_SPACING_MS)
                postNotifications()
                schedulingJob = null
            }
        }
    }

    fun clear() {
        schedulingJob?.cancel()
        schedulingJob = null

        scheduledNotifications.clear()
        lastNotifyTimestampMs = 0
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postNotifications() {
        notificationManager.notify(
            scheduledNotifications.map { (notificationId, notification) ->
                NotificationWithIdAndTag(notificationId, notification)
            }
        )

        scheduledNotifications.clear()
        lastNotifyTimestampMs = System.currentTimeMillis()
    }
}

/** Android limits to 1s, use something a little bit bigger to avoid getting silenced by the system. */
private const val NOTIFICATIONS_UPDATE_SPACING_MS = 1250L
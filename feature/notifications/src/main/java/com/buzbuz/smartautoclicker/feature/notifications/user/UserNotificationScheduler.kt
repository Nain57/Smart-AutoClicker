
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

package com.buzbuz.smartautoclicker.feature.notifications.user.group

import android.content.Context
import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.core.domain.model.NotificationRequest
import com.buzbuz.smartautoclicker.feature.notifications.common.NotificationIds
import com.buzbuz.smartautoclicker.feature.notifications.common.getUserScenarioNotificationChannelId
import com.buzbuz.smartautoclicker.feature.notifications.common.notificationIconResId


internal class UserNotificationGroups(private val notificationIds: NotificationIds) {

    private val groups: MutableMap<Long, UserNotificationGroup> =
        mutableMapOf()

    fun getGroup(context: Context, notificationRequest: NotificationRequest): UserNotificationGroup =
        groups[notificationRequest.eventId] ?: UserNotificationGroup(
            groupName = notificationRequest.groupName,
            summaryId = notificationIds.getSummaryNotificationId(notificationRequest.eventId),
            summaryBuilder = notificationRequest.createSummaryNotificationBuilder(context)
        )

    private fun NotificationRequest.createSummaryNotificationBuilder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, getUserScenarioNotificationChannelId(importance))
            .setContentTitle(groupName)
            .setSmallIcon(notificationIconResId())
            .setGroup(groupName)
            .setGroupSummary(true)
}


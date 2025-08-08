
package com.buzbuz.smartautoclicker.feature.notifications.common

import javax.inject.Inject
import javax.inject.Singleton

/** Manages the ids of the notifications for the whole app. */
@Singleton
class NotificationIds @Inject constructor() {

    companion object {
        /** The identifier for the foreground notification of Klickr accessibility service. */
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1

        /** The start of the range for the notifications for an action. New ids goes incrementally one by one */
        private const val USER_NOTIFICATION_IDS_START = 100
        /** The start of the range for the notifications group summary. New ids are decremental one by one */
        private const val GROUP_SUMMARY_NOTIFICATION_IDS_START = -100
    }

    private val postedUserNotificationsIds: MutableMap<Long, Int> = mutableMapOf()
    private var userNotificationIdIndex: Int = USER_NOTIFICATION_IDS_START

    private val postedSummaryNotificationsIds: MutableMap<Long, Int> = mutableMapOf()
    private var summaryNotificationIdIndex: Int = GROUP_SUMMARY_NOTIFICATION_IDS_START


    internal fun getUserNotificationId(actionId: Long): Int =
        postedUserNotificationsIds.getOrPut(actionId) { userNotificationIdIndex++ }

    internal fun getSummaryNotificationId(eventId: Long): Int =
        postedSummaryNotificationsIds.getOrPut(eventId) { summaryNotificationIdIndex-- }

    internal fun resetDynamicIdsCache(): List<Int> {
        val clearedIds = postedUserNotificationsIds.values + postedSummaryNotificationsIds.values

        postedUserNotificationsIds.values.clear()
        userNotificationIdIndex = USER_NOTIFICATION_IDS_START

        postedSummaryNotificationsIds.values.clear()
        summaryNotificationIdIndex = GROUP_SUMMARY_NOTIFICATION_IDS_START

        return clearedIds
    }
}


package com.buzbuz.smartautoclicker.feature.notifications.user.group

import androidx.core.app.NotificationCompat

internal data class UserNotificationGroup(
    val groupName: String,
    val summaryId: Int,
    val summaryBuilder: NotificationCompat.Builder,
)
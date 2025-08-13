/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.actions.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.buzbuz.smartautoclicker.core.base.notifications.NotificationIds
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationGroup
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationRequest
import com.buzbuz.smartautoclicker.core.ui.utils.notificationIconResId


internal class ActionNotificationGroups(private val notificationIds: NotificationIds) {

    private val groups: MutableMap<Long, ActionNotificationGroup> =
        mutableMapOf()

    fun getGroup(context: Context, notificationRequest: ActionNotificationRequest): ActionNotificationGroup? {
        val builder = notificationRequest.createSummaryNotificationBuilder(context) ?: return null

        return groups[notificationRequest.eventId] ?: ActionNotificationGroup(
            groupName = notificationRequest.groupName,
            summaryId = notificationIds.getSummaryNotificationId(notificationRequest.eventId),
            summaryBuilder = builder,
        )
    }
}

private fun ActionNotificationRequest.createSummaryNotificationBuilder(context: Context): NotificationCompat.Builder? {
    val channelId = getActionNotificationChannelId(importance) ?: return null

    return NotificationCompat.Builder(context, channelId)
        .setContentTitle(groupName)
        .setSmallIcon(notificationIconResId())
        .setGroup(groupName)
        .setGroupSummary(true)
}
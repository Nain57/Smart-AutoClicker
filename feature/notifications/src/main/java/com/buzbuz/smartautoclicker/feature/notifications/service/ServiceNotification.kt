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
package com.buzbuz.smartautoclicker.feature.notifications.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.feature.notifications.R

internal fun Context.createServiceNotificationBuilder(
    channelId: String,
    activityIntent: PendingIntent,
    scenarioName: String?,
) = NotificationCompat.Builder(this, channelId)
    .setContentTitle(getString(R.string.notification_title, scenarioName ?: ""))
    .setContentText(getString(R.string.notification_message))
    .setContentIntent(activityIntent)
    .setSmallIcon(notificationIconResId())
    .setCategory(Notification.CATEGORY_SERVICE)
    .setOngoing(true)
    .setLocalOnly(true)

@DrawableRes
private fun notificationIconResId(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.drawable.ic_notification_vector
    else R.drawable.ic_action_notification
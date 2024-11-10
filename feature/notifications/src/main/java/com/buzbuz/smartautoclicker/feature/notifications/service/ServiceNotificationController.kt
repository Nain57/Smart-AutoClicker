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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.buzbuz.smartautoclicker.core.common.permissions.model.Permission
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification

import com.buzbuz.smartautoclicker.feature.notifications.common.FOREGROUND_SERVICE_NOTIFICATION_ID
import com.buzbuz.smartautoclicker.feature.notifications.common.SERVICE_CHANNEL_ID
import com.buzbuz.smartautoclicker.feature.notifications.common.createKlickrServiceNotificationChannel
import com.buzbuz.smartautoclicker.feature.notifications.service.actions.ServiceNotificationActionsManager


class ServiceNotificationController(
    context: Context,
    listener: ServiceNotificationListener,
    private val activityPendingIntent: PendingIntent,
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)
    private val notificationActions: ServiceNotificationActionsManager =
        ServiceNotificationActionsManager(listener)

    private var notificationBuilder: NotificationCompat.Builder? = null


    fun createNotification(context: Context, scenarioName: String?): Notification {
        Log.i(TAG, "Create notification")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createKlickrServiceNotificationChannel(context))
        }

        val builder = notificationBuilder
            ?: context.createServiceNotificationBuilder(SERVICE_CHANNEL_ID, activityPendingIntent,scenarioName)

        notificationBuilder = builder
        notificationActions.updateActions(context, builder, isRunning = false, isMenuHidden = false)
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun updateNotificationState(context: Context, isRunning: Boolean, isMenuHidden: Boolean) {
        val builder = notificationBuilder ?: return
        if (!PermissionPostNotification().checkIfGranted(context)) return

        Log.i(TAG, "Updating notification, running=$isRunning; menuHidden=$isMenuHidden")

        notificationActions.updateActions(context, builder, isRunning, isMenuHidden)
        notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, builder.build())
    }

    fun destroyNotification(context: Context) {
        notificationActions.clearActions(context)
        notificationBuilder = null
        Log.i(TAG, "Notification destroyed")
    }
}

/** Tag for logs. */
private const val TAG = "ServiceNotificationManager"
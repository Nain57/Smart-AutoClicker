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

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.util.Log

import androidx.core.app.NotificationManagerCompat

import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.feature.notifications.common.NotificationIds
import com.buzbuz.smartautoclicker.feature.notifications.common.SERVICE_CHANNEL_ID
import com.buzbuz.smartautoclicker.feature.notifications.common.createKlickrServiceNotificationChannel
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationState
import com.buzbuz.smartautoclicker.feature.notifications.service.receivers.NightModeReceiver
import com.buzbuz.smartautoclicker.feature.notifications.service.receivers.NotificationActionsReceiver
import com.buzbuz.smartautoclicker.feature.notifications.service.ui.ServiceNotificationBuilder
import com.buzbuz.smartautoclicker.feature.notifications.service.ui.newServiceNotificationBuilder


class ServiceNotificationController(
    context: Context,
    private val settingsRepository: SettingsRepository,
    listener: ServiceNotificationListener,
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)
    private val notificationActionReceiver: NotificationActionsReceiver =
        NotificationActionsReceiver(listener::notifyAction)
    private val nightModeReceiver: NightModeReceiver =
        NightModeReceiver(::updateNotification)

    private var notificationState: ServiceNotificationState? = null
    private var notificationBuilder: ServiceNotificationBuilder? = null


    fun createNotification(context: Context, scenarioName: String?, isRunning: Boolean, isMenuVisible: Boolean): Notification {
        Log.i(TAG, "Create notification")

        nightModeReceiver.register(context)
        notificationActionReceiver.register(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createKlickrServiceNotificationChannel(context))
        }

        notificationState = ServiceNotificationState(
            scenarioName = scenarioName ?: "",
            isScenarioRunning = isRunning,
            isMenuVisible = isMenuVisible,
            isNightMode = nightModeReceiver.isNightModeEnabled,
        )

        val forceLegacyNotification = settingsRepository.isLegacyNotificationUiEnabled()

        val builder = notificationBuilder
            ?: context.newServiceNotificationBuilder(SERVICE_CHANNEL_ID, notificationState!!, forceLegacyNotification)
        notificationBuilder = builder
        return builder.build()
    }

    fun updateNotification(context: Context, isRunning: Boolean, isMenuVisible: Boolean) {
        val state = notificationState ?: return

        updateNotificationState(
            context,
            state.copy(isScenarioRunning = isRunning, isMenuVisible = isMenuVisible)
        )
    }

    private fun updateNotification(context: Context, isNightModeEnabled: Boolean) {
        val state = notificationState ?: return

        updateNotificationState(
            context,
            state.copy(isNightMode = isNightModeEnabled)
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateNotificationState(context: Context, state: ServiceNotificationState) {
        val builder = notificationBuilder ?: return
        if (!PermissionPostNotification().checkIfGranted(context)) return

        Log.i(TAG, "Updating notification: $state")

        notificationState = state
        builder.updateState(context, state)
        notificationManager.notify(NotificationIds.FOREGROUND_SERVICE_NOTIFICATION_ID, builder.build())
    }

    fun destroyNotification() {
        nightModeReceiver.unregister()
        notificationActionReceiver.unregister()
        notificationBuilder = null

        Log.i(TAG, "Notification destroyed")
    }
}

/** Tag for logs. */
private const val TAG = "ServiceNotificationManager"
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
package com.buzbuz.smartautoclicker.feature.notifications.service.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationListener

internal class ServiceNotificationActionsManager(private val actionsListener: ServiceNotificationListener) {

    private var notificationBroadcastReceiver: BroadcastReceiver? = null

    fun updateActions(context: Context, builder: NotificationCompat.Builder, isRunning: Boolean, isMenuHidden: Boolean) {
        if (notificationBroadcastReceiver == null) {
            notificationBroadcastReceiver = createNotificationActionReceiver()
            ContextCompat.registerReceiver(
                context,
                notificationBroadcastReceiver,
                ServiceNotificationAction.getAllActionsIntentFilter(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }

        builder.clearActions()
        when  {
            !isRunning && !isMenuHidden -> builder.addServiceNotificationAction(context, ServiceNotificationAction.PLAY_AND_HIDE)
            isRunning && isMenuHidden -> builder.addServiceNotificationAction(context, ServiceNotificationAction.PAUSE_AND_SHOW)
            isRunning && !isMenuHidden -> builder.addServiceNotificationAction(context, ServiceNotificationAction.HIDE)
            else -> builder.addServiceNotificationAction(context, ServiceNotificationAction.SHOW)
        }
        builder.addServiceNotificationAction(context, ServiceNotificationAction.STOP_SCENARIO)
    }

    fun clearActions(context: Context) {
        notificationBroadcastReceiver?.let(context::unregisterReceiver)
        notificationBroadcastReceiver = null
    }

    private fun createNotificationActionReceiver() : BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                Log.d(TAG, "Notification action received: ${intent.action}")
                actionsListener.onNewIntentAction(intent.action)
            }
        }
}

/** Tag for logs. */
private const val TAG = "ServiceNotificationActions"
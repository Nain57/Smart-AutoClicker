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
package com.buzbuz.smartautoclicker.feature.notifications.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationAction
import com.buzbuz.smartautoclicker.feature.notifications.service.model.getAllActionsBroadcastIntentFilter
import com.buzbuz.smartautoclicker.feature.notifications.service.model.toServiceNotificationAction


internal class NotificationActionsReceiver(
    private val onReceived: (ServiceNotificationAction) -> Unit,
): BroadcastReceiver() {

    private var isRegistered: Boolean = false

    fun register(context: Context) {
        if (isRegistered) return

        isRegistered = true

        ContextCompat.registerReceiver(
            context,
            this,
            getAllActionsBroadcastIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun unregister(context: Context) {
        if (!isRegistered) return

        context.unregisterReceiver(this)

        isRegistered = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.toServiceNotificationAction()?.let { action ->
            Log.i(TAG, "Notification action received: ${intent.action}")
            onReceived(action)
        }
    }
}

private const val TAG = "NotificationActionsReceiver"
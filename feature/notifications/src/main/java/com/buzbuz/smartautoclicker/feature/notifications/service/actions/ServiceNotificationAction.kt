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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.feature.notifications.R
import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationListener


internal enum class ServiceNotificationAction(val intentAction: String) {
    PLAY_AND_HIDE("com.buzbuz.smartautoclicker.PLAY_AND_HIDE"),
    PAUSE_AND_SHOW("com.buzbuz.smartautoclicker.PAUSE_AND_SHOW"),
    SHOW("com.buzbuz.smartautoclicker.SHOW"),
    HIDE("com.buzbuz.smartautoclicker.HIDE"),
    STOP_SCENARIO("com.buzbuz.smartautoclicker.STOP_SCENARIO");

    companion object {
        fun getAllActionsIntentFilter(): IntentFilter =
            IntentFilter().apply { entries.forEach { addAction(it.intentAction) } }
    }
}

internal fun NotificationCompat.Builder.addServiceNotificationAction(context: Context, action: ServiceNotificationAction) =
    when (action) {
        ServiceNotificationAction.PLAY_AND_HIDE -> addAction(
            R.drawable.ic_play_arrow,
            context.getString(R.string.notification_button_play_and_hide),
            PendingIntent.getBroadcast(context, 0, Intent(action.intentAction), PendingIntent.FLAG_IMMUTABLE),
        )

        ServiceNotificationAction.PAUSE_AND_SHOW -> addAction(
            R.drawable.ic_pause,
            context.getString(R.string.notification_button_pause_and_show),
            PendingIntent.getBroadcast(context, 0, Intent(action.intentAction), PendingIntent.FLAG_IMMUTABLE),
        )

        ServiceNotificationAction.SHOW -> addAction(
            R.drawable.ic_visible_off,
            context.getString(R.string.notification_button_toggle_menu),
            PendingIntent.getBroadcast(context, 0, Intent(action.intentAction), PendingIntent.FLAG_IMMUTABLE),
        )

        ServiceNotificationAction.HIDE -> addAction(
            R.drawable.ic_visible_on,
            context.getString(R.string.notification_button_toggle_menu),
            PendingIntent.getBroadcast(context, 0, Intent(action.intentAction), PendingIntent.FLAG_IMMUTABLE),
        )

        ServiceNotificationAction.STOP_SCENARIO -> addAction(
            R.drawable.ic_stop,
            context.getString(R.string.notification_button_stop),
            PendingIntent.getBroadcast(context, 0, Intent(action.intentAction), PendingIntent.FLAG_IMMUTABLE),
        )
    }

internal fun ServiceNotificationListener.onNewIntentAction(action: String?) {
    when (action) {
        ServiceNotificationAction.PLAY_AND_HIDE.intentAction -> onPlayAndHide()
        ServiceNotificationAction.PAUSE_AND_SHOW.intentAction -> onPauseAndShow()
        ServiceNotificationAction.HIDE.intentAction -> onHide()
        ServiceNotificationAction.SHOW.intentAction -> onShow()
        ServiceNotificationAction.STOP_SCENARIO.intentAction -> onStop()
    }
}
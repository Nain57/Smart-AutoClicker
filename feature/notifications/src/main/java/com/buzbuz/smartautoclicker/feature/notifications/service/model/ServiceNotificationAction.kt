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
package com.buzbuz.smartautoclicker.feature.notifications.service.model

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.buzbuz.smartautoclicker.core.base.data.scenarioActivityComponentName
import com.buzbuz.smartautoclicker.feature.notifications.R


internal sealed class ServiceNotificationAction {

    abstract val textRes: Int
    abstract val iconRes: Int
    abstract val intent: NotificationActionPendingIntent

    data object Play : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_play
        override val iconRes: Int = R.drawable.ic_notification_play_arrow
        override val intent: NotificationActionPendingIntent.Broadcast =
            NotificationActionPendingIntent.Broadcast("com.buzbuz.smartautoclicker.PLAY")
    }

    data object Pause : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_pause
        override val iconRes: Int = R.drawable.ic_notification_pause
        override val intent: NotificationActionPendingIntent.Broadcast =
            NotificationActionPendingIntent.Broadcast("com.buzbuz.smartautoclicker.PAUSE")
    }

    data object Show : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_show
        override val iconRes: Int = R.drawable.ic_notification_visible_off
        override val intent: NotificationActionPendingIntent.Broadcast =
            NotificationActionPendingIntent.Broadcast("com.buzbuz.smartautoclicker.SHOW")
    }

    data object Hide : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_hide
        override val iconRes: Int = R.drawable.ic_notification_visible_on
        override val intent: NotificationActionPendingIntent.Broadcast =
            NotificationActionPendingIntent.Broadcast("com.buzbuz.smartautoclicker.HIDE")
    }

    data object Config : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_config
        override val iconRes: Int = R.drawable.ic_notification_settings_filled
        override val intent: NotificationActionPendingIntent =
            NotificationActionPendingIntent.Activity(scenarioActivityComponentName)
    }

    data object Stop : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_stop
        override val iconRes: Int = R.drawable.ic_notification_cancel
        override val intent: NotificationActionPendingIntent.Broadcast =
            NotificationActionPendingIntent.Broadcast("com.buzbuz.smartautoclicker.STOP")
    }
}

internal sealed class NotificationActionPendingIntent {
    data class Broadcast(val action: String) : NotificationActionPendingIntent()
    data class Activity(val componentName: ComponentName) : NotificationActionPendingIntent()
}

internal fun getAllActionsBroadcastIntentFilter(): IntentFilter =
    IntentFilter().apply {
        addAction(ServiceNotificationAction.Play.intent.action)
        addAction(ServiceNotificationAction.Pause.intent.action)
        addAction(ServiceNotificationAction.Show.intent.action)
        addAction(ServiceNotificationAction.Hide.intent.action)
        addAction(ServiceNotificationAction.Stop.intent.action)
    }

internal fun Intent.toServiceNotificationAction(): ServiceNotificationAction? =
    when (action) {
        ServiceNotificationAction.Play.intent.action -> ServiceNotificationAction.Play
        ServiceNotificationAction.Pause.intent.action -> ServiceNotificationAction.Pause
        ServiceNotificationAction.Show.intent.action -> ServiceNotificationAction.Show
        ServiceNotificationAction.Hide.intent.action -> ServiceNotificationAction.Hide
        ServiceNotificationAction.Stop.intent.action -> ServiceNotificationAction.Stop
        else -> null
    }

internal fun ServiceNotificationAction.getPendingIntent(context: Context): PendingIntent =
    when (val intent = intent) {
        is NotificationActionPendingIntent.Activity ->
            PendingIntent.getActivity(
                context,
                0,
                Intent.makeMainActivity(scenarioActivityComponentName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        is NotificationActionPendingIntent.Broadcast ->
            PendingIntent.getBroadcast(context, 0, Intent(intent.action), PendingIntent.FLAG_IMMUTABLE)
    }

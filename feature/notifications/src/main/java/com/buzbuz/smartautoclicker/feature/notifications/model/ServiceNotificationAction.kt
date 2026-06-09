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
package com.buzbuz.smartautoclicker.feature.notifications.model

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.R


internal sealed class ServiceNotificationAction {

    abstract val textRes: Int
    abstract val iconRes: Int

    data object Play : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_play
        override val iconRes: Int = R.drawable.ic_notification_play_arrow

    }

    data object Pause : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_pause
        override val iconRes: Int = R.drawable.ic_notification_pause

    }

    data object Show : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_show
        override val iconRes: Int = R.drawable.ic_notification_visible_off

    }

    data object Hide : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_hide
        override val iconRes: Int = R.drawable.ic_notification_visible_on

    }

    data object Config : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_config
        override val iconRes: Int = R.drawable.ic_notification_settings_filled

    }

    data object Stop : ServiceNotificationAction() {
        override val textRes: Int = R.string.notification_button_stop
        override val iconRes: Int = R.drawable.ic_notification_cancel

    }
}

internal sealed class NotificationActionPendingIntent {
    data class Broadcast(val action: String) : NotificationActionPendingIntent()
    data class Activity(val componentName: ComponentName) : NotificationActionPendingIntent()
}

internal fun getAllActionsBroadcastIntentFilter(appPackageName: String): IntentFilter =
    IntentFilter().apply {
        addAction(ServiceNotificationAction.Play.getBroadcastAction(appPackageName))
        addAction(ServiceNotificationAction.Pause.getBroadcastAction(appPackageName))
        addAction(ServiceNotificationAction.Show.getBroadcastAction(appPackageName))
        addAction(ServiceNotificationAction.Hide.getBroadcastAction(appPackageName))
        addAction(ServiceNotificationAction.Stop.getBroadcastAction(appPackageName))
    }

internal fun Intent.toServiceNotificationAction(appPackageName: String): ServiceNotificationAction? =
    when (action) {
        ServiceNotificationAction.Play.getBroadcastAction(appPackageName) -> ServiceNotificationAction.Play
        ServiceNotificationAction.Pause.getBroadcastAction(appPackageName) -> ServiceNotificationAction.Pause
        ServiceNotificationAction.Show.getBroadcastAction(appPackageName) -> ServiceNotificationAction.Show
        ServiceNotificationAction.Hide.getBroadcastAction(appPackageName) -> ServiceNotificationAction.Hide
        ServiceNotificationAction.Stop.getBroadcastAction(appPackageName) -> ServiceNotificationAction.Stop
        else -> null
    }

internal fun ServiceNotificationAction.getPendingIntent(context: Context, appComponentsProvider: AppComponentsProvider): PendingIntent =
    when (val intent = getIntent(appComponentsProvider)) {
        is NotificationActionPendingIntent.Activity ->
            PendingIntent.getActivity(
                context,
                0,
                Intent.makeMainActivity(intent.componentName),
                PendingIntent.FLAG_IMMUTABLE,
            )

        is NotificationActionPendingIntent.Broadcast ->
            PendingIntent.getBroadcast(context, 0, Intent(intent.action), PendingIntent.FLAG_IMMUTABLE)
    }


private fun ServiceNotificationAction.getIntent(appComponentsProvider: AppComponentsProvider): NotificationActionPendingIntent =
    when (this) {
        ServiceNotificationAction.Play -> NotificationActionPendingIntent.Broadcast(getBroadcastAction(appComponentsProvider.currentAppId))
        ServiceNotificationAction.Pause -> NotificationActionPendingIntent.Broadcast(getBroadcastAction(appComponentsProvider.currentAppId))
        ServiceNotificationAction.Show -> NotificationActionPendingIntent.Broadcast(getBroadcastAction(appComponentsProvider.currentAppId))
        ServiceNotificationAction.Hide -> NotificationActionPendingIntent.Broadcast(getBroadcastAction(appComponentsProvider.currentAppId))
        ServiceNotificationAction.Stop -> NotificationActionPendingIntent.Broadcast(getBroadcastAction(appComponentsProvider.currentAppId))
        ServiceNotificationAction.Config -> NotificationActionPendingIntent.Activity(appComponentsProvider.scenarioActivityComponentName)
    }

private fun ServiceNotificationAction.getBroadcastAction(appPackageName: String): String =
    when (this) {
        ServiceNotificationAction.Play -> "$appPackageName.PLAY"
        ServiceNotificationAction.Pause -> "$appPackageName.PAUSE"
        ServiceNotificationAction.Show -> "$appPackageName.SHOW"
        ServiceNotificationAction.Hide -> "$appPackageName.HIDE"
        ServiceNotificationAction.Stop -> "$appPackageName.STOP"
        ServiceNotificationAction.Config -> throw IllegalArgumentException("This action doesn't use broadcasts")
    }

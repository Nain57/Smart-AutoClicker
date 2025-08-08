
package com.buzbuz.smartautoclicker.feature.notifications.service.model

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

internal fun getAllActionsBroadcastIntentFilter(): IntentFilter =
    IntentFilter().apply {
        addAction(ServiceNotificationAction.Play.getBroadcastAction())
        addAction(ServiceNotificationAction.Pause.getBroadcastAction())
        addAction(ServiceNotificationAction.Show.getBroadcastAction())
        addAction(ServiceNotificationAction.Hide.getBroadcastAction())
        addAction(ServiceNotificationAction.Stop.getBroadcastAction())
    }

internal fun Intent.toServiceNotificationAction(): ServiceNotificationAction? =
    when (action) {
        ServiceNotificationAction.Play.getBroadcastAction() -> ServiceNotificationAction.Play
        ServiceNotificationAction.Pause.getBroadcastAction() -> ServiceNotificationAction.Pause
        ServiceNotificationAction.Show.getBroadcastAction() -> ServiceNotificationAction.Show
        ServiceNotificationAction.Hide.getBroadcastAction() -> ServiceNotificationAction.Hide
        ServiceNotificationAction.Stop.getBroadcastAction() -> ServiceNotificationAction.Stop
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
        ServiceNotificationAction.Play -> NotificationActionPendingIntent.Broadcast(getBroadcastAction())
        ServiceNotificationAction.Pause -> NotificationActionPendingIntent.Broadcast(getBroadcastAction())
        ServiceNotificationAction.Show -> NotificationActionPendingIntent.Broadcast(getBroadcastAction())
        ServiceNotificationAction.Hide -> NotificationActionPendingIntent.Broadcast(getBroadcastAction())
        ServiceNotificationAction.Stop -> NotificationActionPendingIntent.Broadcast(getBroadcastAction())
        ServiceNotificationAction.Config -> NotificationActionPendingIntent.Activity(appComponentsProvider.scenarioActivityComponentName)
    }

private fun ServiceNotificationAction.getBroadcastAction(): String =
    when (this) {
        ServiceNotificationAction.Play -> "com.buzbuz.smartautoclicker.PLAY"
        ServiceNotificationAction.Pause -> "com.buzbuz.smartautoclicker.PAUSE"
        ServiceNotificationAction.Show -> "com.buzbuz.smartautoclicker.SHOW"
        ServiceNotificationAction.Hide -> "com.buzbuz.smartautoclicker.HIDE"
        ServiceNotificationAction.Stop -> "com.buzbuz.smartautoclicker.STOP"
        ServiceNotificationAction.Config -> throw IllegalArgumentException("This action doesn't use broadcasts")
    }
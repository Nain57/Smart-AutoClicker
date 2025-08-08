
package com.buzbuz.smartautoclicker.feature.notifications.service.ui

import android.app.Notification
import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.R
import com.buzbuz.smartautoclicker.feature.notifications.common.notificationIconResId
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationAction
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationState
import com.buzbuz.smartautoclicker.feature.notifications.service.model.getPendingIntent

internal class CustomLayoutNotificationBuilder(
    context: Context,
    channelId: String,
    initialState: ServiceNotificationState,
    private val appComponentsProvider: AppComponentsProvider,
) : ServiceNotificationBuilder(context, channelId) {

    init {
        setSmallIcon(notificationIconResId())
        setCategory(Notification.CATEGORY_SERVICE)
        setOngoing(true)
        setLocalOnly(true)
        setStyle(NotificationCompat.DecoratedCustomViewStyle())

        updateState(context, initialState)
    }

    override fun updateState(context: Context, state: ServiceNotificationState) {
        setCustomContentView(getCustomContentView(context, state))
        setCustomBigContentView(getCustomBigContentView(context, state))
    }

    private fun getCustomContentView(context: Context, state: ServiceNotificationState): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_service).apply {
            setTextViewText(
                R.id.text_scenario_name,
                context.getString(R.string.notification_title, "\n${state.scenarioName}")
            )

            addAction(
                context,
                R.id.button_play_pause,
                if (state.isScenarioRunning) ServiceNotificationAction.Pause else ServiceNotificationAction.Play
            )
            addAction(
                context,
                R.id.button_show_hide,
                if (state.isMenuVisible) ServiceNotificationAction.Hide else ServiceNotificationAction.Show
            )
        }

    private fun getCustomBigContentView(context: Context, state: ServiceNotificationState): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_service_big).apply {
            setTextViewText(R.id.text_scenario_name, state.scenarioName)

            addAction(
                context,
                R.id.button_play_pause,
                if (state.isScenarioRunning) ServiceNotificationAction.Pause else ServiceNotificationAction.Play
            )
            addAction(
                context,
                R.id.button_show_hide,
                if (state.isMenuVisible) ServiceNotificationAction.Hide else ServiceNotificationAction.Show
            )
            addAction(context, R.id.button_config, ServiceNotificationAction.Config)
            addAction(context, R.id.button_exit, ServiceNotificationAction.Stop)
        }

    private fun RemoteViews.addAction(context: Context, @IdRes viewId: Int, action: ServiceNotificationAction) {
        setImageViewResource(viewId, action.iconRes)
        setOnClickPendingIntent(viewId, action.getPendingIntent(context, appComponentsProvider))
    }
}



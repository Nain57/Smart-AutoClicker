
package com.buzbuz.smartautoclicker.feature.notifications.service.ui

import android.app.Notification
import android.content.Context

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.R
import com.buzbuz.smartautoclicker.feature.notifications.common.notificationIconResId
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationAction
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationState
import com.buzbuz.smartautoclicker.feature.notifications.service.model.getPendingIntent

internal class LegacyNotificationBuilder(
    context: Context,
    channelId: String,
    initialState: ServiceNotificationState,
    private val appComponentsProvider: AppComponentsProvider,
) : ServiceNotificationBuilder(context, channelId) {

    init {
        setContentTitle(context.getString(R.string.notification_title, initialState.scenarioName))
        setContentText(context.getString(R.string.notification_message))
        setContentIntent(ServiceNotificationAction.Config.getPendingIntent(context, appComponentsProvider))
        setSmallIcon(notificationIconResId())
        setCategory(Notification.CATEGORY_SERVICE)
        setOngoing(true)
        setLocalOnly(true)

        updateState(context, initialState)
    }

    override fun updateState(context: Context, state: ServiceNotificationState) {
        clearActions()

        addServiceNotificationAction(
            context,
            if (state.isScenarioRunning) ServiceNotificationAction.Pause else ServiceNotificationAction.Play,
        )
        addServiceNotificationAction(
            context,
            if (state.isMenuVisible) ServiceNotificationAction.Hide else ServiceNotificationAction.Show,
        )
        addServiceNotificationAction(context, ServiceNotificationAction.Stop)
    }

    private fun LegacyNotificationBuilder.addServiceNotificationAction(
        context: Context,
        action: ServiceNotificationAction,
    ) =
        addAction(
            action.iconRes,
            context.getString(action.textRes),
            action.getPendingIntent(context, appComponentsProvider),
        )
}



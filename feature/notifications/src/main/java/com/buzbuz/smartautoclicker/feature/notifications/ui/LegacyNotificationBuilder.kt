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
package com.buzbuz.smartautoclicker.feature.notifications.ui

import android.app.Notification
import android.content.Context

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.R
import com.buzbuz.smartautoclicker.core.ui.utils.notificationIconResId
import com.buzbuz.smartautoclicker.feature.notifications.model.ServiceNotificationAction
import com.buzbuz.smartautoclicker.feature.notifications.model.ServiceNotificationState
import com.buzbuz.smartautoclicker.feature.notifications.model.getPendingIntent

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



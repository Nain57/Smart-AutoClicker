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
package com.buzbuz.smartautoclicker.feature.notifications.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.buzbuz.smartautoclicker.feature.notifications.R


@RequiresApi(Build.VERSION_CODES.O)
internal fun createKlickrServiceNotificationChannel(context: Context): NotificationChannel =
    NotificationChannel(
        SERVICE_CHANNEL_ID,
        context.getString(R.string.notification_service_channel_name),
        NotificationManager.IMPORTANCE_LOW,
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun createUserScenarioNotificationChannel(context: Context): NotificationChannel =
    NotificationChannel(
        USER_SCENARIO_CHANNEL_ID,
        context.getString(R.string.notification_scenario_channel_name),
        NotificationManager.IMPORTANCE_LOW,
    )


/** The channel identifier for the foreground notification of the accessibility service. */
internal const val SERVICE_CHANNEL_ID = "KlickrService"
/** The channel identifier for the notification sent by a user scenario. */
internal const val USER_SCENARIO_CHANNEL_ID = "KlickrScenario"

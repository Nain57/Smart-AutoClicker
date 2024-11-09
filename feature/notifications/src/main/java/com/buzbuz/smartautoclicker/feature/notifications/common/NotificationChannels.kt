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
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

import com.buzbuz.smartautoclicker.feature.notifications.R


@RequiresApi(Build.VERSION_CODES.O)
internal fun createKlickrServiceNotificationChannel(context: Context): NotificationChannel =
    NotificationChannel(
        SERVICE_CHANNEL_ID,
        context.getString(R.string.notification_service_channel_name),
        NotificationManager.IMPORTANCE_LOW,
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun NotificationManagerCompat.createUserScenarioNotificationChannelGroup(context: Context) =
    createNotificationChannelGroup(
        NotificationChannelGroup(
            USER_SCENARIO_CHANNELS_GROUP_ID,
            context.getString(R.string.notification_scenario_channel_group_name),
        )
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun NotificationManagerCompat.createUserScenarioNotificationChannelMin(context: Context) =
    createNotificationChannel(
        NotificationChannel(
            USER_SCENARIO_MIN_CHANNEL_ID,
            context.getString(R.string.notification_scenario_channel_min_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply { group = USER_SCENARIO_CHANNELS_GROUP_ID }
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun NotificationManagerCompat.createUserScenarioNotificationChannelLow(context: Context) =
    createNotificationChannel(
        NotificationChannel(
            USER_SCENARIO_LOW_CHANNEL_ID,
            context.getString(R.string.notification_scenario_channel_low_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { group = USER_SCENARIO_CHANNELS_GROUP_ID }
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun NotificationManagerCompat.createUserScenarioNotificationChannelDefault(context: Context) =
    createNotificationChannel(
        NotificationChannel(
            USER_SCENARIO_DEFAULT_CHANNEL_ID,
            context.getString(R.string.notification_scenario_channel_default_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { group = USER_SCENARIO_CHANNELS_GROUP_ID }
    )

@RequiresApi(Build.VERSION_CODES.O)
internal fun NotificationManagerCompat.createUserScenarioNotificationChannelHigh(context: Context) =
    createNotificationChannel(
        NotificationChannel(
            USER_SCENARIO_HIGH_CHANNEL_ID,
            context.getString(R.string.notification_scenario_channel_high_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { group = USER_SCENARIO_CHANNELS_GROUP_ID }
    )

internal fun getUserScenarioNotificationChannelId(importance: Int): String =
    when (importance) {
        NotificationManager.IMPORTANCE_MIN -> USER_SCENARIO_MIN_CHANNEL_ID
        NotificationManager.IMPORTANCE_LOW -> USER_SCENARIO_LOW_CHANNEL_ID
        NotificationManager.IMPORTANCE_DEFAULT -> USER_SCENARIO_DEFAULT_CHANNEL_ID
        NotificationManager.IMPORTANCE_HIGH -> USER_SCENARIO_HIGH_CHANNEL_ID
        else -> SERVICE_CHANNEL_ID
    }


/** The channel identifier for the foreground notification of the accessibility service. */
internal const val SERVICE_CHANNEL_ID = "KlickrService"

private const val USER_SCENARIO_CHANNELS_GROUP_ID = "Klickr User Scenarios"
/** The channel identifier for the notification sent by a user scenario with MIN importance. */
private const val USER_SCENARIO_MIN_CHANNEL_ID = "KlickrScenario MIN"
/** The channel identifier for the notification sent by a user scenario with LOW importance. */
private const val USER_SCENARIO_LOW_CHANNEL_ID = "KlickrScenario LOW"
/** The channel identifier for the notification sent by a user scenario with DEFAULT importance. */
private const val USER_SCENARIO_DEFAULT_CHANNEL_ID = "KlickrScenario DEFAULT"
/** The channel identifier for the notification sent by a user scenario with HIGH importance. */
private const val USER_SCENARIO_HIGH_CHANNEL_ID = "KlickrScenario HIGH"

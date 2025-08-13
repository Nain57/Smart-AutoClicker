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

import android.content.Context
import androidx.core.app.NotificationCompat
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.model.ServiceNotificationState


internal abstract class ServiceNotificationBuilder(
    context: Context,
    channelId: String,
) : NotificationCompat.Builder(context, channelId) {

    abstract fun updateState(context: Context, state: ServiceNotificationState)
}

internal fun Context.newServiceNotificationBuilder(
    channelId: String,
    initialState: ServiceNotificationState,
    appComponentsProvider: AppComponentsProvider,
    forceLegacy: Boolean,
): ServiceNotificationBuilder {
    if (forceLegacy) return LegacyNotificationBuilder(this, channelId, initialState, appComponentsProvider)

    return try {
        CustomLayoutNotificationBuilder(this, channelId, initialState, appComponentsProvider)
    } catch (ex: Exception) {
        // Some devices doesn't support custom views in notification, use the regular format instead
        LegacyNotificationBuilder(this, channelId, initialState, appComponentsProvider)
    }
}



package com.buzbuz.smartautoclicker.feature.notifications.service.ui

import android.content.Context
import androidx.core.app.NotificationCompat
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationState


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


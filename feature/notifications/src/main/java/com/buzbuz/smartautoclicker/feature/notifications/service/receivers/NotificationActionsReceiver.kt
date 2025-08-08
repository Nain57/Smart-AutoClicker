
package com.buzbuz.smartautoclicker.feature.notifications.service.receivers

import android.content.Context
import android.content.Intent
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver
import com.buzbuz.smartautoclicker.feature.notifications.service.model.ServiceNotificationAction
import com.buzbuz.smartautoclicker.feature.notifications.service.model.getAllActionsBroadcastIntentFilter
import com.buzbuz.smartautoclicker.feature.notifications.service.model.toServiceNotificationAction


internal class NotificationActionsReceiver(
    private val onReceived: (ServiceNotificationAction) -> Unit,
): SafeBroadcastReceiver(getAllActionsBroadcastIntentFilter()) {

    override fun onReceive(context: Context, intent: Intent) {
        intent.toServiceNotificationAction()?.let { action ->
            Log.i(TAG, "Notification action received: ${intent.action}")
            onReceived(action)
        }
    }
}

private const val TAG = "NotificationActionsReceiver"
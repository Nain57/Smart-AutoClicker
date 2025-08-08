
package com.buzbuz.smartautoclicker.feature.notifications.service.receivers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver


internal class NightModeReceiver(
    private val onChanged: (context: Context, isNightMode: Boolean) -> Unit,
): SafeBroadcastReceiver(IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)) {

    var isNightModeEnabled: Boolean = false
        private set
        get() {
            if (!isRegistered) throw IllegalStateException("Can't get night mode value, listener is not registered")
            return field
        }

    override fun onRegistered(context: Context) {
        isNightModeEnabled = context.isNightModeEnabled()
    }

    override fun onUnregistered() {
        isNightModeEnabled = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        val newIsNightModeEnabled = context.isNightModeEnabled()
        if (isNightModeEnabled == newIsNightModeEnabled) return

        Log.i(TAG, "Ui mode changed, isNightModeEnabled=${isNightModeEnabled}")

        isNightModeEnabled = newIsNightModeEnabled
        onChanged(context, newIsNightModeEnabled)
    }
}

private fun Context.isNightModeEnabled(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private const val TAG = "NightModeReceiver"
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
package com.buzbuz.smartautoclicker.feature.notifications.receivers

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
/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.quality.domain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver

/**
 * Use to simulate the Quality while in debug.
 *
 * The following command will change the Quality:
 * adb shell "am broadcast \
 *     -a com.buzbuz.smartautoclicker.core.common.quality.domain.TEST_STATE \
 *     --es EXTRA_STATE Quality"
 *
 * With Quality being the class name of the wanted [Quality] or null.
 */
internal class DebugQualityReceiver(
    private val onNewQuality: (Quality?) -> Unit,
) : SafeBroadcastReceiver(IntentFilter(BROADCAST_ACTION)) {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        intent.getStringExtra(EXTRA_STATE).toQuality().let(onNewQuality)
    }

    private fun String?.toQuality(): Quality? {
        val quality = when (this) {
            "Unknown" -> Quality.Unknown
            "High" -> Quality.High
            "ExternalIssue" -> Quality.ExternalIssue
            "Crashed" -> Quality.Crashed
            "FirstTime" -> Quality.FirstTime
            null -> null
            else -> {
                Log.e(TAG, "Invalid quality")
                null
            }
        }

        Log.d(TAG, "Forcing $quality")

        return quality
    }
}

private const val BROADCAST_ACTION = "com.buzbuz.smartautoclicker.core.common.quality.domain.TEST_STATE"
private const val EXTRA_STATE = "EXTRA_STATE"

private const val TAG = "DebugQualityReceiver"
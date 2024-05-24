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
package com.buzbuz.smartautoclicker.feature.permissions.model

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log

/**
 * There is no reliable way to get the accessibility service state due to the multiple possible states across all
 * devices manufacturer. All possible APIs have their flaws and can return an invalid value in certain scenario.
 *
 * As we can't trust the device, we will only trust ourselves and the Android API documentation: As long as the
 * permission is granted, the application service is bound by the system, and stopped only when the permission has been
 * revoked. This means the permission state should be the same as the service running state, so to get a clear status
 * of the permission, you need to provide it with [isServiceRunning].
 */
data class PermissionAccessibilityService(
    private val componentName: ComponentName,
    private val isServiceRunning: () -> Boolean,
) : Permission.Special() {

    override fun isGranted(context: Context): Boolean =
        isServiceRunning()

    override fun onStartRequestFlow(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        val bundle = Bundle()
        val showArgs = componentName.flattenToString()
        bundle.putString(EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_SHOW_ACCESSIBILITY_FRAGMENT_ARGUMENTS, bundle)

        return try {
            context.startActivity(intent)
            true
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Can't find device accessibility service settings menu.")
            false
        }
    }
}

/** Intent extra bundle key for the Android settings app. */
private const val EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
/** Intent extra bundle key for the Android settings app. */
private const val EXTRA_SHOW_ACCESSIBILITY_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
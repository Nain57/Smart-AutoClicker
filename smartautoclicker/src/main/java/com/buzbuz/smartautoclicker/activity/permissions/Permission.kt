/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.activity.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.SmartAutoClickerService

sealed class Permission {

    enum class Type(val permission: Permission) {
        ACCESSIBILITY(Special.Accessibility),
        OVERLAY(Special.Overlay),
        NOTIFICATIONS(Dangerous.Notification),
    }

    fun isOptional(): Boolean = this is Optional

    fun hasBeenRequestedBefore(context: Context): Boolean =
        context.getPermissionSharedPrefs().getBoolean(javaClass.simpleName, false)

    fun startRequestFlow(context: Context) {
        context.getPermissionSharedPrefs()
            .edit()
            .putBoolean(javaClass.simpleName, true)
            .apply()

        onStartRequestFlow(context)
    }

    abstract val titleRes: Int
    abstract val descriptionRes: Int

    abstract fun isGranted(context: Context): Boolean
    protected abstract fun onStartRequestFlow(context: Context)

    sealed class Special : Permission() {

        data object Accessibility : Special() {

            override val titleRes: Int = R.string.dialog_title_permission_accessibility
            override val descriptionRes: Int = R.string.message_permission_desc_accessibility

            override fun isGranted(context: Context): Boolean =
                SmartAutoClickerService.isServiceStarted()

            override fun onStartRequestFlow(context: Context) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

                val bundle = Bundle()
                val showArgs = context.packageName + "/" + SmartAutoClickerService::class.java.name
                bundle.putString(EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY, showArgs)
                intent.putExtra(EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY, showArgs)
                intent.putExtra(EXTRA_SHOW_ACCESSIBILITY_FRAGMENT_ARGUMENTS, bundle)

                context.startActivity(intent)
            }
        }

        data object Overlay : Special() {

            override val titleRes: Int = R.string.dialog_title_permission_overlay
            override val descriptionRes: Int = R.string.message_permission_desc_overlay

            override fun isGranted(context: Context): Boolean =
                Settings.canDrawOverlays(context)

            override fun onStartRequestFlow(context: Context) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

                context.startActivity(intent)
            }
        }
    }

    sealed class Dangerous : Permission() {

        /** Launcher for requesting the permission. */
        protected lateinit var permissionLauncher: ActivityResultLauncher<String>

        fun initResultLauncher(fragment: Fragment, onResult: (isGranted: Boolean) -> Unit) {
            permissionLauncher = fragment
                .registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    onResult(granted)
                }
        }

        data object Notification : Dangerous(), Optional {

            override val titleRes: Int = R.string.dialog_title_permission_notification
            override val descriptionRes: Int = R.string.message_permission_desc_notification

            override fun isGranted(context: Context): Boolean =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
                else context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

            override fun onStartRequestFlow(context: Context) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Tells if the [Permission] is mandatory or not. */
    private interface Optional
}

/** Get the shared preferences file for the permissions*/
private fun Context.getPermissionSharedPrefs() =
    getSharedPreferences("permissions", Context.MODE_PRIVATE)

/** Intent extra bundle key for the Android settings app. */
private const val EXTRA_ACCESSIBILITY_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
/** Intent extra bundle key for the Android settings app. */
private const val EXTRA_SHOW_ACCESSIBILITY_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
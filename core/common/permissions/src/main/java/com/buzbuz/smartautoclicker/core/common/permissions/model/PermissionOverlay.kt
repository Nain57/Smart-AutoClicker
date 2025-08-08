
package com.buzbuz.smartautoclicker.core.common.permissions.model

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

data class PermissionOverlay(
    private val optional: Boolean = false,
) : Permission.Special(optional) {

    override fun isGranted(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    override fun onStartRequestFlow(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        return try {
            context.startActivity(intent)
            true
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Can't find device overlay settings menu.")
            false
        }
    }
}
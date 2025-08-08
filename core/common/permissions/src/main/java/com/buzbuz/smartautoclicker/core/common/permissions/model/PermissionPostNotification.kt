
package com.buzbuz.smartautoclicker.core.common.permissions.model

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.buzbuz.smartautoclicker.core.base.data.getNotificationSettingsIntent

@SuppressLint("InlinedApi")
data class PermissionPostNotification(
    private val optional: Boolean = false,
) : Permission.Dangerous(optional), Permission.ForApiRange {

    override val fromApiLvl: Int
        get() = Build.VERSION_CODES.TIRAMISU

    override val permissionString: String
        get() = Manifest.permission.POST_NOTIFICATIONS

    override val fallbackSettingsIntent: Intent
        get() = getNotificationSettingsIntent()

    override fun isGranted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}
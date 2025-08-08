
package com.buzbuz.smartautoclicker.core.base.data

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi


fun getOpenWebBrowserIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

fun getOpenWebBrowserPickerIntent(uri: Uri): Intent =
    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
        data = uri
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

@RequiresApi(Build.VERSION_CODES.O)
fun getNotificationSettingsIntent(): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Settings.EXTRA_APP_PACKAGE, "com.buzbuz.smartautoclicker")
    }
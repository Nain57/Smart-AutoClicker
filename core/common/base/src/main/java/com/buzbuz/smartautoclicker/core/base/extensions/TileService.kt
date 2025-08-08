
package com.buzbuz.smartautoclicker.core.base.extensions

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

fun TileService.startActivityAndCollapseCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startActivityAndCollapse(
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
    } else {
        @Suppress("StartActivityAndCollapseDeprecated", "DEPRECATION")
        startActivityAndCollapse(intent)
    }
}
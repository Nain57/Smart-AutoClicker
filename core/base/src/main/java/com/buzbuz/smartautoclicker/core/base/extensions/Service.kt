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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build

import androidx.core.app.ServiceCompat

fun Service.startForegroundMediaProjectionServiceCompat(notificationId : Int, notification: Notification) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )

        else -> startForeground(notificationId, notification)
    }
}

fun AccessibilityService.requestFilterKeyEvents(enabled: Boolean) {
    val info = serviceInfo

    // On some Xiaomi devices, serviceInfo is null. As a AOSP framework developer, I can tell you
    // this is REALLY bad. Try to force an empty one, but i'm not even sure this call is really
    // interpreted in their shitty phones.
    if (info == null) {
        serviceInfo = AccessibilityServiceInfo().apply {
            flags = if (enabled) AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS else 0
        }
        return
    }

    serviceInfo = info.apply {
        flags =
            if (enabled) flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            else flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
    }
}
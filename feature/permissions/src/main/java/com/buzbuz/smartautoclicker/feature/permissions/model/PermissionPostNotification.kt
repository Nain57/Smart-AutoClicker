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

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Build

@SuppressLint("InlinedApi")
data object PermissionPostNotification : Permission.Dangerous(), Permission.Optional, Permission.ForApiRange {

    override val fromApiLvl: Int
        get() = Build.VERSION_CODES.TIRAMISU

    override val permissionString: String
        get() = Manifest.permission.POST_NOTIFICATIONS

    override fun isGranted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}
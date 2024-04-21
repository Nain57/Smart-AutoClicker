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
package com.buzbuz.smartautoclicker.core.android.application

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build

data class AndroidApplicationInfo internal constructor(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
)

fun getAndroidApplicationInfo(packageManager: PackageManager, intent: Intent): AndroidApplicationInfo? =
    packageManager.resolveActivityCompat(intent, 0)
        ?.toAndroidApplicationInfo(packageManager)

fun getAllAndroidApplicationsInfo(packageManager: PackageManager): List<AndroidApplicationInfo> =
    packageManager.queryIntentActivitiesCompat(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        0,
    ).mapNotNull { it.toAndroidApplicationInfo(packageManager) }

private fun ResolveInfo.toAndroidApplicationInfo(packageManager: PackageManager): AndroidApplicationInfo? =
    activityInfo?.let { info ->
        AndroidApplicationInfo(
            ComponentName(info.packageName, info.name),
            info.loadLabel(packageManager).toString(),
            info.loadIcon(packageManager)
        )
    }

private fun PackageManager.resolveActivityCompat(intent: Intent, flags: Int): ResolveInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        resolveActivity(intent, flags)
    }

private fun PackageManager.queryIntentActivitiesCompat(intent: Intent, flags: Int): MutableList<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        queryIntentActivities(intent, flags)
    }
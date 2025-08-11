
package com.buzbuz.smartautoclicker.core.android.application

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build

data class AndroidApplicationInfo(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
)

fun PackageManager.getApplicationLabel(intent: Intent): String? =
    resolveActivityCompat(intent, 0)?.loadLabel(this)?.toString()

fun PackageManager.getAndroidApplicationInfo(intent: Intent): AndroidApplicationInfo? =
    resolveActivityCompat(intent, 0)?.toAndroidApplicationInfo(this)

fun PackageManager.getAllAndroidApplicationsInfo(): List<AndroidApplicationInfo> =
    queryIntentActivitiesCompat(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        0,
    ).mapNotNull { it.toAndroidApplicationInfo(this) }

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
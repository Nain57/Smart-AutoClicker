/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui.LocalePluginExecutionActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalePluginNotificationController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun showLaunchFallback(
        configurationJson: String,
        scenarioName: String,
        requestId: String,
    ): Boolean {
        ensureChannel()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Can't show launch fallback, notifications are disabled")
            showToast(R.string.locale_plugin_error_fallback_unavailable)
            return false
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_LAUNCH,
            LocalePluginExecutionActivity.createFallbackIntent(context, configurationJson, requestId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_LAUNCH,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.buzbuz.smartautoclicker.core.ui.R.drawable.ic_intent)
                .setContentTitle(context.getString(R.string.locale_plugin_notification_launch_title))
                .setContentText(context.getString(R.string.locale_plugin_notification_launch_text, scenarioName))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
        return true
    }

    @SuppressLint("MissingPermission")
    fun showError(messageRes: Int) {
        ensureChannel()
        showToast(messageRes)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_ERROR,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.buzbuz.smartautoclicker.core.ui.R.drawable.ic_warning)
                .setContentTitle(context.getString(R.string.locale_plugin_notification_error_title))
                .setContentText(context.getString(messageRes))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
    }

    fun cancelLaunchFallback() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_LAUNCH)
    }

    private fun showToast(messageRes: Int) {
        mainHandler.post {
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.locale_plugin_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }

}

private const val CHANNEL_ID = "locale_plugin_launches"
private const val NOTIFICATION_ID_LAUNCH = 7101
private const val NOTIFICATION_ID_ERROR = 7102
private const val TAG = "LocalePluginNotification"

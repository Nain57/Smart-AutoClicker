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
package com.buzbuz.smartautoclicker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.activity.ScenarioActivity


internal class ServiceNotificationManager(context: Context, private val actionsCallback: ServiceNotificationCallbacks) {

    companion object {
        /** The identifier for the foreground notification of this service. */
        internal const val NOTIFICATION_ID = 42
        /** The channel identifier for the foreground notification of this service. */
        private const val NOTIFICATION_CHANNEL_ID = "SmartAutoClickerService"

        /** Actions from the notification. */
        private const val INTENT_ACTION_PLAY_AND_HIDE = "com.buzbuz.smartautoclicker.PLAY_AND_HIDE"
        private const val INTENT_ACTION_PAUSE_AND_SHOW = "com.buzbuz.smartautoclicker.PAUSE_AND_SHOW"
        private const val INTENT_ACTION_HIDE = "com.buzbuz.smartautoclicker.HIDE"
        private const val INTENT_ACTION_SHOW = "com.buzbuz.smartautoclicker.SHOW"
        private const val INTENT_ACTION_STOP_SCENARIO = "com.buzbuz.smartautoclicker.ACTION_STOP_SCENARIO"

        /** Tag for logs. */
        private const val TAG = "ServiceNotificationManager"
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    private val notificationIntent: PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, ScenarioActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    private val notificationIcon: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.drawable.ic_notification_vector
        else R.drawable.ic_notification
    private val notificationMessage: String =
        context.getString(R.string.notification_message)
    private val notificationIntentFilter: IntentFilter = IntentFilter().apply {
        addAction(INTENT_ACTION_PLAY_AND_HIDE)
        addAction(INTENT_ACTION_PAUSE_AND_SHOW)
        addAction(INTENT_ACTION_SHOW)
        addAction(INTENT_ACTION_HIDE)
        addAction(INTENT_ACTION_STOP_SCENARIO)
    }

    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationBroadcastReceiver: BroadcastReceiver? = null

    fun createNotification(context: Context, scenarioName: String?): Notification {
        Log.i(TAG, "Create notification")

        val builder = notificationBuilder ?: createNotificationBuilder(context, scenarioName)

        if (notificationBroadcastReceiver == null) {
            notificationBroadcastReceiver = createNotificationActionReceiver()
            ContextCompat.registerReceiver(
                context,
                notificationBroadcastReceiver,
                notificationIntentFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }

        notificationBuilder = builder
        return builder.buildWithActions(context, isRunning = false, isMenuHidden = false)
    }

    fun updateNotificationState(context: Context, isRunning: Boolean, isMenuHidden: Boolean) {
        val builder = notificationBuilder ?: return
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return

        Log.i(TAG, "Updating notification, running=$isRunning; menuHidden=$isMenuHidden")

        notificationManager.notify(NOTIFICATION_ID, builder.buildWithActions(context, isRunning, isMenuHidden))
    }

    fun destroyNotification(context: Context) {
        notificationBuilder = null

        notificationBroadcastReceiver?.let(context::unregisterReceiver)
        notificationBroadcastReceiver = null

        Log.i(TAG, "Notification destroyed")
    }

    private fun createNotificationBuilder(context: Context, scenarioName: String?): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createNotificationChannel(context))
        }

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title, scenarioName ?: ""))
            .setContentText(notificationMessage)
            .setContentIntent(notificationIntent)
            .setSmallIcon(notificationIcon)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setLocalOnly(true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context): NotificationChannel =
        NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )

    private fun createNotificationActionReceiver() : BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return

                when (intent.action) {
                    INTENT_ACTION_PLAY_AND_HIDE -> actionsCallback.onPlayAndHide()
                    INTENT_ACTION_PAUSE_AND_SHOW -> actionsCallback.onPauseAndShow()
                    INTENT_ACTION_HIDE -> actionsCallback.onHide()
                    INTENT_ACTION_SHOW -> actionsCallback.onShow()
                    INTENT_ACTION_STOP_SCENARIO -> actionsCallback.onStop()
                }
            }
        }

    private fun NotificationCompat.Builder.buildWithActions(
        context: Context,
        isRunning: Boolean,
        isMenuHidden: Boolean,
    ): Notification {

        clearActions()
        when  {
            !isRunning && !isMenuHidden -> addPlayAndHideAction(context)
            isRunning && isMenuHidden -> addPauseAndShowAction(context)
            isRunning && !isMenuHidden -> addHideAction(context)
            else -> addShowAction(context)
        }
        addStopAction(context)
        return build()
    }

    private fun NotificationCompat.Builder.addPlayAndHideAction(context: Context): NotificationCompat.Builder =
        addAction(
            R.drawable.ic_play_arrow,
            context.getString(R.string.notification_button_play_and_hide),
            PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_PLAY_AND_HIDE), PendingIntent.FLAG_IMMUTABLE),
        )

    private fun NotificationCompat.Builder.addPauseAndShowAction(context: Context): NotificationCompat.Builder =
        addAction(
            R.drawable.ic_pause,
            context.getString(R.string.notification_button_pause_and_show),
            PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_PAUSE_AND_SHOW), PendingIntent.FLAG_IMMUTABLE),
        )

    private fun NotificationCompat.Builder.addHideAction(context: Context): NotificationCompat.Builder =
        addAction(
            R.drawable.ic_visible_off,
            context.getString(R.string.notification_button_toggle_menu),
            PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_HIDE), PendingIntent.FLAG_IMMUTABLE),
        )

    private fun NotificationCompat.Builder.addShowAction(context: Context): NotificationCompat.Builder =
        addAction(
            R.drawable.ic_visible_on,
            context.getString(R.string.notification_button_toggle_menu),
            PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_SHOW), PendingIntent.FLAG_IMMUTABLE),
        )

    private fun NotificationCompat.Builder.addStopAction(context: Context): NotificationCompat.Builder =
        addAction(
            R.drawable.ic_stop,
            context.getString(R.string.notification_button_stop),
            PendingIntent.getBroadcast(context, 0, Intent(INTENT_ACTION_STOP_SCENARIO), PendingIntent.FLAG_IMMUTABLE),
        )
}

interface ServiceNotificationCallbacks {
    fun onPlayAndHide(): Unit?
    fun onPauseAndShow(): Unit?
    fun onShow(): Unit?
    fun onHide(): Unit?
    fun onStop(): Unit?
}
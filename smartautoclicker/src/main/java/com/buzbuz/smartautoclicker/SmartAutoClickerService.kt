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
package com.buzbuz.smartautoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

import androidx.core.app.NotificationCompat

import com.buzbuz.smartautoclicker.SmartAutoClickerService.Companion.LOCAL_SERVICE_INSTANCE
import com.buzbuz.smartautoclicker.SmartAutoClickerService.Companion.getLocalService
import com.buzbuz.smartautoclicker.activity.ScenarioActivity
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.extensions.requestFilterKeyEvents
import com.buzbuz.smartautoclicker.core.base.extensions.startForegroundMediaProjectionServiceCompat
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import java.io.FileDescriptor
import java.io.PrintWriter

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * AccessibilityService implementation for the SmartAutoClicker.
 *
 * Started automatically by Android once the user has defined this service has an accessibility service, it provides
 * an API to start and stop the DetectorEngine correctly in order to display the overlay UI and record the screen for
 * clicks detection.
 * This API is offered through the [LocalService] class, which is instantiated in the [LOCAL_SERVICE_INSTANCE] object.
 * This system is used instead of the usual binder interface because an [AccessibilityService] already has its own
 * binder and it can't be changed. To access this local service, use [getLocalService].
 *
 * We need this service to be an accessibility service in order to inject the detected event on the currently
 * displayed activity. This injection is made by the [dispatchGesture] method, which is called everytime an event has
 * been detected.
 */
class SmartAutoClickerService : AccessibilityService(), AndroidExecutor {

    companion object {
        /** The identifier for the foreground notification of this service. */
        private const val NOTIFICATION_ID = 42
        /** The channel identifier for the foreground notification of this service. */
        private const val NOTIFICATION_CHANNEL_ID = "SmartAutoClickerService"
        /** The instance of the [ILocalService], providing access for this service to the Activity. */
        private var LOCAL_SERVICE_INSTANCE: ILocalService? = null
            set(value) {
                field = value
                LOCAL_SERVICE_CALLBACK?.invoke(field)
            }
        /** Callback upon the availability of the [LOCAL_SERVICE_INSTANCE]. */
        private var LOCAL_SERVICE_CALLBACK: ((ILocalService?) -> Unit)? = null
            set(value) {
                field = value
                value?.invoke(LOCAL_SERVICE_INSTANCE)
            }

        /**
         * Static method allowing an activity to register a callback in order to monitor the availability of the
         * [ILocalService]. If the service is already available upon registration, the callback will be immediately
         * called.
         *
         * @param stateCallback the object to be notified upon service availability.
         */
        fun getLocalService(stateCallback: ((ILocalService?) -> Unit)?) {
            LOCAL_SERVICE_CALLBACK = stateCallback
        }

        fun isServiceStarted(): Boolean = LOCAL_SERVICE_INSTANCE != null
    }

    interface ILocalService {

        fun startDumbScenario(dumbScenario: DumbScenario)
        fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
        fun onKeyEvent(event: KeyEvent?): Boolean
        fun stop()
        fun release()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        LOCAL_SERVICE_INSTANCE = LocalService(
            context = this,
            androidExecutor = this,
            onStart = { isSmart, name ->
                if (isSmart) startForegroundMediaProjectionServiceCompat(NOTIFICATION_ID, createNotification(name))
                requestFilterKeyEvents(true)
            },
            onStop = {
                requestFilterKeyEvents(false)
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            },
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        LOCAL_SERVICE_INSTANCE?.stop()
        LOCAL_SERVICE_INSTANCE?.release()
        LOCAL_SERVICE_INSTANCE = null

        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean =
        LOCAL_SERVICE_INSTANCE?.onKeyEvent(event) ?: super.onKeyEvent(event)

    /**
     * Create the notification for this service allowing it to be set as foreground service.
     *
     * @param scenarioName the name to de displayed in the notification title
     *
     * @return the newly created notification.
     */
    private fun createNotification(scenarioName: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }

        val intent = Intent(this, ScenarioActivity::class.java)
        val icon =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.drawable.ic_notification_vector
            else R.drawable.ic_notification

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title, scenarioName))
            .setContentText(getString(R.string.notification_message))
            .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(icon)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setLocalOnly(true)
            .build()
    }

    override suspend fun executeGesture(gestureDescription: GestureDescription) {
        suspendCoroutine<Unit?> { continuation ->
            dispatchGesture(
                gestureDescription,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) = continuation.resume(null)
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Gesture cancelled: $gestureDescription")
                        continuation.resume(null)
                    }
                },
                null,
            )
        }
    }

    override fun executeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            Log.w(TAG, "Can't start activity, it is not found.")
        } catch (arex: AndroidRuntimeException) {
            Log.w(TAG, "Can't start activity, Intent is invalid: $intent", arex)
        }
    }

    override fun executeSendBroadcast(intent: Intent) {
        try {
            sendBroadcast(intent)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't send broadcast, Intent is invalid: $intent", iaex)
        }
    }

    /**
     * Dump the state of the service via adb.
     * adb shell "dumpsys activity service com.buzbuz.smartautoclicker.debug/com.buzbuz.smartautoclicker.SmartAutoClickerService"
     */
    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        super.dump(fd, writer, args)

        if (writer == null) return

        (LOCAL_SERVICE_INSTANCE as? LocalService)
            ?.dump(writer) ?: writer.println("None")
    }

    override fun onInterrupt() { /* Unused */ }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Unused */ }
}

/** Tag for the logs. */
private const val TAG = "SmartAutoClickerService"
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
package com.buzbuz.smartautoclicker.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.AndroidRuntimeException
import android.util.Log
import kotlinx.coroutines.delay

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ServiceActionExecutor(private val service: AccessibilityService) {

    private val gestureResultCallback = SuspendableGestureResultCallback()

    suspend fun safeDispatchGesture(gestureDescription: GestureDescription) {
        if (!dispatchGestureSync(gestureDescription)) {
            Log.w(TAG, "System did not execute the gesture properly, delaying processing to avoid spamming slow system")
            delay(500)
        }
    }

    private suspend fun dispatchGestureSync(gestureDescription: GestureDescription): Boolean =
        suspendCoroutine { continuation ->
            try {
                service.dispatchGesture(
                    gestureDescription,
                    gestureResultCallback.withContinuation(continuation),
                    null,
                )
            } catch (rEx: RuntimeException) {
                Log.w(TAG, "System is not responsive, the user might be spamming gesture too quickly", rEx)
                continuation.resume(false)
            }
        }

    fun safeStartActivity(intent: Intent) {
        try {
            service.startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            Log.w(TAG, "Can't start activity, it is not found.")
        } catch (arex: AndroidRuntimeException) {
            Log.w(TAG, "Can't start activity, Intent is invalid: $intent", arex)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't start activity, Intent contains invalid arguments: $intent")
        } catch (secEx: SecurityException) {
            Log.w(TAG, "Can't start activity with intent $intent, permission is denied by the system")
        } catch (npe: NullPointerException) {
            Log.w(TAG, "Can't start activity with intent $intent, intent is invalid")
        }
    }

    fun safeSendBroadcast(intent: Intent) {
        try {
            service.sendBroadcast(intent)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't send broadcast, Intent is invalid: $intent", iaex)
        }
    }
}

private const val TAG = "ServiceActionExecutor"
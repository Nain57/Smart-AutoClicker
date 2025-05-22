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
import com.buzbuz.smartautoclicker.core.base.Dumpable
import kotlinx.coroutines.delay
import java.io.PrintWriter


class ServiceActionExecutor(private val service: AccessibilityService) : Dumpable {

    private val gestureExecutor: GestureExecutor = GestureExecutor()

    fun reset() {
        gestureExecutor.reset()
    }

    suspend fun safeDispatchGesture(gestureDescription: GestureDescription) {
        if (!gestureExecutor.dispatchGesture(service, gestureDescription)) {
            Log.w(TAG, "System did not execute the gesture properly, delaying processing to avoid spamming slow system")
            delay(500)
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

    fun safePerformGlobalBack() {
        try {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } catch (ex: Exception) {
            Log.w(TAG, "Can't execute back button action", ex)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        gestureExecutor.dump(writer, prefix)
    }
}

private const val TAG = "ServiceActionExecutor"

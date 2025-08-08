
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

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        gestureExecutor.dump(writer, prefix)
    }
}

private const val TAG = "ServiceActionExecutor"
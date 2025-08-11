
package com.buzbuz.smartautoclicker.core.domain.model

import android.content.Intent
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import android.accessibilityservice.GestureDescription

/** Execute the actions related to Android. */
interface SmartActionExecutor : AndroidExecutor {

    /** Start the activity defined by the provided intent. */
    fun executeStartActivity(intent: Intent)

    /** Send a broadcast defined by the provided intent. */
    fun executeSendBroadcast(intent: Intent)

    /** Send a notification defined by the provided NotificationRequest. */
    fun executeNotification(notification: NotificationRequest)

    fun getScreenBounds(): Rect? // from rootInActiveWindow.boundsInScreen

    fun executeGlobalBack(): Boolean
    fun executeGlobalHome(): Boolean
    fun executeGlobalRecents(): Boolean
    fun executeGlobalNotifications(): Boolean
    fun executeGlobalQuickSettings(): Boolean

    fun executeScreenshot(roi: Rect?, savePath: String?): Boolean

    fun tapSafeArea(): Boolean // short tap around (40,80) with tiny jitter

    fun executeSetText(text: String): Boolean
    fun executeImeKeySequence(codes: List<Int>, intervalMs: Long): Boolean

    /** Request to reset any state related to action execution, most likely because a new session is starting. */
    fun clearState()
}

data class NotificationRequest(
    val actionId: Long,
    val eventId: Long,
    val title: String,
    val message: String,
    val groupName: String,
    val importance: Int,
)
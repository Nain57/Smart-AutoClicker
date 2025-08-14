/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationRequest

interface AndroidActionExecutor: Dumpable {

    /**
     * Initialize the executor.
     *
     * No action can be executed until this method is called.
     *
     * @param service the Android AccessibilityService allowing to execute the actions. It will be kept as a weak
     * reference to avoid any potential leak, so if actions are not executed after init call, you might need to check if
     * the service is correctly started.
     */
    fun init(service: AccessibilityService)

    /** Reset internal state. Actions currently executing will be cancelled. */
    fun resetState()

    /**
     * Release any resources held by the executor.
     * Should be called when the service is destroyed.
     */
    fun clear()

    /**
     * Dispatch a gesture to the touch screen.
     *
     * Any gestures currently in progress, whether from the user, this service, or another service, will be cancelled.
     * Method will return only once the gesture is dispatched.
     *
     * The gesture will be dispatched as if it were performed directly on the screen by a user, so the events may be
     * affected by features such as magnification and explore by touch.
     *
     * <strong>Note:</strong> In order to dispatch gestures, your service must declare the capability by setting the
     * android.R.styleable.AccessibilityService_canPerformGestures property in its meta-data. For more information,
     * see SERVICE_META_DATA.
     *
     * @param gestureDescription The gesture to dispatch
     */
    suspend fun dispatchGesture(gestureDescription: GestureDescription)

    /**
     * Performs a global action.
     *
     * Such an action can be performed at any moment regardless of the current application or user location in that
     * application. For example going back, going home, opening recents, etc.
     *
     * @param globalAction Perform actions using ids like the id constants referenced below:
     * - @see AccessibilityService.GLOBAL_ACTION_BACK
     * - @see AccessibilityService.GLOBAL_ACTION_HOME
     * - @see AccessibilityService.GLOBAL_ACTION_RECENTS
     */
    fun performGlobalAction(globalAction: Int)

    /**
     * Action that sets the text of the currently focused item on the screen.
     *
     * Performing the action with an empty text will clear the text in the view. This action will also put the
     * cursor at the end of text.
     */
    fun writeTextOnFocusedItem(text: String, validate: Boolean)

    /**
     * Launch a new activity.
     *
     * As the intent is built by the user, nothing ensures it will be correctly dispatched and not cause exceptions from
     * the android framework. This method caught all those use cases and can be safely used with any intent.
     *
     * Please note that nothing ensure the intent is effectively sent if malformed.
     */
    fun startActivity(intent: Intent)

    /**
     * Send an Android broadcast
     *
     * As the intent is built by the user, nothing ensures it will be correctly dispatched and not cause exceptions from
     * the android framework. This method caught all those use cases and can be safely used with any intent.
     *
     * Please note that nothing ensure the intent is effectively sent if malformed.
     */
    fun sendBroadcast(intent: Intent)

    /**
     * Post an Android notification on the device.
     *
     * Notifications are queued and flushed periodically to avoid being flagged as spam by the system. Please note that
     * unlike all other methods from this executor, this one will return before the action is effectively executed (iow,
     * due to the queuing system).
     */
    fun postNotification(notificationRequest: ActionNotificationRequest)
}

/** The maximum supported duration for a gesture. This limitation comes from Android GestureStroke API.  */
const val GESTURE_DURATION_MAX_VALUE = 59_999L
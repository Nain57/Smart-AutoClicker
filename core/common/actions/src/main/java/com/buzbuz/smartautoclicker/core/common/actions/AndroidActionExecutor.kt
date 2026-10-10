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
import android.content.Intent

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.common.actions.model.ActionNotificationRequest
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

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
     * The pointer a gesture holding its first finger left down on the screen, or null when nothing is held.
     *
     * It must be given to AndroidGestureBuilder.build for every gesture dispatched while it is held: the system only
     * honours a continuation when the first step of the dispatched gesture holds exactly one stroke for each pointer
     * currently down, so a gesture built without it would have the hold cancelled.
     */
    fun getHeldPointer(): HeldPointer?

    /**
     * Dispatch a gesture built by an AndroidGestureBuilder, and keep track of the pointer it leaves down.
     *
     * Method will return only once the whole gesture is dispatched.
     *
     * @param gesture the gesture to dispatch.
     */
    suspend fun dispatchBuiltGesture(gesture: BuiltGesture)

    /**
     * Verify the pointer left down on the screen by a gesture holding its first finger, if any, is still held.
     *
     * The system lifts it without notice when the user touches the screen. Once lost, [getHeldPointer] returns null.
     * Nothing is dispatched to the system, the user touches being watched while a pointer is held, so it can be called
     * on every frame from any thread.
     *
     * @return true if a pointer is still held, false if it has been lost or if nothing was held.
     */
    fun checkHeldPointer(): Boolean

    /**
     * Lift the pointer left down on the screen by a gesture holding its first finger, if any.
     *
     * Method will return only once the pointer is lifted.
     */
    suspend fun releaseHeldPointer()

    /**
     * Some pixel devices have an OS issue on Android 15 & 16.
     * Injecting a double tap somewhere seems to fix the problem most of the time. This method will take care of
     * injecting this gesture, it will avoid spamming it by skipping unblock requests to close to the previous one.
     *
     * @see [com.buzbuz.smartautoclicker.core.base.workarounds.isImpactedByInputBlock]
     */
    suspend fun dispatchPixelWorkaroundGestureIfNeeded()

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
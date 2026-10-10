/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.SystemClock
import android.util.Log

import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.toContinuation
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class HeldPointerManager @Inject constructor (
    private val userTouchWatcher: UserTouchWatcher,
) {

    /** The pointer the last dispatched gesture left down on the screen, if any. */
    var heldPointer: HeldPointer? = null
        private set

    /** Uptime of the start of the dispatch of the gesture that put [heldPointer] down. */
    private var heldSinceMs: Long = 0L

    /**
     * Called when the pointer holding the finger have changed its gesture. Null if not pointer is being held.
     *
     * @param dispatchStartMs the uptime of the start of the dispatch of the gesture leaving [pointer] down. It becomes
     * the start of the hold only if no pointer was held before: a continuation keeps the original start, so a user
     * touch made while it was dispatched is still detected.
     */
    fun onHeldPointerContinuationChanged(pointer: HeldPointer?, dispatchStartMs: Long = SystemClock.uptimeMillis()) {
        if (heldPointer == null) heldSinceMs = dispatchStartMs
        heldPointer = pointer
    }

    /** Called when a gesture continuation the held pointer have been cancelled, leading to hold being released. */
    fun onHeldPointerCancelled() {
        heldPointer = null
    }

    /**
     * Verify the pointer left down on the screen, if any, is still held by the system.
     *
     * The system silently lifts a held pointer when the user touches the screen: the gesture that left it down has
     * already completed, so there is no callback to be notified. Any user touch made since the start of the hold
     * therefore means the pointer is lost, and it is dropped: the next gesture must start a new stroke instead of
     * continuing a pointer that is no longer down.
     *
     * @return true if the pointer is still held or if there was none, false if it has been lost.
     */
    fun checkHeldPointer(): Boolean {
        if (heldPointer == null || !userTouchWatcher.hasUserTouchedSince(heldSinceMs)) return true

        heldPointer = null
        return false
    }

    /** Lift the pointer left down on the screen, if any, and wait for the system to have done it. */
    suspend fun releaseHeldPointer(dispatcher: suspend (GestureDescription) -> Boolean): Boolean {
        val release = heldPointer?.toReleaseGesture() ?: let {
            heldPointer = null
            return true
        }
        heldPointer = null

        return dispatcher(release)
    }

    /**
     * Lift the pointer left down on the screen without waiting for the system.
     *
     * A pointer left down freezes the touch screen of the user, so the shutdown paths must be able to let go of it
     * even when they can't suspend.
     */
    fun releaseHeldPointerNow(service: AccessibilityService?) {
        val release = heldPointer?.toReleaseGesture() ?: let {
            heldPointer = null
            return
        }
        heldPointer = null

        if (service == null) {
            Log.w(TAG, "Can't release the held pointer, service is not available anymore")
            return
        }

        try {
            service.dispatchGesture(release, null, null)
        } catch (rEx: RuntimeException) {
            Log.w(TAG, "Can't release the held pointer, system is not responsive", rEx)
        }
    }
}

/**
 * The gesture lifting a held pointer: a continuation of its stroke, as short as possible, ending the touch sequence
 * where the pointer was resting.
 */
private fun HeldPointer.toReleaseGesture(): GestureDescription? {
    val releaseStroke = stroke.toContinuation(position, position, willContinue = false) ?: return null

    return try {
        GestureDescription.Builder()
            .addStroke(releaseStroke)
            .build()
    } catch (ex: IllegalArgumentException) {
        Log.w(TAG, "Can't build the gesture releasing the held pointer", ex)
        null
    } catch (ex: IllegalStateException) {
        Log.w(TAG, "Can't build the gesture releasing the held pointer", ex)
        null
    }
}

private const val TAG = "HeldPointerManager"
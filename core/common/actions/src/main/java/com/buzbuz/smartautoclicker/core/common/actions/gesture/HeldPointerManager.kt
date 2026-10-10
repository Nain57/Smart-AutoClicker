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
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx.continueWith
import com.buzbuz.smartautoclicker.core.common.actions.model.HELD_POINTER_RELEASE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HeldPointerManager @Inject constructor () {

    /** The pointer the last dispatched gesture left down on the screen, if any. */
    var heldPointer: HeldPointer? = null
        private set

    /** Called when the pointer holding the finger have changed its gesture. Null if not pointer is being held. */
    fun onHeldPointerContinuationChanged(pointer: HeldPointer?) {
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
     * already completed, so there is no callback to be notified. The only way to know is to continue its stroke, the
     * system refusing a continuation of a pointer that is no longer down. The probe keeps the pointer down, and becomes
     * the stroke to continue afterwards.
     *
     * The system also refuses a gesture emitting no motion event, which is the case of a pointer continuing without
     * moving. The probe moves it by one pixel, back and forth between two positions, to stay below any touch slop.
     *
     * @return true if the pointer is still held or if there was none, false if it has been lost.
     */
    suspend fun checkHeldPointer(dispatcher: suspend (GestureDescription) -> Boolean): Boolean {
        Log.d(TAG, "Check held pointer state")
        val pointer = heldPointer ?: return true
        val probePosition = pointer.position.toProbePosition()
        val probeStroke = pointer.stroke.toContinuation(pointer.position, probePosition, willContinue = true) ?: let {
            heldPointer = null
            return false
        }

        heldPointer = HeldPointer(probeStroke, probePosition, pointer.owner)

        val startNs = SystemClock.elapsedRealtimeNanos()
        val isHeld = dispatcher(GestureDescription.Builder().addStroke(probeStroke).build())
        Log.d(TAG, "Held pointer probe: held=$isHeld, duration=${(SystemClock.elapsedRealtimeNanos() - startNs) / 1_000}µs")

        if (!isHeld) heldPointer = null
        return isHeld
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

/** A continuation of this stroke, as short as possible, from where the pointer rests to [to]. */
private fun StrokeDescription.toContinuation(from: Point, to: Point, willContinue: Boolean): StrokeDescription? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

    return try {
        continueWith(
            path = Path().apply { if (from == to) hold(from) else line(from, to) },
            startTimeMs = 0L,
            durationMs = HELD_POINTER_RELEASE_DURATION_MS,
            willContinue = willContinue,
        )
    } catch (ex: IllegalArgumentException) {
        Log.w(TAG, "Can't build the continuation of the held pointer stroke", ex)
        null
    } catch (ex: IllegalStateException) {
        Log.w(TAG, "Can't build the continuation of the held pointer stroke", ex)
        null
    }
}

/** The position a probe moves a held pointer to: one pixel aside, alternating between two positions. */
private fun Point.toProbePosition(): Point =
    Point(if (x % 2 == 0) x + 1 else x - 1, y)

private const val TAG = "HeldPointerManager"
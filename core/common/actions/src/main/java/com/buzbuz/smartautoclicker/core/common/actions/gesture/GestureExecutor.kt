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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds


@Singleton
internal class GestureExecutor @Inject constructor() : Dumpable {

    private var completedGestures: Long = 0L
    private var cancelledGestures: Long = 0L
    private var errorGestures: Long = 0L

    /** The pointer the last dispatched gesture left down on the screen, if any. */
    private var heldPointer: HeldPointer? = null


    fun clear(service: AccessibilityService?) {
        releaseHeldPointerNow(service)
        completedGestures = 0L
        cancelledGestures = 0L
        errorGestures = 0L
    }

    fun getHeldPointer(): HeldPointer? = heldPointer

    /**
     * Dispatch a gesture built by an [AndroidGestureBuilder], and record the pointer it leaves down on the screen.
     *
     * A cancelled or timed out dispatch means the system has torn down whatever this service had down, so the held
     * pointer is dropped: continuing its stroke afterwards would only get the next gesture refused.
     */
    suspend fun dispatchBuiltGesture(service: AccessibilityService, gesture: BuiltGesture): Boolean {
        for (chain in gesture.chains) {
            if (dispatchGestures(service, chain)) continue

            heldPointer = null
            return false
        }

        heldPointer = gesture.heldPointer
        return true
    }

    /** Lift the pointer left down on the screen, if any, and wait for the system to have done it. */
    suspend fun releaseHeldPointer(service: AccessibilityService?): Boolean {
        val release = heldPointer?.toReleaseGesture() ?: let {
            heldPointer = null
            return true
        }
        heldPointer = null

        if (service == null) {
            Log.w(TAG, "Can't release the held pointer, service is not available anymore")
            return false
        }

        return dispatchGesture(service, release)
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

    suspend fun dispatchGesture(service: AccessibilityService, gesture: GestureDescription): Boolean =
        dispatchGestures(service, listOf(gesture))

    /**
     * Dispatch a chain of gestures, each one continuing the pointers left down by the previous one.
     *
     * They are all dispatched without waiting for their individual result: the system appends a gesture continuing
     * the pointers currently down to what it has already scheduled, which keeps the strokes of a finger seamless.
     * Waiting for each result instead would freeze the fingers on the screen between two of them.
     */
    suspend fun dispatchGestures(service: AccessibilityService, gestures: List<GestureDescription>): Boolean {
        if (gestures.isEmpty()) return true

        val results = List(gestures.size) { CompletableDeferred<Boolean>() }
        var dispatchFailed = false

        gestures.forEachIndexed { index, gesture ->
            if (dispatchFailed) {
                // The chain is broken, the remaining gestures would continue pointers that are no longer down.
                results[index].complete(false)
                return@forEachIndexed
            }

            try {
                service.dispatchGesture(
                    /* gesture = */ gesture,
                    /* callback = */ results[index].toGestureResultCallback(),
                    /* handler = */ null,
                )
            } catch (rEx: RuntimeException) {
                Log.w(TAG, "System is not responsive, the user might be spamming gesture too quickly", rEx)
                dispatchFailed = true
                results[index].complete(false)
            }
        }

        val allCompleted = withTimeoutOrNull(gestures.timeoutDurationMs().milliseconds) {
            results.map { result -> result.await() }.all { completed -> completed }
        }

        if (allCompleted == null) {
            Log.w(TAG, "Gesture error, timeout or system error occurred.")
            errorGestures++
            return false
        }

        if (!allCompleted) {
            Log.w(TAG, "Gesture has been cancelled.")
            cancelledGestures++
            return false
        }

        completedGestures++
        return true
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = "${prefix.addDumpTabulationLvl()}- "

        writer.apply {
            append(prefix).println("* GestureExecutor:")
            append(contentPrefix).append("Completed=$completedGestures").println()
            append(contentPrefix).append("Cancelled=$cancelledGestures").println()
            append(contentPrefix).append("Error=$errorGestures").println()
            append(contentPrefix).append("HeldPointer=${heldPointer != null}, owner=${heldPointer?.owner}").println()
        }
    }
}

/**
 * The gesture lifting a held pointer: a continuation of its stroke, as short as possible, ending the touch sequence
 * where the pointer was resting.
 */
private fun HeldPointer.toReleaseGesture(): GestureDescription? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

    return try {
        GestureDescription.Builder()
            .addStroke(
                stroke.continueWith(
                    path = Path().apply { hold(position) },
                    startTimeMs = 0L,
                    durationMs = HELD_POINTER_RELEASE_DURATION_MS,
                    willContinue = false,
                )
            )
            .build()
    } catch (ex: IllegalArgumentException) {
        Log.w(TAG, "Can't build the gesture releasing the held pointer", ex)
        null
    } catch (ex: IllegalStateException) {
        Log.w(TAG, "Can't build the gesture releasing the held pointer", ex)
        null
    }
}

private fun CompletableDeferred<Boolean>.toGestureResultCallback(): GestureResultCallback =
    object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            complete(true)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            complete(false)
        }
    }

private fun GestureDescription.durationMs(): Long {
    var maxEndTime = 0L
    for (i in 0 until strokeCount) {
        val stroke = getStroke(i)
        maxEndTime = maxOf(maxEndTime, stroke.startTime + stroke.duration)
    }

    return maxEndTime
}

private fun List<GestureDescription>.timeoutDurationMs(): Long =
    (sumOf { gesture -> gesture.durationMs() } * GESTURE_CALLBACK_TIMEOUT_DURATION_RATIO_MS)
        .coerceIn(GESTURE_CALLBACK_TIMEOUT_MIN_MS, GESTURE_CALLBACK_TIMEOUT_MAX_MS)

private const val GESTURE_CALLBACK_TIMEOUT_MIN_MS = 100L
private const val GESTURE_CALLBACK_TIMEOUT_MAX_MS = 65_000L
private const val GESTURE_CALLBACK_TIMEOUT_DURATION_RATIO_MS = 2

private const val TAG = "GestureExecutor"

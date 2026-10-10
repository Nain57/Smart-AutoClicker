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
import android.os.SystemClock
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.workarounds.buildUnblockGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds


@Singleton
internal class GestureExecutor @Inject constructor(
    private val heldPointerManager: HeldPointerManager,
    private val userTouchWatcher: UserTouchWatcher,
) : Dumpable {

    /**
     * Unblock gesture can be annoying when automating, and the pixel issue isn't occurring every minutes so
     * it is safe to debounce.
     */
    private val unblockGestureDebouncer: GestureDebouncer =
        GestureDebouncer(UNBLOCK_GESTURE_DELAY_MS)

    private var completedGestures: Long = 0L
    private var cancelledGestures: Long = 0L
    private var errorGestures: Long = 0L


    fun clear(service: AccessibilityService?) {
        heldPointerManager.releaseHeldPointerNow(service)
        userTouchWatcher.stop()
        completedGestures = 0L
        cancelledGestures = 0L
        errorGestures = 0L
    }

    fun getHeldPointer(): HeldPointer? = heldPointerManager.heldPointer

    /**
     * Dispatch a gesture built by an [com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder],
     * and record the pointer it leaves down on the screen.
     *
     * A cancelled or timed out dispatch means the system has torn down whatever this service had down, so the held
     * pointer is dropped: continuing its stroke afterwards would only get the next gesture refused.
     *
     * The user touches are watched from the start of the dispatch of a gesture leaving a pointer down, so a touch
     * interrupting the gesture itself is noticed, and until the pointer is no longer held.
     */
    suspend fun dispatchBuiltGesture(service: AccessibilityService, gesture: BuiltGesture): Boolean {
        val dispatchStartMs = SystemClock.uptimeMillis()
        if (gesture.heldPointer != null) userTouchWatcher.start(service)

        for (chain in gesture.chains) {
            if (!dispatchGestures(service, chain)) {
                heldPointerManager.onHeldPointerCancelled()
                userTouchWatcher.stop()
                return false
            }
        }

        heldPointerManager.onHeldPointerContinuationChanged(gesture.heldPointer, dispatchStartMs)
        if (heldPointerManager.heldPointer == null) userTouchWatcher.stop()
        return true
    }

    /**
     * Some pixel devices have an OS issue on Android 15 & 16.
     * Injecting a double tap somewhere seems to fix the problem most of the time. This method will take care of
     * injecting this gesture, it will avoid spamming it by skipping unblock requests to close to the previous one.
     */
    suspend fun dispatchPixelWorkaroundGestureIfNeeded(service: AccessibilityService) {
        if (heldPointerManager.heldPointer != null) return

        if (unblockGestureDebouncer.shouldTrigger()) {
            Log.i(TAG, "Injecting unblock gesture")
            dispatchGesture(service, GestureDescription.Builder().buildUnblockGesture())
        }
    }

    /**
     * Called on every processed frame while a pointer is held, from the processing thread. It only compares the start
     * of the hold with the last user touch, there is nothing dispatched to the system.
     *
     * The watcher window is kept once the hold is lost: the event holding it is often detected again right away, and
     * removing then adding back the window while the user is still touching the screen confuses the system.
     */
    fun checkHeldPointer(): Boolean =
        heldPointerManager.checkHeldPointer()

    /** Lift the pointer left down on the screen, if any, and wait for the system to have done it. */
    suspend fun releaseHeldPointer(service: AccessibilityService?): Boolean {
        userTouchWatcher.stop()

        return heldPointerManager.releaseHeldPointer { release ->
            if (service == null) {
                Log.w(TAG, "Can't release the held pointer, service is not available anymore")
                false
            } else {
                dispatchGesture(service, release)
            }
        }
    }

    private suspend fun dispatchGesture(service: AccessibilityService, gesture: GestureDescription): Boolean =
        dispatchGestures(service, listOf(gesture))

    /**
     * Dispatch a chain of gestures, each one continuing the pointers left down by the previous one.
     *
     * They are all dispatched without waiting for their individual result: the system appends a gesture continuing
     * the pointers currently down to what it has already scheduled, which keeps the strokes of a finger seamless.
     * Waiting for each result instead would freeze the fingers on the screen between two of them.
     */
    private suspend fun dispatchGestures(service: AccessibilityService, gestures: List<GestureDescription>): Boolean {
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
            append(contentPrefix).append("HeldPointer=${heldPointerManager.heldPointer != null}, " +
                    "owner=${heldPointerManager.heldPointer?.owner}").println()
        }
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
private const val UNBLOCK_GESTURE_DELAY_MS = 10000L

private const val TAG = "GestureExecutor"

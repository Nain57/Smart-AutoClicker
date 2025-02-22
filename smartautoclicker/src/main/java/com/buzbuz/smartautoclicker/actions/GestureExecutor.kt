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
package com.buzbuz.smartautoclicker.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl

import java.io.PrintWriter
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


internal class GestureExecutor : Dumpable {

    private var resultCallback: GestureResultCallback? = null
    private var currentContinuation: Continuation<Boolean>? = null

    private var completedGestures: Long = 0L
    private var cancelledGestures: Long = 0L
    private var errorGestures: Long = 0L


    fun reset() {
        completedGestures = 0L
        cancelledGestures = 0L
        errorGestures = 0L

        resultCallback = null
        currentContinuation = null
    }

    suspend fun dispatchGesture(service: AccessibilityService, gesture: GestureDescription): Boolean {
        if (currentContinuation != null) {
            Log.w(TAG, "Previous gesture result is not available yet, clearing listener to avoid stale events")
            resultCallback = null
            currentContinuation = null
        }

        resultCallback = resultCallback ?: newGestureResultCallback()
        return suspendCoroutine { continuation ->
            currentContinuation = continuation

            try {
                service.dispatchGesture(gesture, resultCallback, null)
            } catch (rEx: RuntimeException) {
                Log.w(TAG, "System is not responsive, the user might be spamming gesture too quickly", rEx)
                errorGestures++
                resumeExecution(gestureError = true)
            }
        }
    }

    private fun resumeExecution(gestureError: Boolean) {
        currentContinuation?.let { continuation ->
            currentContinuation = null

            try {
                continuation.resume(!gestureError)
            } catch (isEx: IllegalStateException) {
                Log.w(TAG, "Continuation have already been resumed. Did the same event got two results ?")
            }
        } ?: Log.w(TAG, "Can't resume continuation. Did the same event got two results ?")
    }

    private fun newGestureResultCallback() = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            completedGestures++
            resumeExecution(gestureError = false)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            cancelledGestures++
            resumeExecution(gestureError = false)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = "${prefix.addDumpTabulationLvl()}- "

        writer.apply {
            append(prefix).println("* GestureExecutor:")
            append(contentPrefix).append("Completed=$completedGestures").println()
            append(contentPrefix).append("Cancelled=$cancelledGestures").println()
            append(contentPrefix).append("Error=$errorGestures").println()
        }
    }
}

private const val TAG = "GestureExecutor"
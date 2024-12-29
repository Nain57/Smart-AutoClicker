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

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.util.Log
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class SuspendableGestureResultCallback : GestureResultCallback() {

    private var continuation: Continuation<Boolean>? = null

    fun withContinuation(continuation: Continuation<Boolean>): SuspendableGestureResultCallback {
        this.continuation = continuation
        return this
    }

    override fun onCompleted(gestureDescription: GestureDescription?) {
        continuation?.resume(true)
    }

    override fun onCancelled(gestureDescription: GestureDescription?) {
        Log.w(TAG, "Gesture cancelled: $gestureDescription")
        continuation?.resume(false)
    }
}

private const val TAG = "SuspendableGestureResultCallback"
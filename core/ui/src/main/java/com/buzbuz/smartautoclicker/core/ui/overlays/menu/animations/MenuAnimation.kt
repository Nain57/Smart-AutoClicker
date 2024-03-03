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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu.animations

import android.util.Log
import android.util.Size

abstract class MenuAnimation {

    private var endListener: (() -> Unit)? = null

    var isRunning: Boolean = false
        private set

    internal fun start(onAnimationEnded: () -> Unit) {
        if (isRunning) return

        Log.d(TAG, "Starting animation $this")
        endListener = onAnimationEnded
        isRunning = true
        onStart()
    }

    internal fun stop() {
        if (!isRunning) return

        Log.d(TAG, "Stopping animation $this")
        onStop()
        isRunning = false
    }

    protected fun end() {
        if (!isRunning) return

        Log.d(TAG, "$this animation ended")
        isRunning = false

        endListener?.invoke()
        endListener = null
    }

    protected abstract fun onStart()
    protected abstract fun onStop()

    internal open fun getContainerMaximumSize(currentMenuSize: Size): Size =
        currentMenuSize
}

/** Tag for logs. */
private const val TAG = "MenuAnimation"
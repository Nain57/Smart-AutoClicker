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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import com.buzbuz.smartautoclicker.core.ui.R

internal class OverlayMenuAnimationsController(
    private val menuLayout: ViewGroup,
    private val menuLayoutParams: WindowManager.LayoutParams,
    private val menuLayoutMaximumSize: Size,
) {

    private val windowManager: WindowManager = menuLayout.context.getSystemService(WindowManager::class.java)
    private val menuBackground: ViewGroup = menuLayout.findViewById(R.id.menu_background)

    private var runningAnimation: MenuAnimation? = null

    fun startAnimation(animation: MenuAnimation, onAnimationEnd: (() -> Unit)?) {
        // Stop any previously requested animation
        runningAnimation?.stop()

        // Set overlay window size to the maximum one, they can't be animated, but they are usually transparent.
        // It will be set back to the content size once the animations are completed.
        updateWindowSize(animation.getContainerMaximumSize(menuLayoutMaximumSize))

        runningAnimation = animation
        animation.start {
            onAnimationEnded(onAnimationEnd)
        }
    }

    fun isAnimationRunning(): Boolean =
        runningAnimation != null

    private fun onAnimationEnded(onAnimationEnd: (() -> Unit)?) {
        runningAnimation = null
        updateWindowSize(Size(menuLayout.measuredWidth, menuLayout.measuredHeight))
        onAnimationEnd?.invoke()
    }

    private fun updateWindowSize(size: Size) {
        menuLayoutParams.width = size.width
        menuLayoutParams.height = size.height

        Log.d(TAG, "Set menu size to $size")
        windowManager.updateViewLayout(menuLayout, menuLayoutParams)
    }
}

/** Tag for logs. */
private const val TAG = "OverlayMenuAnimationsController"
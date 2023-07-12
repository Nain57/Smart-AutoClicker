/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.overlays.BaseOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequest
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequestStack
import com.buzbuz.smartautoclicker.core.ui.utils.internal.LifoStack

import kotlinx.coroutines.flow.StateFlow

import java.io.PrintWriter

class OverlayManager private constructor(context: Context) {

    companion object {

        /** Singleton preventing multiple instances of the OverlayManager at the same time. */
        @Volatile
        private var INSTANCE: OverlayManager? = null

        /**
         * Get the OverlayManager singleton, or instantiates it if it wasn't yet.
         *
         * @return the OverlayManager singleton.
         */
        fun getInstance(context: Context): OverlayManager {
            return INSTANCE ?: synchronized(this) {
                val instance = OverlayManager(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** The metrics of the device screen. */
    private val displayMetrics = DisplayMetrics.getInstance(context)
    /** The listener upon screen rotation. */
    private val orientationListener: (Context) -> Unit = ::onOrientationChanged

    /** Contains all overlays, from root to top visible overlay. */
    private val overlayBackStack: LifoStack<Overlay> = LifoStack()
    /** The stack containing the navigation requests. */
    private val overlayNavigationRequestStack = OverlayNavigationRequestStack()
    /**
     * Keep track of all overlay lifecycle state in the back stack when required.
     * Useful when you need to save the state of the ui, change it and then restore its previous state.
     */
    private val lifecyclesRegistry = LifecycleStatesRegistry()

    private var isNavigating: Boolean = false
    private var closingChildren: Boolean = false
    private var topOverlay: Overlay? = null

    val backStackTop: StateFlow<Overlay?> = overlayBackStack.topFlow

    fun navigateTo(context: Context, newOverlay: BaseOverlay, hideCurrent: Boolean = false) {
        Log.d(TAG, "Pushing NavigateTo request: HideCurrent=$hideCurrent, Overlay=${newOverlay.hashCode()}" +
                    ", currently navigating: $isNavigating")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateTo(newOverlay, hideCurrent))
        if (!isNavigating) executeNextNavigationRequest(context)
    }

    fun navigateUp(context: Context): Boolean {
        if (overlayBackStack.isEmpty()) return false
        Log.d(TAG, "Pushing NavigateUp request, currently navigating: $isNavigating")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateUp)
        if (!isNavigating) executeNextNavigationRequest(context)
        return true
    }

    fun closeAll(context: Context) {
        Log.d(TAG, "Pushing CloseAll request, currently navigating: $isNavigating")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.CloseAll)
        if (!isNavigating) executeNextNavigationRequest(context)
    }

    fun hideAll() {
        // Save the overlays states to restore them when restoreAll is called
        lifecyclesRegistry.saveStates(overlayBackStack.toList())
        // Hide from top to bottom of the stack
        overlayBackStack.forEachReversed { it.hide() }
    }

    fun restoreAll() {
        val overlayStates = lifecyclesRegistry.restoreStates()
        if (overlayBackStack.isEmpty() || overlayStates.isEmpty()) return

        // Restore from bottom to top
        overlayBackStack.forEach { overlay ->
            overlayStates[overlay]?.let { state ->
                when (state) {
                    Lifecycle.State.STARTED -> overlay.start()
                    Lifecycle.State.RESUMED -> overlay.resume()
                    else -> Unit
                }

            } ?: Log.w(TAG, "State for overlay ${overlay.hashCode()} not found, can't restore state")
        }
    }

    fun setTopOverlay(context: Context, overlay: Overlay) {
        if (topOverlay != null) return

        topOverlay = overlay.apply {
            create(
                appContext = context,
                dismissListener = { _, _ -> topOverlay = null },
            )
            resume()
        }
    }

    fun removeTopOverlay() {
        topOverlay?.destroy()
        topOverlay = null
    }

    private fun executeNextNavigationRequest(context: Context) {
        isNavigating = true

        val request = if (overlayNavigationRequestStack.isNotEmpty()) overlayNavigationRequestStack.pop() else null
        Log.d(TAG, "Executing next navigation request $request")

        when (request) {
            is OverlayNavigationRequest.NavigateTo -> executeNavigateTo(context, request)
            is OverlayNavigationRequest.NavigateUp -> executeNavigateUp()
            is OverlayNavigationRequest.CloseAll -> executeCloseAll()
            null -> {
                // If there is no more navigation requests, set the top overlay as current
                if (overlayBackStack.isNotEmpty()) {
                    Log.d(TAG, "No more pending request, resume top overlay")
                    overlayBackStack.peek().resume()
                }

                isNavigating = false
            }
        }
    }

    private fun executeNavigateTo(context: Context, request: OverlayNavigationRequest.NavigateTo) {
        // Get the current top overlay
        val currentOverlay: Overlay? =
            if (overlayBackStack.isEmpty()) {
                // First item ? Start listening for orientation.
                displayMetrics.addOrientationListener(orientationListener)
                null
            } else {
                overlayBackStack.peek()
            }

        // Create the new one and add it to the stack
        request.overlay.create(
            appContext = context,
            dismissListener = ::onOverlayDismissed,
        )
        overlayBackStack.push(request.overlay)

        // Update current lifecycle
        currentOverlay?.apply {
            if (request.hideCurrent) stop()
            else pause()
        }

        executeNextNavigationRequest(context)
    }

    private fun executeNavigateUp() {
        if (overlayBackStack.isEmpty()) return
        overlayBackStack.peek().destroy()
    }

    private fun executeCloseAll() {
        topOverlay?.destroy()

        if (overlayBackStack.isEmpty()) {
            isNavigating = false
            return
        }
        overlayBackStack.bottom?.destroy()
    }

    private fun onOverlayDismissed(context: Context, overlay: Overlay) {
        isNavigating = true

        val dismissedIndex = overlayBackStack.indexOf(overlay)

        // First, close all overlays over the dismissed one, from top to bottom.
        if (dismissedIndex != overlayBackStack.size - 1) {
            Log.d(TAG, "Overlay dismissed isn't at the top of stack, dismissing all children...")

            closingChildren = true
            while (dismissedIndex != overlayBackStack.size - 1) executeNavigateUp()
            closingChildren = false

            Log.d(TAG, "Children all dismissed.")
        }

        // Remove the dismissed overlay from the stack now that we are sure that all children are also destroyed.
        overlayBackStack.pop()

        // Skip if we are currently in the children close loop
        if (closingChildren) return

        // If there is no more overlays, no need to keep track of the orientation
        if (overlayBackStack.isEmpty()) {
            displayMetrics.removeOrientationListener(orientationListener)
        }

        executeNextNavigationRequest(context)
    }

    private fun onOrientationChanged(context: Context) {
        overlayBackStack.forEachReversed { (it as BaseOverlay).changeOrientation() }
    }

    fun dump(writer: PrintWriter, prefix: String) {
        writer.apply {
            println("$prefix * OverlayManager:")
            val contentPrefix = "$prefix\t"
            val itemPrefix = "$contentPrefix\t"

            println("$contentPrefix * Back Stack:")
            overlayBackStack.forEach { overlay -> (overlay as BaseOverlay).dump(this, itemPrefix) }

            println("$contentPrefix * Navigation Request Stack:")
            overlayNavigationRequestStack.dump(this, itemPrefix)
        }
    }
}

/** Tag for logs. */
private const val TAG = "OverlayManager"
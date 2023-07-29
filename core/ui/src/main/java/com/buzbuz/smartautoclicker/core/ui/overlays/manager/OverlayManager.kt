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
import android.graphics.Point
import android.util.Log

import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.overlays.BaseOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.FullscreenOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequest
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequestStack
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.utils.internal.LifoStack

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

import java.io.PrintWriter

/**
 * Manages the overlays navigation and backstack.
 * Can be seen as the equivalent of the Android FragmentManager, but for [Overlay].
 */
class OverlayManager internal constructor(context: Context) {

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
    private val orientationListener: (Context) -> Unit = { onOrientationChanged() }

    /** Contains all overlays, from root to top visible overlay. */
    private val overlayBackStack: LifoStack<Overlay> = LifoStack()
    /** The stack containing the navigation requests. */
    private val overlayNavigationRequestStack = OverlayNavigationRequestStack()
    /**
     * Keep track of all overlay lifecycle state in the back stack when required.
     * Useful when you need to save the state of the ui, change it and then restore its previous state.
     */
    private val lifecyclesRegistry = LifecycleStatesRegistry()

    /** Tells if we are currently executing navigation requests. */
    private var isNavigating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    /** Tells if we are currently closing the children a of dismissed overlay. */
    private var closingChildren: Boolean = false
    /** The overlay at the top of the stack (the top visible one). Null if the stack is empty. */
    private var topOverlay: Overlay? = null
    /**
     * When defined with [lockMenuPosition], all [OverlayMenu] will be displayed at this position with the move button
     * removed. Use [unlockMenuPosition] to restore the user position and allow menu moving.
     */
    private var menuLockedPosition: Point? = null

    /** Flow on the top of the overlay stack. Null if the stack is empty. */
    val backStackTop: Flow<Overlay?> = isNavigating
        .filter { navigating -> !navigating }
        .combine(overlayBackStack.topFlow) { _, stackTop ->
            Log.d(TAG, "New back stack top: $stackTop")
            stackTop
        }
        .distinctUntilChanged()

    /** @return the top of the overlay back stack. */
    fun getBackStackTop(): Overlay? =
        overlayBackStack.top

    /** Display the provided overlay and pause the current one, if any. */
    fun navigateTo(context: Context, newOverlay: Overlay, hideCurrent: Boolean = false) {
        Log.d(TAG, "Pushing NavigateTo request: HideCurrent=$hideCurrent, Overlay=${newOverlay.hashCode()}" +
                    ", currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateTo(newOverlay, hideCurrent))
        if (!isNavigating.value) executeNextNavigationRequest(context)
    }

    /** Destroys the currently shown overlay. */
    fun navigateUp(context: Context): Boolean {
        if (overlayBackStack.isEmpty()) return false
        Log.d(TAG, "Pushing NavigateUp request, currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateUp)
        if (!isNavigating.value) executeNextNavigationRequest(context)
        return true
    }

    /** Destroys all overlays on the back stack. */
    fun closeAll(context: Context) {
        Log.d(TAG, "Pushing CloseAll request, currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.CloseAll)
        if (!isNavigating.value) executeNextNavigationRequest(context)
    }

    /**
     * Hide all overlays on the backstack.
     * Their lifecycles will be saved, and can be restored using [restoreVisibility].
     */
    fun hideAll() {
        if (lifecyclesRegistry.haveStates()) return
        Log.d(TAG, "Hide all overlays from the stack")

        // Save the overlays states to restore them when restoreAll is called
        lifecyclesRegistry.saveStates(overlayBackStack.toList())
        // Hide from top to bottom of the stack
        overlayBackStack.forEachReversed { it.hide() }
    }

    /**
     * Restore the states of all overlays on the backstack.
     * The states must have been saved using [hideAll].
     */
    fun restoreVisibility() {
        val overlayStates = lifecyclesRegistry.restoreStates()
        if (overlayBackStack.isEmpty() || overlayStates.isEmpty()) return

        Log.d(TAG, "Restore overlays visibility")

        // Restore from bottom to top
        overlayBackStack.forEach { overlay ->
            overlayStates[overlay]?.let { state ->
                Log.d(TAG, "Restoring ${overlay.hashCode()} state to $state")

                when (state) {
                    Lifecycle.State.STARTED -> overlay.start()
                    Lifecycle.State.RESUMED -> overlay.resume()
                    else -> Unit
                }

            } ?: Log.w(TAG, "State for overlay ${overlay.hashCode()} not found, can't restore state")
        }
    }

    /**
     * Set an overlay as being shown above all overlays in the backstack.
     * It will not be added to the backstack, and can be seen as "an overlay for overlays".
     */
    fun setTopOverlay(overlay: FullscreenOverlay) {
        if (topOverlay != null) return
        val stackTop = overlayBackStack.top ?: return

        Log.d(TAG, "Create top overlay ${overlay.hashCode()} using context of ${stackTop.hashCode()}")

        topOverlay = overlay.apply {
            create(
                appContext = stackTop.context,
                dismissListener = { _, _ -> topOverlay = null },
            )

            resume()
        }
    }

    /** Remove and destroys the top overlay defined with [setTopOverlay]. */
    fun removeTopOverlay() {
        Log.d(TAG, "Remove top overlay")

        topOverlay?.destroy()
        topOverlay = null
    }

    fun lockMenuPosition(position: Point) {
        Log.d(TAG, "Locking menu position to $position")
        menuLockedPosition = position

        overlayBackStack.forEach { overlay ->
            if (overlay is OverlayMenu) overlay.lockPosition(position)
        }
    }

    fun unlockMenuPosition() {
        Log.d(TAG, "Unlocking menu position")
        menuLockedPosition = null

        overlayBackStack.forEach { overlay ->
            if (overlay is OverlayMenu) overlay.unlockPosition()
        }
    }

    private fun executeNextNavigationRequest(context: Context) {
        isNavigating.value = true

        val request = if (overlayNavigationRequestStack.isNotEmpty()) overlayNavigationRequestStack.pop() else null
        Log.d(TAG, "Executing next navigation request $request")

        when (request) {
            is OverlayNavigationRequest.NavigateTo -> executeNavigateTo(context, request)
            is OverlayNavigationRequest.NavigateUp -> executeNavigateUp()
            is OverlayNavigationRequest.CloseAll -> executeCloseAll()
            null -> {
                // If there is no more navigation requests, set the top overlay as current
                if (overlayBackStack.isNotEmpty()) {
                    Log.d(TAG, "No more pending request, resume stack top overlay")
                    overlayBackStack.peek().resume()
                }

                isNavigating.value = false
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
        // Lock this new overlay position if its a menu and if the lock has been requested
        menuLockedPosition?.let {
            if (request.overlay is OverlayMenu) request.overlay.lockPosition(it)
        }

        // Update current lifecycle
        currentOverlay?.apply {
            pause()
            if (request.hideCurrent) stop()
        }

        request.overlay.start()
        overlayBackStack.push(request.overlay)

        executeNextNavigationRequest(context)
    }

    private fun executeNavigateUp() {
        if (overlayBackStack.isEmpty()) return

        overlayBackStack.peek().apply {
            pause()
            stop()
            destroy()
        }
    }

    private fun executeCloseAll() {
        topOverlay?.destroy()

        if (overlayBackStack.isEmpty()) {
            isNavigating.value = false
            return
        }
        overlayBackStack.bottom?.destroy()
    }

    private fun onOverlayDismissed(context: Context, overlay: Overlay) {
        Log.d(TAG, "Overlay dismissed ${overlay.hashCode()}")

        isNavigating.value = true

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

    private fun onOrientationChanged() {
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
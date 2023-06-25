package com.buzbuz.smartautoclicker.core.ui.overlays.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.overlays.BaseOverlay
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequest
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation.OverlayNavigationRequestStack

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
    protected val displayMetrics = DisplayMetrics.getInstance(context)
    /** The listener upon screen rotation. */
    private val orientationListener: (Context) -> Unit = ::onOrientationChanged

    /** Contains all overlays, from root to top visible overlay. */
    private val overlayBackStack: ArrayDeque<BaseOverlay> = ArrayDeque(emptyList())
    /** The stack containing the navigation requests. */
    private val overlayNavigationRequestStack = OverlayNavigationRequestStack()
    /**
     * Keep track of all overlay lifecycle state in the back stack when required.
     * Useful when you need to save the state of the ui, change it and then restore its previous state.
     */
    private val lifecyclesRegistry = LifecycleStatesRegistry()

    private var isNavigating: Boolean = false
    private var closingChildren: Boolean = false

    fun navigateTo(context: Context, newOverlay: BaseOverlay, hideCurrent: Boolean = false) {
        Log.d(
            TAG, "Pushing NavigateTo request: HideCurrent=$hideCurrent, Overlay=${newOverlay.hashCode()}" +
                    ", currently navigating: $isNavigating"
        )

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
        overlayBackStack.toMutableList().apply {
            // Save the overlays states to restore them when restoreAll is called
            lifecyclesRegistry.saveStates(this)

            // Hide from top to bottom of the stack
            reverse()
            forEach { it.hide() }
        }
    }

    fun restoreAll() {
        val backStack = overlayBackStack.toMutableList()
        val overlayStates = lifecyclesRegistry.restoreStates()

        if (backStack.isEmpty() || overlayStates.isEmpty()) return

        // Restore from bottom to top
        backStack.forEach { overlay ->
            overlayStates[overlay]?.let { state ->
                when (state) {
                    Lifecycle.State.STARTED -> overlay.start()
                    Lifecycle.State.RESUMED -> overlay.resume()
                    else -> Unit
                }

            } ?: Log.w(TAG, "State for overlay ${overlay.hashCode()} not found, can't restore state")
        }
    }

    private fun executeNextNavigationRequest(context: Context) {
        isNavigating = true

        val request = overlayNavigationRequestStack.pop()
        Log.d(TAG, "Executing next navigation request $request")

        when (request) {
            is OverlayNavigationRequest.NavigateTo -> executeNavigateTo(context, request)
            is OverlayNavigationRequest.NavigateUp -> executeNavigateUp()
            is OverlayNavigationRequest.CloseAll -> executeCloseAll()
            null -> {
                // If there is no more navigation requests, set the top overlay as current
                if (!overlayBackStack.isEmpty()) {
                    Log.d(TAG, "No more pending request, resume top overlay")
                    overlayBackStack.last().resume()
                }

                isNavigating = false
            }
        }
    }

    private fun executeNavigateTo(context: Context, request: OverlayNavigationRequest.NavigateTo) {
        // Get the current top overlay
        val currentOverlay: BaseOverlay? =
            if (overlayBackStack.isEmpty()) {
                // First item ? Start listening for orientation.
                displayMetrics.addOrientationListener(orientationListener)
                null
            } else {
                overlayBackStack.last()
            }

        // Create the new one and add it to the stack
        request.overlay.create(
            appContext = context,
            dismissListener = ::onOverlayDismissed,
        )
        overlayBackStack.addLast(request.overlay)

        // Update current lifecycle
        currentOverlay?.apply {
            if (request.hideCurrent) stop()
            else pause()
        }

        executeNextNavigationRequest(context)
    }

    private fun executeNavigateUp() {
        if (overlayBackStack.isEmpty()) return
        overlayBackStack.last().destroy()
    }

    private fun executeCloseAll() {
        if (overlayBackStack.isEmpty()) {
            isNavigating = false
            return
        }
        overlayBackStack.first().destroy()
    }

    private fun onOverlayDismissed(context: Context, overlay: Overlay) {
        isNavigating = true

        val dismissedIndex = overlayBackStack.indexOf(overlay)

        // First, close all overlays over the dismissed one, from top to bottom.
        if (dismissedIndex != overlayBackStack.indices.last) {
            Log.d(TAG, "Overlay dismissed isn't at the top of stack, dismissing all children...")

            closingChildren = true
            while (dismissedIndex != overlayBackStack.indices.last) executeNavigateUp()
            closingChildren = false

            Log.d(TAG, "Children all dismissed.")
        }

        // Remove the dismissed overlay from the stack now that we are sure that all children are also destroyed.
        overlayBackStack.removeLast()

        // Skip if we are currently in the children close loop
        if (closingChildren) return

        // If there is no more overlays, no need to keep track of the orientation
        if (overlayBackStack.isEmpty()) {
            displayMetrics.removeOrientationListener(orientationListener)
        }

        executeNextNavigationRequest(context)
    }

    private fun onOrientationChanged(context: Context) {
        overlayBackStack.toMutableList().apply {
            reverse()
            forEach { overlay -> overlay.changeOrientation() }
        }
    }

    fun dump(writer: PrintWriter, prefix: String) {
        writer.apply {
            println("$prefix * OverlayManager:")
            val contentPrefix = "$prefix\t"
            val itemPrefix = "$contentPrefix\t"

            println("$contentPrefix * Back Stack:")
            overlayBackStack.forEach { overlay -> overlay.dump(this, itemPrefix) }

            println("$contentPrefix * Navigation Request Stack:")
            overlayNavigationRequestStack.dump(this, itemPrefix)
        }
    }
}

/** Tag for logs. */
private const val TAG = "OverlayManager"
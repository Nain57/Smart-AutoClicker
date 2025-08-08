
package com.buzbuz.smartautoclicker.core.common.overlays.manager

import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.common.overlays.base.BaseOverlay
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import com.buzbuz.smartautoclicker.core.common.overlays.manager.navigation.OverlayNavigationRequest
import com.buzbuz.smartautoclicker.core.common.overlays.manager.navigation.OverlayNavigationRequestStack
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import com.buzbuz.smartautoclicker.core.common.overlays.other.FullscreenOverlay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the overlays navigation and backstack.
 * Can be seen as the equivalent of the Android FragmentManager, but for [Overlay].
 */
@Singleton
class OverlayManager @Inject internal constructor(
    private val displayConfigManager: DisplayConfigManager,
    private val menuPositionDataSource: OverlayMenuPositionDataSource,
): Dumpable {

    companion object {
        /** The type of window used for the overlays. */
        const val OVERLAY_WINDOW_TYPE: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        fun AlertDialog.showAsOverlay() {
            window?.setType(OVERLAY_WINDOW_TYPE)
            show()
        }
    }


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
    private var isNavigating: MutableStateFlow<OverlayNavigationRequest?> = MutableStateFlow(null)
    /** The overlay at the top of the stack (the top visible one). Null if the stack is empty. */
    private var topOverlay: Overlay? = null
    /** Notifies the caller of [navigateUpToRoot] once the all overlays above the root are destroyed. */
    private var navigateUpToRootCompletionListener: (() -> Unit)? = null

    var onVisibilityChangedListener: (() -> Unit)? = null

    /** Flow on the top of the overlay stack. Null if the stack is empty. */
    val backStackTop: Flow<Overlay?> = isNavigating
        .filter { navigating -> navigating == null }
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
        Log.d(
            TAG, "Pushing NavigateTo request: HideCurrent=$hideCurrent, Overlay=${newOverlay.hashCode()}" +
                    ", currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateTo(newOverlay, hideCurrent))
        if (isNavigating.value == null) executeNextNavigationRequest(context)
    }

    /** Destroys the currently shown overlay. */
    fun navigateUp(context: Context): Boolean {
        if (overlayBackStack.isEmpty()) return false
        Log.d(TAG, "Pushing NavigateUp request, currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateUp)
        if (isNavigating.value == null) executeNextNavigationRequest(context)
        return true
    }

    /** Destroys all overlays in the backstack except the root one. */
    fun navigateUpToRoot(context: Context, completionListener: (() -> Unit)? = null) {
        if (overlayBackStack.size <= 1) {
            completionListener?.invoke()
            return
        }

        navigateUpToRootCompletionListener = completionListener
        var navigateUpCount = overlayNavigationRequestStack.toList().fold(overlayBackStack.size - 1) { acc, overlayNavigationRequest ->
            acc + if (overlayNavigationRequest is OverlayNavigationRequest.NavigateTo) 1 else -1
        }
        when (isNavigating.value) {
            is OverlayNavigationRequest.NavigateTo -> navigateUpCount+= 1
            is OverlayNavigationRequest.NavigateUp -> navigateUpCount-= 1
            else -> Unit
        }
        Log.d(TAG, "Navigating to root, pushing $navigateUpCount NavigateUp requests, currently navigating: ${isNavigating.value}")

        repeat(navigateUpCount) {
            overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateUp)
        }
        if (isNavigating.value  == null) executeNextNavigationRequest(context)
    }

    /** Destroys all overlays on the back stack. */
    fun closeAll(context: Context) {
        if (topOverlay == null && overlayBackStack.isEmpty()) return

        Log.d(TAG, "Close all overlays (${overlayBackStack.size}, currently navigating: ${isNavigating.value}")

        overlayNavigationRequestStack.clear()
        lifecyclesRegistry.clearStates()
        topOverlay?.destroy()
        repeat(overlayBackStack.size) {
            overlayNavigationRequestStack.push(OverlayNavigationRequest.NavigateUp)
        }
        if (isNavigating.value == null) executeNextNavigationRequest(context)
    }

    /** Propagate the provided touch event to the focused overlay, if any. */
    fun propagateKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "Propagating key event $event")

        return topOverlay?.handleKeyEvent(event)
            ?: overlayBackStack.top?.handleKeyEvent(event)
            ?: false
    }

    /**
     * Hide all overlays on the backstack.
     * Their lifecycles will be saved, and can be restored using [restoreVisibility].
     */
    fun hideAll() {
        if (isStackHidden()) return

        Log.d(TAG, "Hide all overlays from the stack")

        // Save the overlays states to restore them when restoreAll is called
        lifecyclesRegistry.saveStates(overlayBackStack.toList())
        // Hide from top to bottom of the stack
        overlayBackStack.forEachReversed { it.hide() }

        onVisibilityChangedListener?.invoke()
    }

    /**
     * Restore the states of all overlays on the backstack.
     * The states must have been saved using [hideAll].
     */
    fun restoreVisibility() {
        if (!isStackHidden()) return

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

        onVisibilityChangedListener?.invoke()
    }

    /** @return true if the overlay stack has been hidden via [hideAll], false if not. */
    fun isStackHidden(): Boolean =
        lifecyclesRegistry.haveStates()

    fun isOverlayStackVisible(): Boolean =
        getBackStackTop()?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false

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
        menuPositionDataSource.lockPosition(position)
    }

    fun unlockMenuPosition() {
        Log.d(TAG, "Unlocking menu position")
        menuPositionDataSource.unlockPosition()
    }

    private fun executeNextNavigationRequest(context: Context) {
        val request = if (overlayNavigationRequestStack.isNotEmpty()) overlayNavigationRequestStack.pop() else null
        isNavigating.value = request
        Log.d(TAG, "Executing next navigation request $request")

        when (request) {
            is OverlayNavigationRequest.NavigateTo -> executeNavigateTo(context, request)
            OverlayNavigationRequest.NavigateUp -> executeNavigateUp()
            null -> onNavigationCompleted()
        }
    }

    private fun executeNavigateTo(context: Context, request: OverlayNavigationRequest.NavigateTo) {
        // Get the current top overlay
        val currentOverlay: Overlay? =
            if (overlayBackStack.isEmpty()) {
                // First item ? Start listening for orientation.
                displayConfigManager.addOrientationListener(orientationListener)
                null
            } else {
                overlayBackStack.peek()
            }

        // Create the new one and add it to the stack
        request.overlay.create(
            appContext = context,
            dismissListener = ::onOverlayDismissed,
        )

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

    private fun onOverlayDismissed(context: Context, overlay: Overlay) {
        Log.d(TAG, "Overlay dismissed ${overlay.hashCode()}")

        // Remove the dismissed overlay from the stack now that we are sure it is destroyed.
        overlayBackStack.pop()

        // If there is no more overlays, no need to keep track of the orientation
        if (overlayBackStack.isEmpty()) {
            displayConfigManager.removeOrientationListener(orientationListener)
        }

        executeNextNavigationRequest(context)
    }

    private fun onNavigationCompleted() {
        // If there is no more navigation requests, resume the top overlay
        if (overlayBackStack.isNotEmpty()) {
            // If the overlay stack was requested hidden, do nothing
            if (!isStackHidden()) {
                Log.d(TAG, "No more pending request, resume stack top overlay")
                overlayBackStack.peek().resume()
            } else {
                Log.d(TAG, "No more pending request, but stack is hidden, delaying resume...")
            }

            navigateUpToRootCompletionListener?.invoke()
            navigateUpToRootCompletionListener = null
        }

        isNavigating.value = null
    }

    private fun onOrientationChanged() {
        overlayBackStack.forEachReversed { it.changeOrientation() }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()
        val itemPrefix = contentPrefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* OverlayManager:")

            overlayNavigationRequestStack.dump(this, contentPrefix)

            append(contentPrefix).println("- BackStack:")
            overlayBackStack.forEach { overlay -> (overlay as BaseOverlay).dump(this, itemPrefix) }
        }
    }
}

/** Tag for logs. */
private const val TAG = "OverlayManager"
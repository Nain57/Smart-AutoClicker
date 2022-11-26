/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.baseui

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.State
import java.io.PrintWriter


/**
 * Base class for an overlay based ui providing lifecycle and back stack management.
 *
 * Initialization starts with the [create] method, which will call the correct implementation methods to creates and
 * show the overlay ui object.
 * Back stack management is ensured by the method [showSubOverlay], resuming the parent ui object once the child is
 * dismissed.
 */
abstract class OverlayController internal constructor(
    appContext: Context,
    private val theme: Int? = null,
    private val recreateOnRotation: Boolean = false,
) : LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    /** The lifecycle of the ui component controlled by this class */
    private var lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    /** The store for the view models of the [OverlayController] implementations. */
    private val modelStore: ViewModelStore by lazy { ViewModelStore() }
    override fun getViewModelStore(): ViewModelStore = modelStore
    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory =
        ViewModelProvider.AndroidViewModelFactory.getInstance((context.applicationContext as Application))

    /** The metrics of the device screen. */
    protected val screenMetrics = ScreenMetrics.getInstance(appContext)
    /** The listener upon screen rotation. */
    private val orientationListener: (Context) -> Unit = ::onOrientationChanged

    /** The context for this overlay. See [newOverlayContext]. */
    protected var context: Context = newOverlayContext(appContext)

    /**
     * OverlayController for an overlay shown from this OverlayController using [showSubOverlay].
     * Null if none has been shown, or if a previous sub OverlayController has been dismissed.
     */
    private var subOverlayController: OverlayController? = null
    /**
     * Listener called when the overlay shown by the controller is dismissed.
     * Null unless the overlay is shown.
     */
    private var onDestroyListener: (() -> Unit)? = null

    /**
     * Call to [showSubOverlay] that has been made while hidden.
     * It will be executed once [show] is called.
     */
    private var pendingSubOverlayRequest: Pair<OverlayController, Boolean>? = null

    /** True if the overlay should be recreated on next [show] call, false if not. */
    private var shouldBeRecreated: Boolean = false

    /** Creates the ui object to be shown. */
    protected open fun onCreate() = Unit
    /** Show the ui object to the user. */
    protected open fun onStart() = Unit
    /** The ui object is in foreground. */
    protected open fun onResume() = Unit
    /** The ui object is visible but no in foreground. */
    protected open fun onPause() = Unit
    /** Hide the ui object from the user. */
    protected open fun onStop() = Unit
    /** Destroys the ui object. */
    protected open fun onDestroyed() = Unit
    /** The device screen orientation have changed. */
    protected open fun onOrientationChanged() = Unit

    /**
     * Creates and show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     *
     * @param dismissListener object notified upon the shown ui dismissing.
     */
    fun create(dismissListener: (() -> Unit)? = null) {
        if (lifecycleRegistry.currentState == State.DESTROYED) lifecycleRegistry.currentState = State.INITIALIZED
        if (lifecycleRegistry.currentState != State.INITIALIZED) return

        Log.d(TAG, "create overlay ${hashCode()}")
        onDestroyListener = dismissListener
        screenMetrics.addOrientationListener(orientationListener)

        onCreate()
        lifecycleRegistry.currentState = State.CREATED
    }

    /**
     * Show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    fun show() {
        if (lifecycleRegistry.currentState != State.CREATED) return

        if (shouldBeRecreated) {
            recreate(true)
            return
        }

        Log.d(TAG, "show overlay ${hashCode()}")

        onStart()
        lifecycleRegistry.currentState = State.STARTED

        // If there is no sub overlay, this one is in foreground.
        if (subOverlayController == null) {
            onResume()
            lifecycleRegistry.currentState = State.RESUMED
        }
    }

    /**
     * Hide the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    fun hide() {
        if (!lifecycleRegistry.currentState.isAtLeast(State.STARTED)) return

        if (lifecycleRegistry.currentState == State.RESUMED) {
            lifecycleRegistry.currentState = State.STARTED
            onPause()
        }

        Log.d(TAG, "hide overlay ${hashCode()}")
        lifecycleRegistry.currentState = State.CREATED
        onStop()
    }

    /**
     * Dismiss the ui object. If not hidden, hide it first.
     * If the lifecycle doesn't allows it, does nothing.
     */
    @CallSuper
    open fun destroy() {
        if (!lifecycleRegistry.currentState.isAtLeast(State.CREATED)) return
        if (lifecycleRegistry.currentState.isAtLeast(State.STARTED)) hide()

        Log.d(TAG, "destroy overlay ${hashCode()}")

        lifecycleRegistry.currentState = State.DESTROYED
        if (!shouldBeRecreated) {
            subOverlayController?.destroy()
        }

        screenMetrics.removeOrientationListener(orientationListener)
        onDestroyed()

        if (!shouldBeRecreated) {
            onDestroyListener?.invoke()
            onDestroyListener = null
        }
    }

    /**
     * Destroy and create the overlay. once again.
     * The [onDestroyListener] will not be called during the process and the [subOverlayController] (if any) will not
     * be destroyed.
     *
     * @param show true if the overlay should be immediately show after recreation, false if not.
     */
    private fun recreate(show: Boolean) {
        if (!lifecycleRegistry.currentState.isAtLeast(State.CREATED)) return

        Log.d(TAG, "recreating overlay ${hashCode()}")

        destroy()
        shouldBeRecreated = false

        context = newOverlayContext(context.applicationContext)
        lifecycleRegistry = LifecycleRegistry(this)
        create(onDestroyListener)
        if (show) show()
    }

    /**
     * Update the overlay orientation.
     *
     * In order to avoid recreating the whole overlay tree on each screen rotation (which can be heavy if the user is
     * deep in it), the following behaviour is applied:
     * - If [recreateOnRotation] is true and the lifecycle at least [Lifecycle.State.STARTED]: the overlay is visible
     * and thus, must be recreated => Destroy and Create the overlay again.
     * - If [recreateOnRotation] is but the lifecycle is below [Lifecycle.State.STARTED]: the overlay is created but
     * not hidden => Flag the overlay for recreation and delay it until next show call.
     *
     * In all cases, [onOrientationChanged] will be called to notify this [OverlayController] implementation for
     * rotation.
     */
    private fun onOrientationChanged(context: Context) {
        Log.d(TAG, "onOrientationChanged for overlay ${hashCode()}")
        onOrientationChanged()

        if (!recreateOnRotation) return

        shouldBeRecreated = true
        if (lifecycleRegistry.currentState.isAtLeast(State.STARTED)) {
            recreate(true)
        } else {
            Log.d(TAG, "not visible, delay recreation of overlay ${hashCode()}")
        }
    }

    /**
     * Creates and show another overlay managed by a OverlayController from this dialog.
     *
     * Using this method instead of directly calling [create] and [show] on the new OverlayController will allow to keep
     * a back stack of OverlayController, allowing to resume the current overlay once the new overlay is dismissed.
     *
     * @param overlayController the controller of the new overlay to be shown.
     * @param hideCurrent true to hide the current overlay, false to display the new overlay over it.
     */
    @CallSuper
    open fun showSubOverlay(overlayController: OverlayController, hideCurrent: Boolean = false) {
        if (!lifecycleRegistry.currentState.isAtLeast(State.CREATED)) {
            Log.e(TAG, "Can't show ${overlayController.hashCode()}, parent ${hashCode()} is not created")
            return
        }

        if (lifecycleRegistry.currentState < State.RESUMED) {
            Log.i(TAG, "Delaying sub overlay: ${overlayController.hashCode()}; hide=$hideCurrent; parent=${hashCode()}")
            pendingSubOverlayRequest = overlayController to hideCurrent
            return
        }

        Log.d(TAG, "show sub overlay: ${overlayController.hashCode()}; hide=$hideCurrent; parent=${hashCode()}")

        subOverlayController = overlayController
        overlayController.create(dismissListener = { onSubOverlayDismissed(overlayController) })

        if (hideCurrent) {
            hide()
        } else {
            lifecycleRegistry.currentState = State.STARTED
            onPause()
        }

        overlayController.show()
    }

    /**
     * Listener upon the closing of a overlay opened with [showSubOverlay].
     *
     * @param dismissedOverlay the sub overlay dismissed.
     */
    private fun onSubOverlayDismissed(dismissedOverlay: OverlayController) {
        Log.d(TAG, "sub overlay dismissed: ${dismissedOverlay.hashCode()}; parent=${hashCode()}")

        if (dismissedOverlay == subOverlayController) {
            subOverlayController = null

            when (lifecycleRegistry.currentState) {
                State.DESTROYED -> return
                State.CREATED -> show()
                State.STARTED -> {
                    onResume()
                    lifecycleRegistry.currentState = State.RESUMED
                }
                else -> {}
            }

            if (pendingSubOverlayRequest != null) {
                showSubOverlay(pendingSubOverlayRequest!!.first, pendingSubOverlayRequest!!.second)
                pendingSubOverlayRequest = null
            }
        }
    }

    /**
     * Get a new context wrapper from the provided theme. If the theme is null, the application theme is used.
     *
     * This is required because an overlay can be attached to a context without UI configuration changes notification,
     * which can leads to an invalid theming for the dialog, an invalid rotation ...
     *
     * @param appContext the Android application context.
     */
    private fun newOverlayContext(appContext: Context): Context =
        if (theme != null) newOverlayContextThemeWrapper(appContext, theme, screenMetrics.orientation)
        else appContext

    /**
     * Dump the state of this overlay controller into the provided writer.
     *
     * @param writer the writer to dump into.
     * @param prefix the prefix to start each line with.
     */
    fun dump(writer: PrintWriter, prefix: String) {
        writer.apply {
            println("$prefix * ${this@OverlayController.toDumpString()}:")

            val contentPrefix = "$prefix\t"
            println("$contentPrefix Lifecycle: ${lifecycleRegistry.currentState}")
            if (recreateOnRotation) println("$contentPrefix\t - shouldBeRecreated")

            println("$contentPrefix SubOverlay: ${subOverlayController?.toDumpString()}")

            subOverlayController?.dump(writer, prefix)
        }
    }

    /** @return the dump representation of this OverlayController. */
    private fun toDumpString() = "${javaClass.simpleName}@${hashCode()}"
}

/** Tag for logs. */
private const val TAG = "OverlayController"
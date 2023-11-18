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
package com.buzbuz.smartautoclicker.core.ui.overlays

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.KeyEvent

import androidx.annotation.CallSuper
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.lifecycleScope

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager

import com.google.android.material.color.DynamicColors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.io.PrintWriter

/**
 * Base class for an overlay based ui providing lifecycle management.
 *
 * Initialization starts with the [create] method, which will call the correct implementation methods to creates and
 * show the overlay ui object.
 */
abstract class BaseOverlay internal constructor(
    private val theme: Int? = null,
    private val recreateOnRotation: Boolean = false,
) : Overlay() {

    /** The context for this overlay. */
    override lateinit var context: Context
    /** The metrics of the device screen. */
    protected val displayMetrics: DisplayMetrics by lazy {
        DisplayMetrics.getInstance(context)
    }

    /** The lifecycle of the ui component controlled by this class */
    internal var lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /** The store for the view models of the [BaseOverlay] implementations. */
    private val modelStore: ViewModelStore by lazy { ViewModelStore() }
    override val viewModelStore: ViewModelStore
        get() = modelStore
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance((context.applicationContext as Application))
    }

    /** Job used for debouncing the user interactions. */
    private var debounceUserInteractionJob: Job? = null

    /**
     * Listener called when the overlay shown by the controller is dismissed.
     * Null unless the overlay is shown.
     */
    private var onDestroyListener: (() -> Unit)? = null

    /** True if the overlay should be recreated on next [start] call, false if not. */
    private var shouldBeRecreated: Boolean = false

    override fun show() {
        resume()
    }

    override fun hide() {
        stop()
    }

    override fun back() {
        if (!OverlayManager.getInstance(context).navigateUp(context)) {
            Log.w(TAG, "Overlay ${hashCode()} can't be removed from back stack, destroying manually...")
            destroy()
        }
    }

    override fun finish() {
        destroy()
    }

    /**
     * Creates and show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     *
     * @param appContext the application context.
     * @param dismissListener object notified upon the shown ui dismissing.
     */
    override fun create(appContext: Context, dismissListener: ((Context, Overlay) -> Unit)?) {
        if (lifecycleRegistry.currentState == State.DESTROYED) lifecycleRegistry.currentState = State.INITIALIZED
        if (lifecycleRegistry.currentState != State.INITIALIZED) return

        Log.d(TAG, "create overlay ${hashCode()}")
        context = newOverlayContext(appContext)
        dismissListener?.let { listener -> onDestroyListener = { listener(appContext, this@BaseOverlay) } }

        onCreate()
        lifecycleRegistry.currentState = State.CREATED
    }

    /**
     * Show the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    override fun start() {
        if (lifecycleRegistry.currentState != State.CREATED) return

        // During the orientation change, this overlay was hidden (in CREATED state). As it was not displayed, the
        // recreation of the overlay was delayed to its next start request.
        if (shouldBeRecreated) {
            recreate()
        }

        Log.d(TAG, "show overlay ${hashCode()}")

        onStart()
        lifecycleRegistry.currentState = State.STARTED
    }

    override fun resume() {
        if (lifecycleRegistry.currentState == State.CREATED) start()
        if (lifecycleRegistry.currentState != State.STARTED) return

        Log.d(TAG, "resume overlay ${hashCode()}")
        onResume()
        lifecycleRegistry.currentState = State.RESUMED
    }

    override fun pause() {
        if (lifecycleRegistry.currentState != State.RESUMED) return

        Log.d(TAG, "pause overlay ${hashCode()}")
        lifecycleRegistry.currentState = State.STARTED
        onPause()
    }

    /**
     * Hide the ui object.
     * If the lifecycle doesn't allows it, does nothing.
     */
    override fun stop() {
        if (!lifecycleRegistry.currentState.isAtLeast(State.STARTED)) return
        if (lifecycleRegistry.currentState.isAtLeast(State.RESUMED)) pause()

        Log.d(TAG, "hide overlay ${hashCode()}")
        lifecycleRegistry.currentState = State.CREATED
        onStop()
    }

    /**
     * Dismiss the ui object. If not hidden, hide it first.
     * If the lifecycle doesn't allows it, does nothing.
     */
    @CallSuper
    override fun destroy() {
        if (!lifecycleRegistry.currentState.isAtLeast(State.CREATED)) return
        if (lifecycleRegistry.currentState.isAtLeast(State.STARTED)) stop()

        Log.d(TAG, "destroy overlay ${hashCode()}")

        lifecycleRegistry.currentState = State.DESTROYED
        onDestroy()

        if (!shouldBeRecreated) {
            debounceUserInteractionJob?.cancel()
            debounceUserInteractionJob = null

            onDestroyListener?.invoke()
            onDestroyListener = null

            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                modelStore.clear()
                cancel()
            }
        }
    }

    protected fun debounceUserInteraction(userInteraction: () -> Unit) {
        if (debounceUserInteractionJob == null && lifecycleRegistry.currentState == State.RESUMED) {
            debounceUserInteractionJob = lifecycleScope.launch {
                userInteraction()
                delay(800)
                debounceUserInteractionJob = null
            }
        }
    }

    /**
     * Destroy and create the overlay. once again.
     * The [onDestroyListener] will not be called during the process.
     */
    private fun recreate() {
        if (!lifecycleRegistry.currentState.isAtLeast(State.CREATED)) return

        Log.d(TAG, "recreating overlay ${hashCode()}")

        destroy()
        shouldBeRecreated = false

        lifecycleRegistry = LifecycleRegistry(this)
        create(context)
    }

    /**
     * Update the overlay orientation.
     *
     * In order to avoid recreating the whole overlay tree on each screen rotation (which can be heavy if the user is
     * deep in it), the following behaviour is applied:
     * - If [recreateOnRotation] is true and the lifecycle at least [Lifecycle.State.STARTED]: the overlay is visible
     * and thus, must be recreated => Destroy and Create the overlay again.
     * - If [recreateOnRotation] is but the lifecycle is below [Lifecycle.State.STARTED]: the overlay is created but
     * hidden => Flag the overlay for recreation and delay it until next show call.
     *
     * In all cases, [onOrientationChanged] will be called to notify this [BaseOverlay] implementation for
     * rotation.
     */
    override fun changeOrientation() {
        Log.d(TAG, "onOrientationChanged for overlay ${hashCode()}")

        onOrientationChanged()

        if (!recreateOnRotation) return

        shouldBeRecreated = true
        val preRotationLifecycleState = lifecycleRegistry.currentState
        if (preRotationLifecycleState.isAtLeast(State.STARTED)) {
            recreate()
            start()
            if (preRotationLifecycleState == State.RESUMED) resume()
        } else {
            Log.d(TAG, "not visible, delay recreation of overlay ${hashCode()}")
        }
    }

    override fun handleKeyEvent(keyEvent: KeyEvent): Boolean =
        onKeyEvent(keyEvent)

    /**
     * Get a new context wrapper from the provided theme. If the theme is null, the application theme is used.
     *
     * This is required because an overlay can be attached to a context without UI configuration changes notification,
     * which can leads to an invalid theming for the dialog, an invalid rotation ...
     *
     * @param appContext the Android application context.
     */
    private fun newOverlayContext(appContext: Context): Context =
        if (theme == null) appContext
        else DynamicColors.wrapContextIfAvailable(
            ContextThemeWrapper(appContext, theme).apply {
                applyOverrideConfiguration(
                    Configuration(applicationContext.resources.configuration).apply {
                        orientation = DisplayMetrics.getInstance(appContext).orientation
                    }
                )
            }
        )

    /**
     * Dump the state of this overlay controller into the provided writer.
     *
     * @param writer the writer to dump into.
     * @param prefix the prefix to start each line with.
     */
    fun dump(writer: PrintWriter, prefix: String) {
        writer.apply {
            println("$prefix * ${this@BaseOverlay.toDumpString()}:")

            val contentPrefix = "$prefix\t"
            println("$contentPrefix Lifecycle: ${lifecycleRegistry.currentState}")
            if (recreateOnRotation) println("$contentPrefix\t - shouldBeRecreated")
        }
    }

    /** @return the dump representation of this OverlayController. */
    private fun toDumpString() = "${javaClass.simpleName}@${hashCode()}"
}

inline fun <reified VM : ViewModel> BaseOverlay.viewModels(): Lazy<VM> =
    ViewModelLazy(
        VM::class,
        { viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras },
    )

/** Tag for logs. */
private const val TAG = "BaseOverlay"
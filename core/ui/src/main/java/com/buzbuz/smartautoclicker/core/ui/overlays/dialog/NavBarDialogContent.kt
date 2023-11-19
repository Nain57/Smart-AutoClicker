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
package com.buzbuz.smartautoclicker.core.ui.overlays.dialog

import android.app.Application
import android.content.Context
import android.view.ViewGroup

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton

abstract class NavBarDialogContent(
    appContext: Context,
) : LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    /** The lifecycle of the ui component controlled by this class */
    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    override val lifecycle: Lifecycle = lifecycleRegistry

    /** The store for the view models of the [NavBarDialogContent] implementations. */
    private val modelStore: ViewModelStore by lazy { ViewModelStore() }
    override val viewModelStore: ViewModelStore
        get() = modelStore

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance((appContext as Application))
    }

    /** The container for the dialog content. */
    private lateinit var rootContainer: ViewGroup
    /** The root view of the content. Provided by the implementation via [onCreateView]. */
    private lateinit var root: ViewGroup

    /** The owner of the dialog. */
    lateinit var dialogController: NavBarDialog
    /** The identifier of this content in the navigation bar. */
    protected var navBarId: Int = -1
        private set
    /** The Android context. */
    protected val context: Context
        get() = rootContainer.context

    /**
     * Creates the content.
     * The views will be inflated, but not attached nor shown yet.
     *
     * @param controller the owner of the dialog.
     * @param container the container view for this content.
     * @param identifier the identifier of this content in the parent navigation bar.
     */
    fun create(controller: NavBarDialog, container: ViewGroup, identifier: Int) {
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) return

        navBarId = identifier
        dialogController = controller
        rootContainer = container
        root = onCreateView(container)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        onViewCreated()
    }

    /**
     * Attach this content to the container.
     * The content is the current one, but the dialog is hidden.
     */
    fun start() {
        if (lifecycleRegistry.currentState != Lifecycle.State.CREATED) return

        rootContainer.addView(root)

        if (createCopyButtonsAreAvailable()) {
            dialogController.createCopyButtons.apply {
                buttonNew.setOnClickListener { onCreateButtonClicked() }
                buttonCopy.setOnClickListener { onCopyButtonClicked() }
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        onStart()
    }

    /** The content is visible. */
    fun resume() {
        if (lifecycleRegistry.currentState != Lifecycle.State.STARTED) return
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** The dialog has been hidden. */
    fun pause() {
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) return
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    /**
     * Detach this content from the container.
     * The content is no longer the selected one.
     */
    fun stop() {
        if (lifecycleRegistry.currentState != Lifecycle.State.STARTED) return

        onStop()
        rootContainer.removeView(root)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /**
     * Destroy this content.
     * The dialog has been destroyed. Once this method is called, this content can no longer be used.
     */
    fun destroy() {
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) return

        pause()
        stop()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }

    protected fun debounceUserInteraction(userInteraction: () -> Unit) {
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            dialogController.debounceInteraction(userInteraction)
        }
    }

    protected abstract fun onCreateView(container: ViewGroup): ViewGroup

    protected abstract fun onViewCreated()

    protected open fun onStart() = Unit

    protected open fun onStop() = Unit

    open fun onDialogButtonClicked(buttonType: DialogNavigationButton) = Unit

    protected open fun onCreateButtonClicked() = Unit

    protected open fun onCopyButtonClicked() = Unit

    open fun createCopyButtonsAreAvailable(): Boolean = false
}

inline fun <reified VM : ViewModel> NavBarDialogContent.viewModels(): Lazy<VM> =
    ViewModelLazy(
        VM::class,
        { viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras },
    )

inline fun <reified VM : ViewModel> NavBarDialogContent.dialogViewModels(): Lazy<VM> =
    ViewModelLazy(
        VM::class,
        { dialogController.viewModelStore },
        { dialogController.defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras },
    )
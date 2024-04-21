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

import android.content.Context
import android.view.KeyEvent

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner

abstract class Overlay : LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    /** The context for this overlay. */
    abstract var context: Context

    /**
     * Make the overlay visible again after calling [hide].
     * If the overlay is already visible, does nothing.
     */
    abstract fun show()

    /**
     * Make the overlay invisible.
     * If the overlay is already invisible, does nothing. After a call to [hide], lifecycle will be set to CREATED.
     */
    abstract fun hide()

    /** Destroy the Overlay and returns to the previous one in the back stack. */
    abstract fun back()

    /** Destroy the Overlay. */
    abstract fun finish()

    /** Called once the overlay is created. */
    protected open fun onCreate() = Unit
    /** Called once the overlay is visible to the user. */
    protected open fun onStart() = Unit
    /** Called once the overlay is in foreground (the top of the back stack). */
    protected open fun onResume() = Unit
    /** Called once another overlay is shown in front of this overlay while it is still shown. */
    protected open fun onPause() = Unit
    /** Called once the overlay is no longer visible to the user. */
    protected open fun onStop() = Unit
    /** Called once the overlay is destroyed and no longer used. */
    protected open fun onDestroy() = Unit

    /** The device screen orientation have changed. */
    protected open fun onOrientationChanged() = Unit

    /**
     * Callback that allows an overlay to observe the key events before they are passed to the rest
     * of the system. This means that the events are first delivered here before they are passed to
     * the device policy, the input method, or applications.
     *
     * Note: It is important that key events are handled in such a way that the event stream that
     * would be passed to the rest of the system is well-formed. For example, handling the down
     * event but not the up event and vice versa would generate an inconsistent event stream.
     *
     * Note:The key events delivered in this method are copies and modifying them will have no
     * effect on the events that will be passed to the system. This method is intended to perform
     * purely filtering functionality.
     *
     * @param keyEvent The event to be processed. This event is owned by the caller and cannot be used
     * after this method returns.
     * @return If true then the event will be consumed and not delivered to applications, otherwise
     *         it will be delivered as usual.
     */
    protected open fun onKeyEvent(keyEvent: KeyEvent): Boolean = false

    /** Creates the ui object to be shown. */
    internal abstract fun create(appContext: Context, dismissListener: ((Context, Overlay) -> Unit)? = null)
    /** Show the ui object to the user. */
    internal abstract fun start()
    /** The ui object is in foreground. */
    internal abstract fun resume()
    /** The ui object is visible but no in foreground. */
    internal abstract fun pause()
    /** Hide the ui object from the user. */
    internal abstract fun stop()
    /** Destroys the ui object. */
    internal abstract fun destroy()

    /** Handles the provided KeyEvent. Return true if handled, false if not. */
    internal abstract fun handleKeyEvent(keyEvent: KeyEvent): Boolean
    /** Change the orientation of the overlay. */
    internal abstract fun changeOrientation()
}
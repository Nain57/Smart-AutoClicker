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
}
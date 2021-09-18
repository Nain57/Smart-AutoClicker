/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui

import android.content.Context

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/** Base class for a "ViewModel" for an overlay. */
abstract class OverlayViewModel(protected val context: Context): LifecycleObserver {

    /** The lifecycle owner (the overlay). Defined with [attachToLifecycle]. */
    private var lifecycleOwner: LifecycleOwner? = null

    /** The scope for all coroutines executed by this model. */
    protected val viewModelScope = CoroutineScope(Job())

    /**
     * Attach the view model to a lifecycle.
     * @param lifecycleOwner the owner of the lifecycle to attach to.
     */
    fun attachToLifecycle(lifecycleOwner: LifecycleOwner) {
        if (this.lifecycleOwner != null) {
            throw IllegalStateException("Model is already attached to ${this.lifecycleOwner}")
        }

        this.lifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Called when the lifecycle owner is destroyed.
     * Remove the reference on the owner and cancel the [viewModelScope].
     */
    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onCleared() {
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = null
        viewModelScope.cancel()
    }
}
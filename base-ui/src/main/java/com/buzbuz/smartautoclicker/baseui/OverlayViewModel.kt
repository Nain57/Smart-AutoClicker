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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*

/** Base class for a "ViewModel" for an overlay. */
abstract class OverlayViewModel(protected val context: Context): DefaultLifecycleObserver {

    /** Tells if this view model is attached to a lifecycle. */
    private var isAttachedToLifecycle: Boolean = false

    /** The scope for all coroutines executed by this model. */
    protected val viewModelScope = CoroutineScope(Job())

    /**
     * Attach the view model to a lifecycle.
     * @param owner the owner of the lifecycle to attach to.
     */
    fun attachToLifecycle(owner: LifecycleOwner) {
        if (isAttachedToLifecycle) {
            throw IllegalStateException("Model is already attached to a lifecycle owner")
        }

        isAttachedToLifecycle = true
        owner.lifecycle.addObserver(this)
    }

    final override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        onCleared()

        owner.lifecycle.removeObserver(this)
        viewModelScope.launch {
            delay(5_000)
            viewModelScope.cancel()
        }
        isAttachedToLifecycle = false
    }

    /**
     * Called when the lifecycle owner is destroyed.
     * Override to clear any resources associated with this view model.
     */
    open fun onCleared() {}
}
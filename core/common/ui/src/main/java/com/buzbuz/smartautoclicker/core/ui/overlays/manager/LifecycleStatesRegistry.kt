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

import androidx.lifecycle.Lifecycle
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu

internal class LifecycleStatesRegistry {

    private val overlaysLifecycleState = mutableMapOf<Overlay, Lifecycle.State>()

    fun saveStates(overlays: List<Overlay>) {
        overlaysLifecycleState.clear()
        overlays.forEach { overlay ->
            val state = when {
                overlay is OverlayMenu && overlay.resumeOnceShown -> Lifecycle.State.RESUMED
                overlay is OverlayMenu && overlay.destroyOnceHidden -> Lifecycle.State.DESTROYED
                else -> overlay.lifecycle.currentState
            }

            overlaysLifecycleState[overlay] = state
        }
    }

    fun restoreStates(): Map<Overlay, Lifecycle.State> {
        val states = overlaysLifecycleState.toMap()
        overlaysLifecycleState.clear()
        return states
    }

    fun haveStates(): Boolean = overlaysLifecycleState.isNotEmpty()
}
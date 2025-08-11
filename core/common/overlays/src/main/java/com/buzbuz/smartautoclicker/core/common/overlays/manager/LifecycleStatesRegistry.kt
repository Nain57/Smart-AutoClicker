
package com.buzbuz.smartautoclicker.core.common.overlays.manager

import androidx.lifecycle.Lifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu

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

    fun clearStates() {
        overlaysLifecycleState.clear()
    }

    fun haveStates(): Boolean = overlaysLifecycleState.isNotEmpty()
}
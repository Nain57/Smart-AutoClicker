
package com.buzbuz.smartautoclicker.core.common.overlays.manager.navigation

import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay

internal sealed class OverlayNavigationRequest {

    data object NavigateUp : OverlayNavigationRequest()

    data class NavigateTo(
        val overlay: Overlay,
        val hideCurrent: Boolean = false,
    ) : OverlayNavigationRequest()
}
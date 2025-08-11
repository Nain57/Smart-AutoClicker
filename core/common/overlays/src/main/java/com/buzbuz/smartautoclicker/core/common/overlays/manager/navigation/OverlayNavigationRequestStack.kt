
package com.buzbuz.smartautoclicker.core.common.overlays.manager.navigation

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.common.overlays.manager.LifoStack
import java.io.PrintWriter

internal class OverlayNavigationRequestStack : LifoStack<OverlayNavigationRequest>(), Dumpable {

    override fun push(element: OverlayNavigationRequest) =
        when(element) {
            is OverlayNavigationRequest.NavigateUp -> pushNavigateUp(element)
            is OverlayNavigationRequest.NavigateTo -> pushNavigateTo(element)
        }

    private fun pushNavigateUp(request: OverlayNavigationRequest.NavigateUp) {
        when {
            isNotEmpty() && top is OverlayNavigationRequest.NavigateTo -> pop()
            else -> super.push(request)
        }
    }

    private fun pushNavigateTo(request: OverlayNavigationRequest.NavigateTo) {
        if (!contains(request)) super.push(request)
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).append("- Navigation Request Stack:")
            if (isEmpty()) {
                append(" empty").println()
                return
            }
            println()

            forEach { request -> append(contentPrefix).println(request) }
        }
    }
}
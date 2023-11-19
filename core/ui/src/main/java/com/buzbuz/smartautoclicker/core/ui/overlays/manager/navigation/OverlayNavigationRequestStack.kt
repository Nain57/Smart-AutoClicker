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
package com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation

import com.buzbuz.smartautoclicker.core.ui.utils.internal.LifoStack
import java.io.PrintWriter

internal class OverlayNavigationRequestStack : LifoStack<OverlayNavigationRequest>() {

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

    fun dump(writer: PrintWriter, prefix: String) {
        forEach { writer.println("$prefix $it") }
    }
}
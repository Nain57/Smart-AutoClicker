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
package com.buzbuz.smartautoclicker.core.ui.overlays.navigation

import java.io.PrintWriter

internal class OverlayNavigationRequestStack {

    /** Contains all requests, from oldest to most recent. */
    private val stack: ArrayDeque<OverlayNavigationRequest> = ArrayDeque(emptyList())

    fun isEmpty(): Boolean = stack.isEmpty()

    fun pop(): OverlayNavigationRequest? =
        if (stack.isNotEmpty()) stack.removeLast() else null

    fun push(request: OverlayNavigationRequest) =
        when(request) {
            is OverlayNavigationRequest.NavigateUp -> pushNavigateUp(request)
            is OverlayNavigationRequest.NavigateTo -> pushNavigateTo(request)
            is OverlayNavigationRequest.CloseAll -> pushCloseAll(request)
        }

    private fun pushNavigateUp(request: OverlayNavigationRequest.NavigateUp) {
        when {
            stack.isNotEmpty() && stack.last() is OverlayNavigationRequest.NavigateTo -> stack.removeLast()
            else -> stack.addLast(request)
        }
    }

    private fun pushNavigateTo(request: OverlayNavigationRequest.NavigateTo) {
        if (!stack.contains(request)) stack.addLast(request)
    }

    private fun pushCloseAll(request: OverlayNavigationRequest.CloseAll) {
        stack.clear()
        stack.addLast(request)
    }

    fun dump(writer: PrintWriter, prefix: String) {
        stack.forEach { writer.println("$prefix $it") }
    }
}
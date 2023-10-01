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
package com.buzbuz.smartautoclicker.core.ui.bindings.dropdown

import android.view.View

internal class DropdownViewsMonitor {

    private val viewMap: MutableMap<DropdownItem, View> = mutableMapOf()

    fun onViewBound(item: DropdownItem, view: View): Boolean {
        if (viewMap.containsKey(item)) return false

        viewMap[item] = view
        return true
    }

    fun onViewUnbound(item: DropdownItem, view: View): Boolean {
        val boundView = viewMap[item] ?: return false
        if (boundView != view) return false

        viewMap.remove(item)
        return true
    }

    fun clearBoundViews(): Unit = viewMap.clear()
}
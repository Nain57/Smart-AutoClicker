
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
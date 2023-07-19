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
package com.buzbuz.smartautoclicker.core.ui.monitoring

import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ViewMonitor {

    private val onMonitoredViewLayoutChangedListener =
        OnGlobalLayoutListener {
            refreshViewSize()
        }

    private var monitoredView: View? = null
    private var positioningType: ViewPositioningType? = null

    private val _position: MutableStateFlow<Rect> = MutableStateFlow(Rect())
    val position: StateFlow<Rect> = _position

    fun attachView(view: View, type: ViewPositioningType) {
        monitoredView = view
        positioningType = type

        refreshViewSize()
        view.viewTreeObserver.addOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
    }

    fun detachView() {
        monitoredView?.viewTreeObserver?.removeOnGlobalLayoutListener(onMonitoredViewLayoutChangedListener)
        monitoredView = null

        _position.value = Rect()
    }

    fun performClick(): Boolean =
        monitoredView?.performClick() ?: false

    private fun refreshViewSize() {
        val view = monitoredView ?: return
        val type = positioningType ?: return

        val location = when (type) {
            ViewPositioningType.WINDOW -> view.getLocationInWindow()
            ViewPositioningType.SCREEN -> view.getLocationOnScreen()
        }
        _position.value = Rect(location.x, location.y, location.x + view.width, location.y + view.height)
    }

    private fun View.getLocationInWindow(): Point {
        val location = IntArray(2)
        getLocationInWindow(location)
        return Point(location[0], location[1])
    }

    private fun View.getLocationOnScreen(): Point {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return Point(location[0], location[1] -  DisplayMetrics.getInstance(context).safeInsetTop)
    }
}

enum class ViewPositioningType {
    WINDOW,
    SCREEN,
}
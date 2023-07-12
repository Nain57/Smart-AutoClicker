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
package com.buzbuz.smartautoclicker.feature.tutorial.data.monitoring

import android.graphics.Rect
import android.view.View
import android.view.View.OnLayoutChangeListener

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ViewMonitor {

    private val onMonitoredViewLayoutChangedListener =
        OnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
            onMonitoredViewLayoutChanged(v, Rect(left, top, right, bottom))
        }

    private var monitoredView: View? = null

    private val _position: MutableStateFlow<Rect> = MutableStateFlow(Rect())
    val position: StateFlow<Rect> = _position

    fun attachView(view: View) {
        monitoredView = view
        view.addOnLayoutChangeListener(onMonitoredViewLayoutChangedListener)
    }

    fun detachView() {
        monitoredView?.removeOnLayoutChangeListener(onMonitoredViewLayoutChangedListener)
        monitoredView = null

        _position.value = Rect()
    }

    fun performClick(): Boolean =
        monitoredView?.performClick() ?: false

    private fun onMonitoredViewLayoutChanged(view: View, relativePosition: Rect) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0]
        val y = location[1]

        _position.value = Rect(x, y, x + relativePosition.width(), y + relativePosition.height())
    }
}
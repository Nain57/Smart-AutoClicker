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

import android.graphics.Rect
import android.view.View

import kotlinx.coroutines.flow.StateFlow

class MonitoredViewsManager private constructor() {

    companion object {

        /** Singleton preventing multiple instances of the MonitoredViewsManager at the same time. */
        @Volatile
        private var INSTANCE: MonitoredViewsManager? = null

        /**
         * Get the MonitoredViewsManager singleton, or instantiates it if it wasn't yet.
         *
         * @return the MonitoredViewsManager singleton.
         */
        fun getInstance(): MonitoredViewsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MonitoredViewsManager()
                INSTANCE = instance
                instance
            }
        }
    }

    private val monitoredViews: MutableMap<MonitoredViewType, ViewMonitor> = mutableMapOf()
    private val monitoredClicks: MutableMap<MonitoredViewType, () -> Unit> = mutableMapOf()

    fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType = ViewPositioningType.WINDOW,
    ) {
        if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor()
        monitoredViews[type]?.attachView(monitoredView, positioningType)
    }

    fun detach(type: MonitoredViewType) {
        monitoredViews[type]?.detachView()
    }

    fun notifyClick(type: MonitoredViewType) {
        monitoredClicks[type]?.invoke()
    }

    fun setExpectedViews(types: Set<MonitoredViewType>) {
        types.forEach { type ->
            if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor()
        }
    }

    fun clearExpectedViews() {
        monitoredViews.clear()
    }

    fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>? =
        monitoredViews[type]?.position

    fun performClick(type: MonitoredViewType): Boolean =
        monitoredViews[type]?.performClick() ?: false

    fun monitorNextClick(type: MonitoredViewType, listener: () -> Unit) {
        monitoredClicks[type] = {
            monitoredClicks.remove(type)
            listener()
        }
    }
}
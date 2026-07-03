/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring

import android.graphics.Rect
import android.view.View

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoredViewsManagerImpl @Inject constructor(
    private val displayConfigManager: DisplayConfigManager,
) : MonitoredViewsManager {

    private val monitoredViews: MutableMap<MonitoredViewType, ViewMonitor> = mutableMapOf()
    private val monitoredClicks: MutableMap<MonitoredViewType, () -> Unit> = mutableMapOf()

    override fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType,
    ) {
        if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor(displayConfigManager)
        monitoredViews[type]?.attachView(monitoredView, positioningType)
    }

    override fun detach(type: MonitoredViewType) {
        monitoredViews[type]?.detachView()
    }

    override fun notifyClick(type: MonitoredViewType) {
        monitoredClicks[type]?.invoke()
    }

    override fun setExpectedViews(types: Set<MonitoredViewType>) {
        types.forEach { type ->
            if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor(displayConfigManager)
        }
    }

    override fun clearExpectedViews() {
        monitoredViews.clear()
    }

    override fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>? =
        monitoredViews[type]?.position

    override fun performClick(type: MonitoredViewType): Boolean {
        notifyClick(type)
        return monitoredViews[type]?.performClick() ?: false
    }

    override fun monitorNextClick(type: MonitoredViewType, listener: () -> Unit) {
        monitoredClicks[type] = {
            monitoredClicks.remove(type)
            listener()
        }
    }

    override fun stopNextClickMonitoring(type: MonitoredViewType) {
        monitoredClicks.remove(type)
    }
}
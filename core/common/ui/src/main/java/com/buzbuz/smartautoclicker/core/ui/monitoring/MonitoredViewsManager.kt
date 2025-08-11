
package com.buzbuz.smartautoclicker.core.ui.monitoring

import android.graphics.Rect
import android.view.View
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoredViewsManager @Inject constructor(
    private val displayConfigManager: DisplayConfigManager,
) {

    private val monitoredViews: MutableMap<MonitoredViewType, ViewMonitor> = mutableMapOf()
    private val monitoredClicks: MutableMap<MonitoredViewType, () -> Unit> = mutableMapOf()

    fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType = ViewPositioningType.SCREEN,
    ) {
        if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor(displayConfigManager)
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
            if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor(displayConfigManager)
        }
    }

    fun clearExpectedViews() {
        monitoredViews.clear()
    }

    fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>? =
        monitoredViews[type]?.position

    fun performClick(type: MonitoredViewType): Boolean {
        return monitoredViews[type]?.performClick() ?: false
    }

    fun monitorNextClick(type: MonitoredViewType, listener: () -> Unit) {
        monitoredClicks[type] = {
            monitoredClicks.remove(type)
            listener()
        }
    }
}
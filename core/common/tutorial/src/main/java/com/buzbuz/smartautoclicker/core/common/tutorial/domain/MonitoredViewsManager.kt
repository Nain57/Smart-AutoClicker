package com.buzbuz.smartautoclicker.core.common.tutorial.domain

import android.graphics.Rect
import android.view.View
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.ViewMonitor
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MonitoredViewsManager {

    fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType = ViewPositioningType.SCREEN,
    )

    fun detach(type: MonitoredViewType)

    fun notifyClick(type: MonitoredViewType)

    fun setExpectedViews(types: Set<MonitoredViewType>)

    fun clearExpectedViews()

    fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>?

    fun performClick(type: MonitoredViewType): Boolean

    fun monitorNextClick(type: MonitoredViewType, listener: () -> Unit)

    fun stopNextClickMonitoring(type: MonitoredViewType)
}
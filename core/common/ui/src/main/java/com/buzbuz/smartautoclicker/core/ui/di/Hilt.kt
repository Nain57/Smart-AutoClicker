
package com.buzbuz.smartautoclicker.core.ui.di

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UiEntryPoint {
    fun monitoredViewManager(): MonitoredViewsManager
}
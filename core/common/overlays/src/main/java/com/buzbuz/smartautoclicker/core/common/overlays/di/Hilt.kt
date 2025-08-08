
package com.buzbuz.smartautoclicker.core.common.overlays.di

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OverlaysEntryPoint {

    fun overlayManager(): OverlayManager
    fun overlayMenuPositionDataSource(): OverlayMenuPositionDataSource
}

package com.buzbuz.smartautoclicker.core.common.overlays.di

import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@DefineComponent.Builder
interface OverlayComponentBuilder {
    fun overlay(@BindsInstance overlay: Overlay): OverlayComponentBuilder
    fun build(): OverlayComponent
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface OverlayComponentBuilderEntryPoint {
    fun overlayComponentBuilder(): OverlayComponentBuilder
}
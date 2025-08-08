
package com.buzbuz.smartautoclicker.feature.smart.debugging.di

import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.DebugModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryElementViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryImageConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.DebugReportModel

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(OverlayComponent::class)
interface DebuggingViewModelsEntryPoint {
    fun debugModel(): DebugModel
    fun debugReportModel(): DebugReportModel
    fun tryElementViewModel(): TryElementViewModel
    fun tryImageConditionViewModel(): TryImageConditionViewModel
}
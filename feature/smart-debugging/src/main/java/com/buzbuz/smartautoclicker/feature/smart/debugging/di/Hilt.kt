/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.di

import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.conditiontry.TryImageConditionViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.eventtry.TryElementViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.DebugReportViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.overview.DebugReportOverviewViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.DebugReportEventOccurrenceDetailsViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition.DebugConditionContentViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter.DebugCounterStateContentViewModel
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.event.DebugEventsStateContentViewModel

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(OverlayComponent::class)
interface DebuggingViewModelsEntryPoint {
    fun debugConditionContentViewModel(): DebugConditionContentViewModel
    fun debugCounterStateContentViewModel(): DebugCounterStateContentViewModel
    fun debugEventStateContentViewModel(): DebugEventsStateContentViewModel
    fun debugReportViewModel(): DebugReportViewModel
    fun debugReportEventOccurrenceViewModel(): DebugReportEventOccurrenceDetailsViewModel
    fun debugReportOverviewViewModel(): DebugReportOverviewViewModel
    fun debugReportTimelineViewModel(): DebugReportTimelineViewModel
    fun tryElementViewModel(): TryElementViewModel
    fun tryImageConditionViewModel(): TryImageConditionViewModel
}
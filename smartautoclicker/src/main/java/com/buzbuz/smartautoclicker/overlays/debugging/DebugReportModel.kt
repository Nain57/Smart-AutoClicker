/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.debugging

import android.content.Context

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.engine.DetectorEngine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

/** */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugReportModel(context: Context) : OverlayViewModel(context) {

    /** */
    val reportItems: Flow<List<DebugReportItem>> = DetectorEngine.getDetectorEngine(context).debugEngine
        .flatMapLatest { it.debugReport }
        .map { report ->
            report ?: return@map emptyList()

            buildList {
                add(DebugReportItem.ScenarioReportItem(
                    id = report.scenario.id,
                    name = report.scenario.name,
                    duration = report.sessionInfo.totalProcessingTimeMs.milliseconds.toString(),
                    imageProcessed = report.imageProcessedInfo.processingCount.toString(),
                    averageImageProcessingTime = report.imageProcessedInfo.avgProcessingTimeMs.milliseconds.toString(),
                    eventsTriggered = report.eventsTriggeredCount.toString(),
                    conditionsDetected = report.conditionsDetectedCount.toString(),
                ))

                report.eventsProcessedInfo.forEach { (event, debugInfo) ->
                    add(DebugReportItem.EventReportItem(
                        id = event.id,
                        name = event.name,
                        triggerCount = debugInfo.successCount.toString(),
                        processingCount = debugInfo.processingCount.toString(),
                        avgProcessingDuration = debugInfo.avgProcessingTimeMs.milliseconds.toString(),
                        minProcessingDuration = debugInfo.minProcessingTimeMs.milliseconds.toString(),
                        maxProcessingDuration = debugInfo.maxProcessingTimeMs.milliseconds.toString(),
                    ))

                    event.conditions?.forEach { condition ->
                        report.conditionsProcessedInfo[condition.id]?.let { (condition, condDebugInfo) ->
                            add(DebugReportItem.ConditionReportItem(
                                id = condition.id,
                                name = condition.name,
                                matchCount = condDebugInfo.successCount.toString(),
                                processingCount = condDebugInfo.processingCount.toString(),
                                avgProcessingDuration = condDebugInfo.avgProcessingTimeMs.milliseconds.toString(),
                                minProcessingDuration = condDebugInfo.minProcessingTimeMs.milliseconds.toString(),
                                maxProcessingDuration = condDebugInfo.maxProcessingTimeMs.milliseconds.toString(),
                            ))
                        }
                    }
                }
            }
        }
}

sealed class DebugReportItem {

    abstract val id: Long

    abstract val name: String

    data class ScenarioReportItem(
        override val id: Long,
        override val name: String,
        val duration: String,
        val imageProcessed: String,
        val averageImageProcessingTime: String,
        val eventsTriggered: String,
        val conditionsDetected: String,
    ) : DebugReportItem()

    data class EventReportItem(
        override val id: Long,
        override val name: String,
        val triggerCount: String,
        val processingCount: String,
        val avgProcessingDuration: String,
        val minProcessingDuration: String,
        val maxProcessingDuration: String,
    ): DebugReportItem()

    data class ConditionReportItem(
        override val id: Long,
        override val name: String,
        val matchCount: String,
        val processingCount: String,
        val avgProcessingDuration: String,
        val minProcessingDuration: String,
        val maxProcessingDuration: String,
    ): DebugReportItem()
}
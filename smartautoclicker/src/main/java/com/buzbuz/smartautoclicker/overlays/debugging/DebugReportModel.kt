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
import com.buzbuz.smartautoclicker.engine.debugging.ConditionProcessingDebugInfo
import com.buzbuz.smartautoclicker.engine.debugging.DebugReport
import com.buzbuz.smartautoclicker.engine.debugging.ProcessingDebugInfo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugReportModel(context: Context) : OverlayViewModel(context) {

    private val debugReport: Flow<DebugReport?> = DetectorEngine.getDetectorEngine(context).debugEngine
        .flatMapLatest { it.debugReport }

    private val expandedEventsMap = MutableStateFlow<MutableSet<Long>>(mutableSetOf())
    private val expandedConditionsMap = MutableStateFlow<MutableSet<Long>>(mutableSetOf())

    val reportItems =
        combine(debugReport, expandedEventsMap, expandedConditionsMap) { report, expandedEvents, expandedCondition ->
            report ?: return@combine emptyList()

            buildList {
                add(newScenarioItem(report))

                report.eventsProcessedInfo.forEach { (event, debugInfo) ->
                    val eventExpanded = expandedEvents.contains(event.id)
                    add(newEventItem(event.id, event.name, debugInfo, eventExpanded))

                    if (eventExpanded) {
                        event.conditions?.forEach { condition ->
                            report.conditionsProcessedInfo[condition.id]?.let { (condition, condDebugInfo) ->
                                add(newConditionItem(
                                    condition.id,
                                    condition.name,
                                    condDebugInfo,
                                    expandedCondition.contains(condition.id),
                                ))
                            }
                        }
                    }

                }
            }
        }

    fun collapseExpandEvent(eventId: Long) {
        viewModelScope.launch {
            expandedEventsMap.emit(
                expandedEventsMap.value.toMutableSet().apply {
                    if (contains(eventId)) remove(eventId)
                    else add(eventId)
                }
            )
        }
    }

    fun collapseExpandCondition(conditionId: Long) {
        viewModelScope.launch {
            expandedConditionsMap.emit(
                expandedConditionsMap.value.toMutableSet().apply {
                    if (contains(conditionId)) remove(conditionId)
                    else add(conditionId)
                }
            )
        }
    }

    private fun newScenarioItem(debugInfo: DebugReport) =
        DebugReportItem.ScenarioReportItem(
            id = debugInfo.scenario.id,
            name = debugInfo.scenario.name,
            duration = debugInfo.sessionInfo.totalProcessingTimeMs.milliseconds.toString(),
            imageProcessed = debugInfo.imageProcessedInfo.processingCount.toString(),
            averageImageProcessingTime = debugInfo.imageProcessedInfo.avgProcessingTimeMs.milliseconds.toString(),
            eventsTriggered = debugInfo.eventsTriggeredCount.toString(),
            conditionsDetected = debugInfo.conditionsDetectedCount.toString(),
            isExpanded = true,
        )

    private fun newEventItem(id: Long, name: String, debugInfo: ProcessingDebugInfo, expanded: Boolean) =
        DebugReportItem.EventReportItem(
            id = id,
            name = name,
            triggerCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.milliseconds.toString(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.milliseconds.toString(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.milliseconds.toString(),
            isExpanded = expanded,
        )

    private fun newConditionItem(id: Long, name: String, debugInfo: ConditionProcessingDebugInfo, expanded: Boolean) =
        DebugReportItem.ConditionReportItem(
            id = id,
            name = name,
            matchCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.milliseconds.toString(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.milliseconds.toString(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.milliseconds.toString(),
            avgConfidence = debugInfo.avgConfidenceRate.formatConfidenceRate(),
            minConfidence = debugInfo.minConfidenceRate.formatConfidenceRate(),
            maxConfidence = debugInfo.maxConfidenceRate.formatConfidenceRate(),
            isExpanded = expanded,
        )
}

sealed class DebugReportItem {

    abstract val id: Long
    abstract val name: String
    abstract val isExpanded: Boolean

    data class ScenarioReportItem(
        override val id: Long,
        override val name: String,
        override val isExpanded: Boolean,
        val duration: String,
        val imageProcessed: String,
        val averageImageProcessingTime: String,
        val eventsTriggered: String,
        val conditionsDetected: String,
    ) : DebugReportItem()

    data class EventReportItem(
        override val id: Long,
        override val name: String,
        override val isExpanded: Boolean,
        val triggerCount: String,
        val processingCount: String,
        val avgProcessingDuration: String,
        val minProcessingDuration: String,
        val maxProcessingDuration: String,
    ): DebugReportItem()

    data class ConditionReportItem(
        override val id: Long,
        override val name: String,
        override val isExpanded: Boolean,
        val matchCount: String,
        val processingCount: String,
        val avgProcessingDuration: String,
        val minProcessingDuration: String,
        val maxProcessingDuration: String,
        val avgConfidence: String,
        val minConfidence: String,
        val maxConfidence: String,
    ): DebugReportItem()
}

/** Format this value as a displayable confidence rate. */
fun Double.formatConfidenceRate(): String = "${String.format("%.2f", this * 100)} % "
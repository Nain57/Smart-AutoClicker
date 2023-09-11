/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ConditionProcessingDebugInfo
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebugReport
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ProcessingDebugInfo

import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

/** ViewModel for the [DebugReportDialog]. */
class DebugReportModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)
    /** Repository for the processing session debugging info. */
    private val debuggingRepository = DebuggingRepository.getDebuggingRepository(application)
    /** The debug report of the last detection session. */
    private val debugReport: Flow<DebugReport?> = debuggingRepository.debugReport

    /** The dialog is represented by a list. This is the items populating it. */
    val reportItems = debugReport.map { report ->
        report ?: return@map emptyList()

        var totalEventProcessingTimeMs = 0L
        var totalEventProcessingCount = 0L
        val eventReports = buildList {
            report.eventsProcessedInfo.forEach { (event, debugInfo) ->
                totalEventProcessingTimeMs += debugInfo.totalProcessingTimeMs
                totalEventProcessingCount += debugInfo.processingCount
                add(newEventItem(event.id.databaseId, event.name, debugInfo, createConditionReports(event.conditions, report)))
            }
        }

        buildList {
            val avgProcTimeMs = if (eventReports.isEmpty()) 0L else totalEventProcessingTimeMs / totalEventProcessingCount
            add(newScenarioItem(report, avgProcTimeMs))
            addAll(eventReports)
        }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        if (condition.bitmap != null) {
            onBitmapLoaded.invoke(condition.bitmap)
            return null
        }

        if (condition.path != null) {
            return viewModelScope.launch(Dispatchers.IO) {
                val bitmap = repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        onBitmapLoaded.invoke(bitmap)
                    }
                }
            }
        }

        onBitmapLoaded.invoke(null)
        return null
    }

    private fun newScenarioItem(debugInfo: DebugReport, averageImageProcessingTime: Long) =
        DebugReportItem.ScenarioReportItem(
            id = debugInfo.scenario.id.databaseId,
            name = debugInfo.scenario.name,
            duration = debugInfo.sessionInfo.totalProcessingTimeMs.formatDuration(),
            imageProcessed = debugInfo.imageProcessedInfo.processingCount.toString(),
            averageImageProcessingTime = averageImageProcessingTime.formatDuration(),
            eventsTriggered = debugInfo.eventsTriggeredCount.toString(),
            conditionsDetected = debugInfo.conditionsDetectedCount.toString(),
        )

    private fun newEventItem(id: Long, name: String, debugInfo: ProcessingDebugInfo, conditionReports: List<ConditionReport>) =
        DebugReportItem.EventReportItem(
            id = id,
            name = name,
            triggerCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.formatDuration(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.formatDuration(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.formatDuration(),
            conditionReports = conditionReports,
        )

    private fun createConditionReports(conditions: List<Condition>?, debugReport: DebugReport) = buildList {
        conditions?.forEach { condition ->
            debugReport.conditionsProcessedInfo[condition.id.databaseId]?.let { (condition, condDebugInfo) ->
                add(newConditionReport(condition.id.databaseId, condition, condDebugInfo))
            }
        }
    }

    private fun newConditionReport(id: Long, condition: Condition, debugInfo: ConditionProcessingDebugInfo) =
        ConditionReport(
            id = id,
            condition = condition,
            matchCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.formatDuration(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.formatDuration(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.formatDuration(),
            avgConfidence = debugInfo.avgConfidenceRate.formatConfidenceRate(),
            minConfidence = debugInfo.minConfidenceRate.formatConfidenceRate(),
            maxConfidence = debugInfo.maxConfidenceRate.formatConfidenceRate(),
        )
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
        val conditionReports: List<ConditionReport>,
    ): DebugReportItem()
}

data class ConditionReport(
    val id: Long,
    val condition: Condition,
    val matchCount: String,
    val processingCount: String,
    val avgProcessingDuration: String,
    val minProcessingDuration: String,
    val maxProcessingDuration: String,
    val avgConfidence: String,
    val minConfidence: String,
    val maxConfidence: String,
)

/** Format this value as a displayable confidence rate. */
fun Double.formatConfidenceRate(): String = "${String.format("%.2f", this * 100)} % "

private fun Long.formatDuration(): String =
    if (this < 1) "< 1ms"
    else milliseconds.toString()
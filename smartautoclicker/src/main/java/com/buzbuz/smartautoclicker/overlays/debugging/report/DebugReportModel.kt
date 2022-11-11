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
package com.buzbuz.smartautoclicker.overlays.debugging.report

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.engine.DetectorEngine
import com.buzbuz.smartautoclicker.engine.debugging.ConditionProcessingDebugInfo
import com.buzbuz.smartautoclicker.engine.debugging.DebugReport
import com.buzbuz.smartautoclicker.engine.debugging.ProcessingDebugInfo

import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

/** ViewModel for the [DebugReportDialog]. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugReportModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)
    /** The debug report of the last detection session. */
    private val debugReport: Flow<DebugReport?> = DetectorEngine.getDetectorEngine(application).debugEngine
        .flatMapLatest { it.debugReport }

    /** The dialog is represented by a list. This is the items populating it. */
    val reportItems = debugReport.map { report ->
            report ?: return@map emptyList()

            buildList {
                add(newScenarioItem(report))
                report.eventsProcessedInfo.forEach { (event, debugInfo) ->
                    add(newEventItem(event.id, event.name, debugInfo, createConditionReports(event.conditions, report)))
                }
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

    private fun newScenarioItem(debugInfo: DebugReport) =
        DebugReportItem.ScenarioReportItem(
            id = debugInfo.scenario.id,
            name = debugInfo.scenario.name,
            duration = debugInfo.sessionInfo.totalProcessingTimeMs.milliseconds.toString(),
            imageProcessed = debugInfo.imageProcessedInfo.processingCount.toString(),
            averageImageProcessingTime = debugInfo.imageProcessedInfo.avgProcessingTimeMs.milliseconds.toString(),
            eventsTriggered = debugInfo.eventsTriggeredCount.toString(),
            conditionsDetected = debugInfo.conditionsDetectedCount.toString(),
        )

    private fun newEventItem(id: Long, name: String, debugInfo: ProcessingDebugInfo, conditionReports: List<ConditionReport>) =
        DebugReportItem.EventReportItem(
            id = id,
            name = name,
            triggerCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.milliseconds.toString(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.milliseconds.toString(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.milliseconds.toString(),
            conditionReports = conditionReports,
        )

    private fun createConditionReports(conditions: List<Condition>?, debugReport: DebugReport) = buildList {
        conditions?.forEach { condition ->
            debugReport.conditionsProcessedInfo[condition.id]?.let { (condition, condDebugInfo) ->
                add(newConditionReport(condition.id, condition, condDebugInfo))
            }
        }
    }

    private fun newConditionReport(id: Long, condition: Condition, debugInfo: ConditionProcessingDebugInfo) =
        ConditionReport(
            id = id,
            condition = condition,
            matchCount = debugInfo.successCount.toString(),
            processingCount = debugInfo.processingCount.toString(),
            avgProcessingDuration = debugInfo.avgProcessingTimeMs.milliseconds.toString(),
            minProcessingDuration = debugInfo.minProcessingTimeMs.milliseconds.toString(),
            maxProcessingDuration = debugInfo.maxProcessingTimeMs.milliseconds.toString(),
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
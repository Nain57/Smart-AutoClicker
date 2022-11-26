/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.engine.debugging

import android.graphics.Rect

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.engine.ProcessorResult

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Engine for the debugging of a scenario processing. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugEngine(
    val instantData: Boolean,
    val generateReport: Boolean,
    private val scenario: Scenario,
    private val events: List<Event>,
) {
    /** Record the detection session duration. */
    private val sessionRecorder = Recorder()
    /** Record all images processed. */
    private val imageRecorder = Recorder()
    /** Map of event id to their recorder. */
    private val eventsRecorderMap: MutableMap<Long, Recorder> = mutableMapOf()
    /** Map of condition id to their recorder. */
    private val conditionsRecorderMap: MutableMap<Long, ConditionRecorder> = mutableMapOf()

    /** The event currently processed. */
    private var currProcEvtId: Long? = null
    /** The condition currently processed. */
    private var currProcCondId: Long? = null

    /** The debug report. Set once the detection session is complete. */
    private val _debugReport = MutableStateFlow<DebugReport?>(null)
    val debugReport: Flow<DebugReport?> = _debugReport

    /** The DebugInfo for the current image. */
    private val currentInfo = MutableSharedFlow<DebugInfo>()
    /** The DebugInfo for the current image. */
    val lastResult = currentInfo
    /** The DebugInfo for the last positive detection. */
    val lastPositiveInfo = currentInfo
        .filter { it.detectionResult.isDetected }

    /** Start the session recorder at the DebugEngine creation. */
    init {
        if (generateReport) sessionRecorder.onProcessingStart()
    }

    internal fun onImageProcessingStarted() {
        if (!generateReport) return

        imageRecorder.onProcessingStart()
    }

    internal fun onEventProcessingStarted(event: Event) {
        if (!generateReport) return

        if (currProcEvtId != null) throw IllegalStateException("start called without a complete")
        currProcEvtId = event.id

        if (!eventsRecorderMap.containsKey(event.id)) {
            eventsRecorderMap[event.id] = Recorder()
        }
        eventsRecorderMap[event.id]!!.onProcessingStart()
    }

    internal fun onConditionProcessingStarted(condition: Condition) {
        if (!generateReport) return

        if (currProcCondId != null) throw IllegalStateException("start called without a complete")
        currProcCondId = condition.id

        if (!conditionsRecorderMap.containsKey(condition.id)) {
            conditionsRecorderMap[condition.id] = ConditionRecorder()
        }
        conditionsRecorderMap[condition.id]!!.onProcessingStart()
    }

    internal fun onConditionProcessingCompleted(detectionResult: DetectionResult) {
        if (!generateReport) return

        if (currProcCondId == null) throw IllegalStateException("completed called before start")

        conditionsRecorderMap[currProcCondId]?.onProcessingEnd(
            detectionResult.isDetected,
            detectionResult.confidenceRate
        )
        currProcCondId = null
    }

    internal suspend fun onEventProcessingCompleted(result: ProcessorResult) {
        if (generateReport) {
            if (currProcEvtId == null) throw IllegalStateException("completed called before start")

            eventsRecorderMap[currProcEvtId]?.onProcessingEnd(result.eventMatched && result.event != null)
            currProcEvtId = null
        }

        // Notify current detection progress
        if (instantData && result.event != null && result.condition != null && result.detectionResult != null) {
            val halfWidth = result.condition.area.width() / 2
            val halfHeight = result.condition.area.height() / 2

            val coordinates = if (result.detectionResult.position.x == 0 && result.detectionResult.position.y == 0) Rect()
            else Rect(
                result.detectionResult.position.x - halfWidth,
                result.detectionResult.position.y - halfHeight,
                result.detectionResult.position.x + halfWidth,
                result.detectionResult.position.y + halfHeight
            )

            currentInfo.emit(DebugInfo(result.event, result.condition, result.detectionResult, coordinates))
        }
    }

    internal fun onImageProcessingCompleted() {
        if (!generateReport) return

        imageRecorder.onProcessingEnd()
    }

    internal fun onSessionEnded() {
        currentInfo.resetReplayCache()
        if (!generateReport) return

        sessionRecorder.onProcessingEnd()

        var eventsTriggeredCount = 0L
        var conditionsDetectedCount = 0L
        val conditions = mutableListOf<Condition>()

        val eventsReport = events.map { event ->
            event.conditions?.let { conditions.addAll(it) }

            val debugInfo = eventsRecorderMap[event.id]?.let { processingRecorder ->
                eventsTriggeredCount += processingRecorder.successCount
                processingRecorder.toProcessingDebugInfo()
            } ?: ProcessingDebugInfo()

            event to debugInfo
        }.sortedBy { it.first.priority }

        val conditionReport = HashMap<Long, Pair<Condition, ConditionProcessingDebugInfo>>()
        conditions.forEach { condition ->
            val debugInfo = conditionsRecorderMap[condition.id]?.let { processingRecorder ->
                conditionsDetectedCount += processingRecorder.successCount
                processingRecorder.toConditionProcessingDebugInfo()
            } ?: ConditionProcessingDebugInfo()

            conditionReport[condition.id] = condition to debugInfo
        }

        _debugReport.value = DebugReport(
            scenario,
            sessionRecorder.toProcessingDebugInfo(),
            imageRecorder.toProcessingDebugInfo(),
            eventsTriggeredCount,
            eventsReport,
            conditionsDetectedCount,
            conditionReport,
        )
    }

    internal fun cancelCurrentProcessing() {
        currProcEvtId = null
        currProcCondId = null
    }
}

/** Debug information for the scenario processing */
data class DebugInfo(
    val event: Event,
    val condition: Condition,
    val detectionResult: DetectionResult,
    val conditionArea: Rect,
)
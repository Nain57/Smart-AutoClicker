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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.data

import android.content.Context
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.debugging.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.debugging.getIsDebugReportEnabled
import com.buzbuz.smartautoclicker.feature.scenario.debugging.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ConditionProcessingDebugInfo
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebugInfo
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebugReport
import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.ProcessingDebugInfo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Engine for the debugging of a scenario processing. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DebugEngine {

    /** Record the detection session duration. */
    private val sessionRecorder = Recorder()
    /** Record all images processed. */
    private val imageRecorder = Recorder()
    /** Map of event id to their recorder. */
    private val eventsRecorderMap: MutableMap<Long, Recorder> = mutableMapOf()
    /** Map of condition id to their recorder. */
    private val conditionsRecorderMap: MutableMap<Long, ConditionRecorder> = mutableMapOf()

    /** Tells if the live debugging data should be computed. */
    private var instantData: Boolean = false
    /** Tells if the debugging report should be generated. */
    private var generateReport: Boolean = false
    /** The scenario currently processed. */
    private var currentScenario: Scenario? = null
    /** The events for the scenario currently processed. */
    private var currentEvents: List<Event> = emptyList()

    /** The event currently processed. */
    private var currProcEvtId: Long? = null
    /** The condition currently processed. */
    private var currProcCondId: Long? = null

    /** Tells if a detection session is currently being debugged. */
    private val _isDebugging = MutableStateFlow(false)
    val isDebugging: Flow<Boolean> = _isDebugging

    /** The debug report. Set once the detection session is complete. */
    private val _debugReport = MutableStateFlow<DebugReport?>(null)
    val debugReport: Flow<DebugReport?> = _debugReport

    /** The DebugInfo for the current image. */
    val currentInfo = MutableSharedFlow<DebugInfo?>()

    fun onSessionStarted(context: Context, scenario: Scenario, events: List<Event>) {
        if (_isDebugging.value) return
        _isDebugging.value = true

        with(context.getDebugConfigPreferences()) {
            instantData = getIsDebugViewEnabled(context)
            generateReport = getIsDebugReportEnabled(context)
        }

        currentScenario = scenario
        currentEvents = events.toList()

        if (generateReport) sessionRecorder.onProcessingStart()
    }

    fun onImageProcessingStarted() {
        if (!generateReport) return

        imageRecorder.onProcessingStart()
    }

    fun onEventProcessingStarted(event: Event) {
        if (!generateReport) return

        if (currProcEvtId != null) throw IllegalStateException("start called without a complete")
        currProcEvtId = event.id.databaseId

        eventsRecorderMap
            .getOrDefaultWithPut(event.id.databaseId) { Recorder() }
            .onProcessingStart()
    }

    fun onConditionProcessingStarted(condition: Condition) {
        if (!generateReport) return

        if (currProcCondId != null) throw IllegalStateException("start called without a complete")
        currProcCondId = condition.id.databaseId

        conditionsRecorderMap
            .getOrDefaultWithPut(condition.id.databaseId) { ConditionRecorder() }
            .onProcessingStart()
    }

    fun onConditionProcessingCompleted(detectionResult: DetectionResult) {
        if (!generateReport) return

        if (currProcCondId == null) throw IllegalStateException("completed called before start")

        conditionsRecorderMap[currProcCondId]?.onProcessingEnd(
            detectionResult.isDetected,
            detectionResult.confidenceRate
        )
        currProcCondId = null
    }

    suspend fun onEventProcessingCompleted(
        isEventMatched: Boolean,
        event: Event?,
        condition: Condition?,
        result: DetectionResult?,
    ) {
        if (generateReport) {
            if (currProcEvtId == null) throw IllegalStateException("completed called before start")

            eventsRecorderMap[currProcEvtId]?.onProcessingEnd(isEventMatched)
            currProcEvtId = null
        }

        // Notify current detection progress
        if (instantData && event != null && condition != null && result != null) {
            val halfWidth = condition.area.width() / 2
            val halfHeight = condition.area.height() / 2

            val coordinates = if (result.position.x == 0 && result.position.y == 0) Rect()
            else Rect(
                result.position.x - halfWidth,
                result.position.y - halfHeight,
                result.position.x + halfWidth,
                result.position.y + halfHeight
            )

            currentInfo.emit(DebugInfo(event, condition, result, coordinates))
        }
    }

    fun onImageProcessingCompleted() {
        if (!generateReport) return

        imageRecorder.onProcessingEnd()
    }

    suspend fun onSessionEnded() {
        currentInfo.apply {
            resetReplayCache()
            emit(null)
        }
        if (!generateReport) return

        sessionRecorder.onProcessingEnd()

        val scenario = currentScenario ?: throw IllegalStateException("Scenario is not defined")

        var eventsTriggeredCount = 0L
        var conditionsDetectedCount = 0L
        val conditions = mutableListOf<Condition>()

        val eventsReport = currentEvents.map { event ->
            event.conditions.let { conditions.addAll(it) }

            val debugInfo = eventsRecorderMap[event.id.databaseId]?.let { processingRecorder ->
                eventsTriggeredCount += processingRecorder.successCount
                processingRecorder.toProcessingDebugInfo()
            } ?: ProcessingDebugInfo()

            event to debugInfo
        }.sortedBy { it.first.priority }

        val conditionReport = HashMap<Long, Pair<Condition, ConditionProcessingDebugInfo>>()
        conditions.forEach { condition ->
            val debugInfo = conditionsRecorderMap[condition.id.databaseId]?.let { processingRecorder ->
                conditionsDetectedCount += processingRecorder.successCount
                processingRecorder.toConditionProcessingDebugInfo()
            } ?: ConditionProcessingDebugInfo()

            conditionReport[condition.id.databaseId] = condition to debugInfo
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

        _isDebugging.value = false
    }

    fun cancelCurrentProcessing() {
        currProcEvtId = null
        currProcCondId = null
    }
}

/**
 * @return the value corresponding to the given key, or insert and return newValue if such a key is not present in
 * the map.
 */
private fun <K, V> MutableMap<K, V>.getOrDefaultWithPut(key: K, newValue: () -> V): V =
    get(key) ?:let {
        newValue().let {
            put(key, it)
            it
        }
    }

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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine

import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.data.DebugConfigurationLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.debugging.data.DebugReportLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.CounterValuesRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.ImageEventOccurrenceRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.DebugReportOverviewRecorder

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.toList
import kotlin.time.Duration.Companion.milliseconds


/** Engine for the debugging of a scenario processing. */
@Singleton
internal class DebugEngine @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val debugConfigurationLocalDataSource: DebugConfigurationLocalDataSource,
    private val debugReportLocalDataSource: DebugReportLocalDataSource,
) : SmartProcessingListener {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher.limitedParallelism(1))

    private val overviewRecorder: DebugReportOverviewRecorder = DebugReportOverviewRecorder()
    private val imgEventOccurrenceRecorder: ImageEventOccurrenceRecorder = ImageEventOccurrenceRecorder()
    private val counterValuesRecorder: CounterValuesRecorder = CounterValuesRecorder()

    private var isReportEnabled: Boolean = false

    private val _isDebuggingSession: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDebuggingSession: StateFlow<Boolean> = _isDebuggingSession

    private val _lastImageEventFulfilled: MutableStateFlow<DebugLiveImageEventOccurrence?> = MutableStateFlow(null)
    val lastImageEventFulfilled: StateFlow<DebugLiveImageEventOccurrence?> = _lastImageEventFulfilled


    override fun onSessionStarted(scenario: Scenario, imageEvents: List<ImageEvent>, triggerEvents: List<TriggerEvent>) {
        coroutineScopeIo.launch {
            isReportEnabled = debugConfigurationLocalDataSource.isDebugReportEnabled()
            _isDebuggingSession.value = true

            if (isReportEnabled) {
                overviewRecorder.onSessionStart(scenario)
                counterValuesRecorder.onSessionStarted(imageEvents, triggerEvents)
                debugReportLocalDataSource.startReportWrite()
            }
        }
    }

    // Processing started on current frame
    override fun onImageEventsProcessingStarted() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            overviewRecorder.onFrameProcessingStarted()
        }
    }

    // Processing started for current Event
    override fun onImageEventProcessingStarted() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            imgEventOccurrenceRecorder.onImageEventProcessingStarted()
            counterValuesRecorder.onEventProcessingStarted()
        }
    }

    // Condition is processed
    override fun onImageConditionProcessingStarted() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            imgEventOccurrenceRecorder.onImageConditionProcessingStarted()
        }
    }

    override fun onImageConditionProcessingCompleted(result: ProcessedConditionResult.Image) {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            imgEventOccurrenceRecorder.onImageConditionProcessingCompleted(result)
        }
    }

    // Processing ended for current Event
    override fun onImageEventFulfilled(event: ImageEvent, results: List<ProcessedConditionResult.Image>) {
        coroutineScopeIo.launch {
            _lastImageEventFulfilled.update {
                DebugLiveImageEventOccurrence(
                    event = event,
                    imageConditionsResults = results.map { result ->
                        DebugLiveImageConditionResult(
                            condition = result.condition,
                            isFulfilled = result.isFulfilled,
                            isDetected = result.haveBeenDetected,
                            confidenceRate = result.confidenceRate,
                            detectionArea = result.getDetectionArea(),
                        )
                    },
                )
            }

            if (isReportEnabled) {
                overviewRecorder.onEventFulfilled(event)
                debugReportLocalDataSource.writeEventOccurrenceToReport(
                    occurrence = DebugReportEventOccurrence.ImageEvent(
                        eventId = event.id.databaseId,
                        frameNumber = overviewRecorder.frameCount,
                        relativeTimestampMs = overviewRecorder.sessionDurationMs,
                        conditionsResults = imgEventOccurrenceRecorder.imageConditionResults.toList(),
                        counterChanges = counterValuesRecorder.eventCounterChanges.toList(),
                    )
                )
                imgEventOccurrenceRecorder.reset()
            }
        }
    }

    // Processing ended on current frame
    override fun onImageEventsProcessingCompleted() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            overviewRecorder.onFrameProcessingStopped()
        }
    }

    override fun onImageEventsProcessingCancelled() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            overviewRecorder.onFrameProcessingStopped()
            imgEventOccurrenceRecorder.reset()
        }
    }

    override fun onTriggerEventProcessingStarted() {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            counterValuesRecorder.onEventProcessingStarted()
        }
    }

    override fun onTriggerEventFulfilled(event: TriggerEvent, results: List<ProcessedConditionResult.Trigger>) {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch

            overviewRecorder.onEventFulfilled(event)
            debugReportLocalDataSource.writeEventOccurrenceToReport(
                occurrence = DebugReportEventOccurrence.TriggerEvent(
                    eventId = event.id.databaseId,
                    relativeTimestampMs = overviewRecorder.sessionDurationMs,
                    counterChanges = counterValuesRecorder.eventCounterChanges.toList(),
                    conditionsResults = results.map { result ->
                        DebugReportConditionResult.TriggerCondition(
                            conditionId = result.condition.id.databaseId,
                            isFulFilled = result.isFulfilled,
                        )
                    }
                )
            )
        }
    }

    override fun onCounterValueChanged(counterName: String, previousValue: Int, newValue: Int) {
        coroutineScopeIo.launch {
            if (!isReportEnabled) return@launch
            counterValuesRecorder.onCounterValueChanged(counterName, previousValue, newValue)
        }
    }

    override fun onSessionEnded() {
        coroutineScopeIo.launch {
            if (isReportEnabled) {
                debugReportLocalDataSource.stopReportWrite(
                    overview = DebugReportOverview(
                        scenarioId = overviewRecorder.scenarioId,
                        duration = overviewRecorder.sessionDurationMs.milliseconds,
                        frameCount = overviewRecorder.frameCount,
                        averageFrameProcessingDuration = overviewRecorder.averageFrameProcessingDurationMs.milliseconds,
                        imageEventFulfilledCount = overviewRecorder.imageEventFulfilledCount,
                        triggerEventFulfilledCount = overviewRecorder.triggerEventFulfilledCount,
                        counterNames = counterValuesRecorder.counterNames,
                    )
                )

                overviewRecorder.reset()
                counterValuesRecorder.reset()
            }

            _lastImageEventFulfilled.value = null
            _isDebuggingSession.value = false
            isReportEnabled = false
        }
    }

    private fun ProcessedConditionResult.Image.getDetectionArea(): Rect? {
        val pos = position ?: return null
        val halfWidth = condition.area.width() / 2
        val halfHeight = condition.area.height() / 2


        return if (pos.x == 0 && pos.y == 0) Rect()
        else Rect(
            pos.x - halfWidth,
            pos.y - halfHeight,
            pos.x + halfWidth,
            pos.y + halfHeight,
        )
    }
}

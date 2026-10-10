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
import android.util.Size

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.EventType
import com.buzbuz.smartautoclicker.core.processing.domain.HoldReleaseReason
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.data.DebugReportLocalDataSource
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveHeldEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportHoldReleaseReason
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.CounterValuesRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.DebugReportOverviewRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.EventOccurrencesRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.EventStateRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.data.mapping.toCountersInitProtobuf
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.HeldEventRecorder
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder.ScreenConditionOccurrenceRecorder

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val debugReportLocalDataSource: DebugReportLocalDataSource,
    private val overviewRecorder: DebugReportOverviewRecorder,
    private val eventOccurrencesRecorder: EventOccurrencesRecorder,
    private val screenConditionOccurrenceRecorder: ScreenConditionOccurrenceRecorder,
    private val counterValuesRecorder: CounterValuesRecorder,
    private val eventStateRecorder: EventStateRecorder,
    private val heldEventRecorder: HeldEventRecorder,
) : SmartProcessingListener {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher.limitedParallelism(1))

    private var isReportEnabled: Boolean = false
    private var shouldGenerateLiveEvents: Boolean = false

    private val shouldWriteReport: Boolean
        get() = isReportEnabled

    private val _isDebuggingSession: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDebuggingSession: StateFlow<Boolean> = _isDebuggingSession

    private val _lastEventProcessed: MutableStateFlow<DebugLiveEventOccurrence?> = MutableStateFlow(null)
    val lastEventProcessed: StateFlow<DebugLiveEventOccurrence?> = _lastEventProcessed

    private val _liveHold: MutableStateFlow<LiveHold?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val heldEvent: Flow<DebugLiveHeldEvent?> = _liveHold.flatMapLatest { hold ->
        hold?.toLiveHeldEventTicker() ?: flowOf(null)
    }


    override fun onSessionStarted(
        scenario: Scenario,
        counters: List<Counter>,
        generateLiveEvents: Boolean,
        generateReport: Boolean,
    ) {
        coroutineScopeIo.launch {
            isReportEnabled = generateReport
            shouldGenerateLiveEvents = generateLiveEvents
            _isDebuggingSession.value = true

            if (shouldWriteReport) {
                overviewRecorder.onSessionStart(scenario)
                counterValuesRecorder.onSessionStarted(counters)

                debugReportLocalDataSource.startReportWrite()
                writeCountersInitToReport(counters)
            }
        }
    }

    // Processing started on current frame
    override fun onEventsListProcessingStarted(eventType: EventType) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            overviewRecorder.onFrameProcessingStarted(eventType)
        }
    }

    // Processing started for current Event
    override fun onEventProcessingStarted(event: Event) {
        coroutineScopeIo.launch {
            eventOccurrencesRecorder.onEventProcessingStarted()
            screenConditionOccurrenceRecorder.onEventProcessingStarted()

            if (!shouldWriteReport) return@launch
            counterValuesRecorder.onEventProcessingStarted()
            eventStateRecorder.onEventProcessingStarted()
        }
    }

    override fun onEventProcessingCompleted(
        event: Event,
        fulfilled: Boolean,
        results: List<ProcessedConditionResult>,
        isHeld: Boolean,
    ) {
        if (isHeld) {
            onHeldEventChecked(event, fulfilled, results)
            return
        }

        coroutineScopeIo.launch {
            if (fulfilled) eventOccurrencesRecorder.onEventFulfilled(event)
            if (!shouldGenerateLiveEvents) return@launch

            _lastEventProcessed.update {
                @Suppress("UNCHECKED_CAST")
                when (event) {
                    is ScreenEvent -> getLiveScreenEventOccurrence(
                        event = event,
                        fulfilled = fulfilled,
                        results = results as List<ProcessedConditionResult.Screen>,
                    )
                    is TriggerEvent -> getLiveTriggerEventOccurrence(
                        event = event,
                        fulfilled = fulfilled,
                        results = results as List<ProcessedConditionResult.Trigger>,
                    )
                }
            }
        }
    }

    // Processing ended for current Event
    override fun onEventActionsExecuted(event: Event, results: List<ProcessedConditionResult>) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            overviewRecorder.onActionsExecuted(event)

            @Suppress("UNCHECKED_CAST")
            when (event) {
                is ScreenEvent -> {
                    writeImageEventToReport(event)
                    screenConditionOccurrenceRecorder.reset()
                }

                is TriggerEvent ->
                    writeTriggerEventToReport(event, results as List<ProcessedConditionResult.Trigger>)
            }
        }
    }

    override fun onHeldEventChanged(heldEvent: Event?) {
        val timestampMs = System.currentTimeMillis()
        coroutineScopeIo.launch {
            heldEventRecorder.onHeldEventChanged(heldEvent, timestampMs)
            if (!shouldGenerateLiveEvents) return@launch

            _liveHold.value = heldEvent?.let { LiveHold(event = it, startTimestampMs = timestampMs) }
        }
    }

    private fun onHeldEventChecked(event: Event, held: Boolean, results: List<ProcessedConditionResult>) {
        val timestampMs = System.currentTimeMillis()
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            @Suppress("UNCHECKED_CAST")
            when (event) {
                is ScreenEvent -> {
                    writeScreenHeldEventCheckToReport(event, held, timestampMs)
                    screenConditionOccurrenceRecorder.reset()
                }

                is TriggerEvent -> writeTriggerHeldEventCheckToReport(
                    event = event,
                    held = held,
                    timestampMs = timestampMs,
                    results = results as List<ProcessedConditionResult.Trigger>,
                )
            }
        }
    }

    override fun onHeldEventReleased(event: Event, reason: HoldReleaseReason) {
        // Already written in the report by the held event check, along with its conditions results
        if (reason == HoldReleaseReason.Unfulfilled) return

        val timestampMs = System.currentTimeMillis()
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch
            writeHeldEventReleaseToReport(event, reason.toReportReleaseReason(), timestampMs)
        }
    }

    // Processing ended on current frame
    override fun onEventsProcessingCompleted(eventType: EventType) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            overviewRecorder.onFrameProcessingStopped(eventType)
        }
    }

    override fun onEventsProcessingCancelled() {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            overviewRecorder.onFrameProcessingStopped(null)
            screenConditionOccurrenceRecorder.reset()
            eventOccurrencesRecorder.reset()
        }
    }

    // Image Condition is processed
    override fun onScreenConditionProcessingStarted() {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            screenConditionOccurrenceRecorder.onImageConditionProcessingStarted()
        }
    }

    // Called anyway,even if not matched
    override fun onScreenConditionProcessingCompleted(result: ProcessedConditionResult.Screen) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch

            screenConditionOccurrenceRecorder.onImageConditionProcessingCompleted(result)
        }
    }

    override fun onCounterValueChanged(counterName: String, previousValue: Double, newValue: Double) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch
            counterValuesRecorder.onCounterValueChanged(counterName, previousValue, newValue)
        }
    }

    override fun onEventStateChanged(event: Event, newValue: Boolean) {
        coroutineScopeIo.launch {
            if (!shouldWriteReport) return@launch
            eventStateRecorder.onEventStateChanged(event, newValue)
        }
    }

    override fun onSessionEnded() {
        coroutineScopeIo.launch {
            if (shouldWriteReport) {
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
                eventStateRecorder.reset()
            }

            eventOccurrencesRecorder.reset()
            screenConditionOccurrenceRecorder.reset()
            heldEventRecorder.reset()
            _lastEventProcessed.value = null
            _liveHold.value = null
            _isDebuggingSession.value = false
            isReportEnabled = false
            shouldGenerateLiveEvents = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLiveScreenEventOccurrence(event: Event, fulfilled: Boolean, results: List<ProcessedConditionResult.Screen>): DebugLiveEventOccurrence.Screen =
        DebugLiveEventOccurrence.Screen(
            event = event as ScreenEvent,
            fulfilled = fulfilled,
            fulfilledCount = eventOccurrencesRecorder.getEventOccurrences(event.id.databaseId),
            processingDurationMs = eventOccurrencesRecorder.getLastEventDurationMs(),
            timestamp = System.currentTimeMillis(),
            conditionsResults = results.map { result ->
                DebugLiveEventConditionResult.Screen(
                    condition = result.condition,
                    isFulfilled = result.isFulfilled,
                    isDetected = result.haveBeenDetected,
                    confidenceRate = result.confidenceRate,
                    detectionArea = result.getDetectionArea(),
                )
            },
        )

    @Suppress("UNCHECKED_CAST")
    private fun getLiveTriggerEventOccurrence(event: Event, fulfilled: Boolean, results: List<ProcessedConditionResult.Trigger>): DebugLiveEventOccurrence.Trigger =
        DebugLiveEventOccurrence.Trigger(
            event = event as TriggerEvent,
            fulfilled = fulfilled,
            fulfilledCount = eventOccurrencesRecorder.getEventOccurrences(event.id.databaseId),
            processingDurationMs = eventOccurrencesRecorder.getLastEventDurationMs(),
            timestamp = System.currentTimeMillis(),
            conditionsResults = results.map { result ->
                DebugLiveEventConditionResult.Trigger(
                    condition = result.condition,
                    isFulfilled = result.isFulfilled,
                )
            },
        )

    private suspend fun writeCountersInitToReport(counters: List<Counter>) {
        debugReportLocalDataSource.writeMessageToReport(counters.toCountersInitProtobuf())
    }

    private suspend fun writeImageEventToReport(event: ScreenEvent) {
        debugReportLocalDataSource.writeEventOccurrenceToReport(
            occurrence = DebugReportEventOccurrence.ScreenEvent(
                eventId = event.id.databaseId,
                frameNumber = overviewRecorder.frameCount,
                relativeTimestampMs = overviewRecorder.sessionDurationMs,
                conditionsResults = screenConditionOccurrenceRecorder.screenConditionResults.toList(),
                counterChanges = counterValuesRecorder.eventCounterChanges.toList(),
                eventStateChanges = eventStateRecorder.changes.toList(),
            )
        )
    }

    private suspend fun writeTriggerEventToReport(event: TriggerEvent, results: List<ProcessedConditionResult.Trigger>) {
        debugReportLocalDataSource.writeEventOccurrenceToReport(
            occurrence = DebugReportEventOccurrence.TriggerEvent(
                eventId = event.id.databaseId,
                relativeTimestampMs = overviewRecorder.sessionDurationMs,
                counterChanges = counterValuesRecorder.eventCounterChanges.toList(),
                eventStateChanges = eventStateRecorder.changes.toList(),
                conditionsResults = results.toReportConditionResults(),
            )
        )
    }

    private suspend fun writeScreenHeldEventCheckToReport(event: ScreenEvent, held: Boolean, timestampMs: Long) {
        debugReportLocalDataSource.writeEventOccurrenceToReport(
            occurrence = DebugReportEventOccurrence.HeldEventCheck.Screen(
                eventId = event.id.databaseId,
                frameNumber = overviewRecorder.frameCount,
                relativeTimestampMs = overviewRecorder.sessionDurationMs,
                conditionsResults = screenConditionOccurrenceRecorder.screenConditionResults.toList(),
                releaseReason = if (held) null else DebugReportHoldReleaseReason.UNFULFILLED,
                holdDurationMs = heldEventRecorder.getHoldDurationMs(event, timestampMs),
            )
        )
    }

    private suspend fun writeTriggerHeldEventCheckToReport(
        event: TriggerEvent,
        held: Boolean,
        timestampMs: Long,
        results: List<ProcessedConditionResult.Trigger>,
    ) {
        debugReportLocalDataSource.writeEventOccurrenceToReport(
            occurrence = DebugReportEventOccurrence.HeldEventCheck.Trigger(
                eventId = event.id.databaseId,
                relativeTimestampMs = overviewRecorder.sessionDurationMs,
                conditionsResults = results.toReportConditionResults(),
                releaseReason = if (held) null else DebugReportHoldReleaseReason.UNFULFILLED,
                holdDurationMs = heldEventRecorder.getHoldDurationMs(event, timestampMs),
            )
        )
    }

    /** Write the release of a hold that ended without any condition check. */
    private suspend fun writeHeldEventReleaseToReport(
        event: Event,
        releaseReason: DebugReportHoldReleaseReason,
        timestampMs: Long,
    ) {
        val holdDurationMs = heldEventRecorder.getHoldDurationMs(event, timestampMs)
        debugReportLocalDataSource.writeEventOccurrenceToReport(
            occurrence = when (event) {
                is ScreenEvent -> DebugReportEventOccurrence.HeldEventCheck.Screen(
                    eventId = event.id.databaseId,
                    // TODO: Remove the +1 once the ScenarioProcessor loop is rewritten. A release outside of a frame
                    //  processing (user touch, event disabled) is detected before the next frame starts.
                    frameNumber =
                        if (overviewRecorder.isProcessingFrame) overviewRecorder.frameCount
                        else overviewRecorder.frameCount + 1,
                    relativeTimestampMs = overviewRecorder.sessionDurationMs,
                    conditionsResults = emptyList(),
                    releaseReason = releaseReason,
                    holdDurationMs = holdDurationMs,
                )
                is TriggerEvent -> DebugReportEventOccurrence.HeldEventCheck.Trigger(
                    eventId = event.id.databaseId,
                    relativeTimestampMs = overviewRecorder.sessionDurationMs,
                    conditionsResults = emptyList(),
                    releaseReason = releaseReason,
                    holdDurationMs = holdDurationMs,
                )
            }
        )
    }
}

/** An event holding a pointer, as displayed by the live debugging. */
private data class LiveHold(
    val event: Event,
    val startTimestampMs: Long,
)

/** Emits the hold duration each time a new second of hold is reached. */
private fun LiveHold.toLiveHeldEventTicker(): Flow<DebugLiveHeldEvent> = flow {
    while (true) {
        val holdDurationMs = (System.currentTimeMillis() - startTimestampMs).coerceAtLeast(0L)
        emit(DebugLiveHeldEvent(event = event, holdDurationS = holdDurationMs / 1_000L))
        delay((1_000L - holdDurationMs % 1_000L).milliseconds)
    }
}

private fun List<ProcessedConditionResult.Trigger>.toReportConditionResults(): List<DebugReportConditionResult.TriggerCondition> =
    map { result ->
        DebugReportConditionResult.TriggerCondition(
            conditionId = result.condition.id.databaseId,
            isFulFilled = result.isFulfilled,
        )
    }

private fun ProcessedConditionResult.Screen.getDetectionArea(): Rect? {
    val pos = position ?: return null
    val size = size ?: return null

    val halfSize = when (val cond = condition) {
        is ScreenCondition.Color ->
            Size(cond.detectionArea.width() / 2, cond.detectionArea.height() / 2)

        is ScreenCondition.Image,
        is ScreenCondition.Text,
        is ScreenCondition.Number ->
            Size(size.x / 2,  size.y / 2)
    }

    return if (pos.x == 0 && pos.y == 0) Rect()
    else Rect(
        pos.x - halfSize.width,
        pos.y - halfSize.height,
        pos.x + halfSize.width,
        pos.y + halfSize.height,
    )
}
private fun HoldReleaseReason.toReportReleaseReason(): DebugReportHoldReleaseReason =
    when (this) {
        HoldReleaseReason.Unfulfilled -> DebugReportHoldReleaseReason.UNFULFILLED
        HoldReleaseReason.Interrupted -> DebugReportHoldReleaseReason.INTERRUPTED
        HoldReleaseReason.EventDisabled -> DebugReportHoldReleaseReason.EVENT_DISABLED
        HoldReleaseReason.OtherEvent -> DebugReportHoldReleaseReason.OTHER_EVENT
        HoldReleaseReason.ScreenRotation -> DebugReportHoldReleaseReason.SCREEN_ROTATION
        HoldReleaseReason.ScenarioStopped -> DebugReportHoldReleaseReason.SCENARIO_STOPPED
    }

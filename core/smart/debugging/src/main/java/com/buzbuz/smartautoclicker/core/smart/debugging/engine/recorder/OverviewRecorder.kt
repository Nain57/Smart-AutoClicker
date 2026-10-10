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
package com.buzbuz.smartautoclicker.core.smart.debugging.engine.recorder

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.EventType
import javax.inject.Inject

internal class DebugReportOverviewRecorder @Inject constructor() {

    private val sessionDurationRecorder = DurationRecorder()
    private val frameProcessingDurationRecorder = DurationRecorder()
    private var totalFrameProcessingDurationMs: Long = 0

    val sessionDurationMs: Long
        get() = sessionDurationRecorder.durationMs()
    val averageFrameProcessingDurationMs: Long
        get() =
            if (frameCount > 0) totalFrameProcessingDurationMs / frameCount
            else 0

    var scenarioId: Long = -1
        private set
    var frameCount: Long = 0
        private set
    var isProcessingFrame: Boolean = false
        private set
    private var lastProcessedEventType: EventType? = null
    var imageEventFulfilledCount: Int = 0
        private set
    var triggerEventFulfilledCount: Int = 0
        private set


    fun onSessionStart(scenario: Scenario) {
        scenarioId = scenario.id.databaseId
        sessionDurationRecorder.start()
    }

    /** @param eventType the type of the events list starting its processing on the current frame. */
    fun onFrameProcessingStarted(eventType: EventType) {
        // The screen events are processed on the same frame as the trigger events processed right before them
        if (eventType == EventType.Trigger || lastProcessedEventType != EventType.Trigger) frameCount += 1
        lastProcessedEventType = eventType
        isProcessingFrame = true
        frameProcessingDurationRecorder.start()
    }

    fun onActionsExecuted(event: Event) {
        when (event) {
            is ScreenEvent -> imageEventFulfilledCount += 1
            is TriggerEvent -> triggerEventFulfilledCount += 1
        }
    }

    /** @param eventType the type of the events list that completed its processing, or null if it was cancelled. */
    fun onFrameProcessingStopped(eventType: EventType?) {
        totalFrameProcessingDurationMs += frameProcessingDurationRecorder.durationMs()
        frameProcessingDurationRecorder.reset()
        isProcessingFrame = false
        // Only a completed trigger events list can be followed by screen events on the same frame
        if (eventType != EventType.Trigger) lastProcessedEventType = null
    }

    fun reset() {
        sessionDurationRecorder.reset()
        scenarioId = 0
        frameCount = 0
        isProcessingFrame = false
        lastProcessedEventType = null
        totalFrameProcessingDurationMs = 0
        imageEventFulfilledCount = 0
        triggerEventFulfilledCount = 0
    }
}

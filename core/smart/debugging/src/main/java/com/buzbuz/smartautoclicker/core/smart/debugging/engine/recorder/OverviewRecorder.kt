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
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import javax.inject.Inject

internal class DebugReportOverviewRecorder @Inject constructor() {

    private val sessionDurationRecorder = DurationRecorder()
    private val frameProcessingDurationRecorder = DurationRecorder()
    private var totalFrameProcessingDurationMs: Long = 0

    val sessionDurationMs: Long
        get() = sessionDurationRecorder.durationMs()
    val averageFrameProcessingDurationMs: Long
        get() = totalFrameProcessingDurationMs / frameCount

    var scenarioId: Long = -1
        private set
    var frameCount: Long = 0
        private set
    var imageEventFulfilledCount: Int = 0
        private set
    var triggerEventFulfilledCount: Int = 0
        private set


    fun onSessionStart(scenario: Scenario) {
        scenarioId = scenario.id.databaseId
        sessionDurationRecorder.start()
    }

    fun onFrameProcessingStarted() {
        frameCount += 1
        frameProcessingDurationRecorder.start()
    }

    fun onEventFulfilled(event: Event) {
        when (event) {
            is ImageEvent -> imageEventFulfilledCount += 1
            is TriggerEvent -> triggerEventFulfilledCount += 1
        }
    }

    fun onFrameProcessingStopped() {
        totalFrameProcessingDurationMs += frameProcessingDurationRecorder.durationMs()
        frameProcessingDurationRecorder.reset()
    }

    fun reset() {
        sessionDurationRecorder.reset()
        scenarioId = 0
        frameCount = 0
        totalFrameProcessingDurationMs = 0
        imageEventFulfilledCount = 0
        triggerEventFulfilledCount = 0
    }
}

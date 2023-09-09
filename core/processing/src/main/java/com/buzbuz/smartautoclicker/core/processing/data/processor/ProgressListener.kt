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
package com.buzbuz.smartautoclicker.core.processing.data.processor

import android.content.Context
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

interface ProgressListener {

    suspend fun onSessionStarted(context: Context, scenario: Scenario, events: List<Event>)

    suspend fun onImageProcessingStarted()

    suspend fun onEventProcessingStarted(event: Event)

    suspend fun onConditionProcessingStarted(condition: Condition)

    suspend fun onConditionProcessingCompleted(detectionResult: DetectionResult)

    suspend fun onEventProcessingCompleted(
        isEventMatched: Boolean,
        event: Event?,
        condition: Condition?,
        isDetected: Boolean?,
        position: Point?,
        confidenceRate: Double?,
    )

    suspend fun onImageProcessingCompleted()

    suspend fun onSessionEnded()

    suspend fun cancelCurrentProcessing()

    suspend fun cancelCurrentConditionProcessing()
}

internal suspend fun ProgressListener.onEventProcessingCompleted(event: Event, isMatched: Boolean, result: ConditionProcessingResult?) {
    onEventProcessingCompleted(
        isMatched,
        event,
        result?.condition,
        result?.isDetected,
        result?.position,
        result?.confidenceRate
    )
}
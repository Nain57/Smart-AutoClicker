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

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

interface ProgressListener {

    fun onSessionStarted(context: Context, scenario: Scenario, events: List<Event>)

    fun onImageProcessingStarted()

    fun onEventProcessingStarted(event: Event)

    fun onConditionProcessingStarted(condition: Condition)

    fun onConditionProcessingCompleted(detectionResult: DetectionResult)

    suspend fun onEventProcessingCompleted(
        isEventMatched: Boolean,
        event: Event?,
        condition: Condition?,
        result: DetectionResult?,
    )

    fun onImageProcessingCompleted()

    suspend fun onSessionEnded()

    fun cancelCurrentProcessing()
}

internal suspend fun ProgressListener.onEventProcessingCompleted(result: ProcessorResult) =
    onEventProcessingCompleted(result.eventMatched, result.event, result.condition, result.detectionResult)
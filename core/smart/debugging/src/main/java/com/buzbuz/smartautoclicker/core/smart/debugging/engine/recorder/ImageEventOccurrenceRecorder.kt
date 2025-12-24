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

import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult


internal class ImageEventOccurrenceRecorder {

    private val conditionDurationRecorder: DurationRecorder = DurationRecorder()

    private val _imageConditionResults: MutableList<DebugReportConditionResult.ImageCondition> = mutableListOf()
    val imageConditionResults: List<DebugReportConditionResult.ImageCondition> = _imageConditionResults


    fun onImageEventProcessingStarted() {
        reset()
    }

    fun onImageConditionProcessingStarted() {
        conditionDurationRecorder.start()
    }

    fun onImageConditionProcessingCompleted(result: ProcessedConditionResult.Image) {
        _imageConditionResults.add(
            DebugReportConditionResult.ImageCondition(
                conditionId = result.condition.id.databaseId,
                isFulFilled = result.isFulfilled,
                detectionDurationMs = conditionDurationRecorder.durationMs(),
                confidenceRate = result.confidenceRate,
            )
        )

        conditionDurationRecorder.reset()
    }

    fun reset() {
        conditionDurationRecorder.reset()
        _imageConditionResults.clear()
    }
}
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

import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.domain.model.scenario.Scenario

data class DebugReport internal constructor(
    val scenario: Scenario,
    val sessionInfo: ProcessingDebugInfo,
    val imageProcessedInfo: ProcessingDebugInfo,
    val eventsTriggeredCount: Long,
    val eventsProcessedInfo: List<Pair<Event, ProcessingDebugInfo>>,
    val conditionsDetectedCount: Long,
    val conditionsProcessedInfo: Map<Long, Pair<Condition, ConditionProcessingDebugInfo>>,
)

data class ProcessingDebugInfo internal constructor(
    val processingCount: Long = 0,
    val successCount: Long = 0,
    val totalProcessingTimeMs: Long = 0,
    val avgProcessingTimeMs: Long = 0,
    val minProcessingTimeMs: Long = 0,
    val maxProcessingTimeMs: Long = 0,
)

data class ConditionProcessingDebugInfo internal constructor(
    val processingCount: Long = 0,
    val successCount: Long = 0,
    val totalProcessingTimeMs: Long = 0,
    val avgProcessingTimeMs: Long = 0,
    val minProcessingTimeMs: Long = 0,
    val maxProcessingTimeMs: Long = 0,
    val avgConfidenceRate: Double = 0.0,
    val minConfidenceRate: Double = 0.0,
    val maxConfidenceRate: Double = 0.0,
)


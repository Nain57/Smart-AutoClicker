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

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event

internal class ProcessingResults(events: List<Event>) {

    private val eventsResults: MutableMap<Long, EventProcessingResults> =
        mutableMapOf<Long, EventProcessingResults>().apply {
            events.forEach {
                put(it.id.databaseId, EventProcessingResults(it))
            }
        }

    fun addResult(condition: Condition, isDetected: Boolean, position: Point, confRate: Double) {
        eventsResults[condition.eventId.databaseId]
            ?.addResult(condition.id.databaseId, isDetected, condition.shouldBeDetected, position, confRate)
    }

    fun clearResults() {
        eventsResults.values.forEach { eventResults ->
            eventResults.clearResults()
        }
    }

    fun getFirstMatchResult(): ConditionProcessingResult? {
        eventsResults.values.forEach { eventsResults ->
            return eventsResults.getFirstMatchResult() ?: return@forEach
        }
        return null
    }

    fun getResult(eventDbId: Long, conditionDbId: Long): ConditionProcessingResult? =
        eventsResults[eventDbId]?.getResult(conditionDbId)
}

private class EventProcessingResults(event: Event) {

    private val conditionsResults: MutableMap<Long, ConditionProcessingResult> =
        mutableMapOf<Long, ConditionProcessingResult>().apply {
            event.conditions.forEach { put(it.id.databaseId, ConditionProcessingResult(it)) }
        }

    fun addResult(conditionId: Long, detected: Boolean, shouldBe: Boolean, pos: Point, confRate: Double) {
        conditionsResults[conditionId]?.apply {
            isDetected = detected
            shouldBeDetected = shouldBe
            position.set(pos.x, pos.y)
            confidenceRate = confRate
        }
    }

    fun clearResults() {
        conditionsResults.values.forEach { result ->
            result.apply {
                isDetected = false
                shouldBeDetected = false
                position.set(0, 0)
                confidenceRate = 0.0
            }
        }
    }

    fun getFirstMatchResult(): ConditionProcessingResult? =
        conditionsResults.values.find { conditionResults ->
            conditionResults.isDetected && conditionResults.shouldBeDetected
        }

    fun getResult(conditionDbId: Long) =
        conditionsResults[conditionDbId]
}

internal data class ConditionProcessingResult(
    val condition: Condition,
    var isDetected: Boolean = false,
    var shouldBeDetected: Boolean = false,
    val position: Point = Point(),
    var confidenceRate: Double = 0.0
)
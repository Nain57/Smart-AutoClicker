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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.result.ProcessedConditionResult

interface DebuggingListener {
    fun onSessionStarted(scenario: Scenario) = Unit

    fun onTriggerEventFulfilled(event: TriggerEvent, results: List<ProcessedConditionResult.Trigger>) = Unit

    fun onImageEventsProcessingStarted() = Unit
    fun onImageEventProcessingStarted() = Unit

    fun onImageConditionProcessingStarted() = Unit
    fun onImageConditionProcessingCompleted(result: ProcessedConditionResult.Image) = Unit

    fun onImageEventFulfilled(event: ImageEvent, results: List<ProcessedConditionResult.Image>) = Unit

    fun onImageEventsProcessingCompleted() = Unit
    fun onImageEventsProcessingCancelled() = Unit

    fun onSessionEnded() = Unit
}
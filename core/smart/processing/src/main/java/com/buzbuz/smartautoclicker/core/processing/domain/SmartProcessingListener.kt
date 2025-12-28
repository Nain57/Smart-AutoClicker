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
package com.buzbuz.smartautoclicker.core.processing.domain

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult


/** Listener upon smart scenario processing.*/
interface SmartProcessingListener {

    /**
     * The processing session have started.
     *
     * @param scenario the [Scenario] running for the processing session.
     * @param imageEvents the list of [ImageEvent] to be processed for this scenario.
     * @param triggerEvents the list of [TriggerEvent] to be processed for this scenario.
     */
    fun onSessionStarted(scenario: Scenario, imageEvents: List<ImageEvent>, triggerEvents: List<TriggerEvent>) = Unit

    /** The processing of an [TriggerEvent] has begun. */
    fun onTriggerEventProcessingStarted() = Unit

    /**
     * A [TriggerEvent] have been fulfilled.
     *
     * @param event the event fulfilled
     * @param results the results for each [TriggerCondition] processed for the event?
     */
    fun onTriggerEventFulfilled(event: TriggerEvent, results: List<ProcessedConditionResult.Trigger>) = Unit

    /** The processing of the [ImageEvent] list on a new screen frame has begun. */
    fun onImageEventsProcessingStarted() = Unit

    /** The processing of an [ImageEvent] on the current screen frame has begun. */
    fun onImageEventProcessingStarted() = Unit

    /** The processing of an [ImageCondition] for the current [ImageEvent] has begun. */
    fun onImageConditionProcessingStarted() = Unit

    /**
     * The processing of an [ImageCondition] for the current [ImageEvent] has completed.
     * This will be called even if the condition is not fulfilled.
     *
     * @param result the result of the detection for the processed condition.
     */
    fun onImageConditionProcessingCompleted(result: ProcessedConditionResult.Image) = Unit

    /**
     * A [ImageEvent] have been fulfilled.
     *
     * @param event the event fulfilled
     * @param results the results for each [ImageCondition] processed for the event?
     */
    fun onImageEventFulfilled(event: ImageEvent, results: List<ProcessedConditionResult.Image>) = Unit

    /** The processing of the [ImageEvent] list on a new screen frame is complete. */
    fun onImageEventsProcessingCompleted() = Unit

    /**
     * The processing of the [ImageEvent] list has been cancelled.
     * This can be caused by the scenario being stopped, or when the device has been rotated.
     */
    fun onImageEventsProcessingCancelled() = Unit

    /**
     * The value of a counter have changed.
     *
     * @param counterName the name of the counter.
     * @param previousValue the value of the counter before the change
     * @param newValue the value of the counter after the change.
     */
    fun onCounterValueChanged(counterName: String, previousValue: Int, newValue: Int)

    /** The processing session have ended.*/
    fun onSessionEnded() = Unit
}
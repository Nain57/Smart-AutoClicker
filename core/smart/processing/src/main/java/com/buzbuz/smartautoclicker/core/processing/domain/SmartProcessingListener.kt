/*
 * Copyright (C) 2026 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
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
     * @param isAnElementTry tells if this session is a complete scenario or a try made during scenario creation.
     */
    fun onSessionStarted(
        scenario: Scenario,
        imageEvents: List<ImageEvent>,
        triggerEvents: List<TriggerEvent>,
        isAnElementTry: Boolean = false,
    ) = Unit


    /** The processing of the [ImageEvent] list on a new screen frame has begun. */
    fun onEventsListProcessingStarted(eventType: EventType) = Unit

    /** The processing of an [Event] on the current screen frame has begun. */
    fun onEventProcessingStarted(event: Event) = Unit

    /**
     * A [Event] conditions have been processed but its actions haven't been executed.
     *
     * @param event the event fulfilled
     * @param fulfilled true if event conditions are fulfilled, false if not.
     * @param results the results for each Condition processed for the event
     */
    fun onEventProcessingCompleted(event: Event, fulfilled: Boolean, results: List<ProcessedConditionResult>)

    /**
     * A [Event] have been fulfilled and its actions are done being executed..
     *
     * @param event the event fulfilled
     * @param results the results for each Condition processed for the event?
     */
    fun onEventActionsExecuted(event: Event, results: List<ProcessedConditionResult>) = Unit

    /** The processing of the [Event] list on a new screen frame is complete. */
    fun onEventsProcessingCompleted(eventType: EventType) = Unit

    /**
     * The processing of the [Event] list has been cancelled.
     * This can be caused by the scenario being stopped, or when the device has been rotated.
     */
    fun onEventsProcessingCancelled() = Unit

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
     * The value of a counter have changed.
     *
     * @param counterName the name of the counter.
     * @param previousValue the value of the counter before the change
     * @param newValue the value of the counter after the change.
     */
    fun onCounterValueChanged(counterName: String, previousValue: Int, newValue: Int) = Unit

    /**
     * The state of an event has been changed.
     *
     * @param event the event that have been changed.
     * @param newValue the value of the event state after the change.
     */
    fun onEventStateChanged(event: Event, newValue: Boolean) = Unit

    /** The processing session have ended.*/
    fun onSessionEnded() = Unit
}

enum class EventType {
    Image,
    Trigger
}
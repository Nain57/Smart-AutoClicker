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

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.model.ProcessedConditionResult


/** Listener upon smart scenario processing.*/
interface SmartProcessingListener {

    /**
     * The processing session have started.
     *
     * @param scenario the [Scenario] running for the processing session.
     * @param counters the list of [Counter] to be processed for this scenario.
     * @param generateLiveEvents tells if the live debugging events should be generated.
     * @param generateReport tells if the debug report should be generated.
     */
    fun onSessionStarted(
        scenario: Scenario,
        counters: List<Counter>,
        generateLiveEvents: Boolean,
        generateReport: Boolean,
    ) = Unit


    /** The processing of the [ScreenEvent] list on a new screen frame has begun. */
    fun onEventsListProcessingStarted(eventType: EventType) = Unit

    /**
     * The event holding a pointer on the screen has changed.
     * This is called when a hold starts, when it is taken over by another event, and when the pointer is released,
     * whatever the reason (conditions unfulfilled, event disabled, lifted by the user, dropped by the system...).
     *
     * @param heldEvent the event now holding the pointer, or null if nothing is held anymore.
     */
    fun onHeldEventChanged(heldEvent: Event?) = Unit

    /**
     * The pointer held by an event has been released.
     * Always called before the [onHeldEventChanged] notifying the end of this hold.
     *
     * @param event the event that was holding the pointer.
     * @param reason the reason of the release.
     */
    fun onHeldEventReleased(event: Event, reason: HoldReleaseReason) = Unit


    /** The processing of an [Event] on the current screen frame has begun. */
    fun onEventProcessingStarted(event: Event) = Unit

    /**
     * A [Event] conditions have been processed but its actions haven't been executed.
     *
     * @param event the event fulfilled
     * @param fulfilled true if event conditions are fulfilled, false if not.
     * @param results the results for each Condition processed for the event
     * @param isHeld true if the event is holding a pointer on the screen and this processing is the verification of
     * its hold. In this case, [fulfilled] tells if the pointer is kept held, or if it is released.
     */
    fun onEventProcessingCompleted(
        event: Event,
        fulfilled: Boolean,
        results: List<ProcessedConditionResult>,
        isHeld: Boolean = false,
    )

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

    /** The processing of an [ScreenCondition] for the current [ScreenEvent] has begun. */
    fun onScreenConditionProcessingStarted() = Unit

    /**
     * The processing of an [ScreenCondition.Image] for the current [ScreenEvent] has completed.
     * This will be called even if the condition is not fulfilled.
     *
     * @param result the result of the detection for the processed condition.
     */
    fun onScreenConditionProcessingCompleted(result: ProcessedConditionResult.Screen) = Unit

    /**
     * The value of a counter have changed.
     *
     * @param counterName the name of the counter.
     * @param previousValue the value of the counter before the change
     * @param newValue the value of the counter after the change.
     */
    fun onCounterValueChanged(counterName: String, previousValue: Double, newValue: Double) = Unit

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
    Screen,
    Trigger
}

/** The reason of the release of a pointer held by an event. */
enum class HoldReleaseReason {
    /** The conditions of the held event are no longer fulfilled. */
    Unfulfilled,
    /** All the actions of the held event have been executed. */
    ActionsCompleted,
    /** The defined hold duration has elapsed. */
    DurationElapsed,
    /** The pointer has been lifted outside of the scenario, most likely by a user interaction with the screen. */
    Interrupted,
    /** The held event has been disabled. */
    EventDisabled,
    /** The gesture of another event has taken over or ended the hold. */
    OtherEvent,
    /** The screen orientation has changed. */
    ScreenRotation,
    /** The scenario has been stopped. */
    ScenarioStopped,
}
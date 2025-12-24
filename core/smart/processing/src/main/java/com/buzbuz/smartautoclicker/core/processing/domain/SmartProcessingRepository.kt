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

import android.content.Context
import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/** Handles the smart scenario processing. */
interface SmartProcessingRepository : Dumpable {

    /** Unique identifier of the scenario that will be/is processed. Set with [setScenarioId]. */
    val scenarioId: StateFlow<Identifier?>

    /**
     * Tells if the detection can be started or not.
     * It requires at least one event enabled on start in the current scenario to be started.
     */
    val canStartDetection: Flow<Boolean>

    /** State of the scenario processing.*/
    val detectionState: Flow<DetectionState>


    /** @return the unique identifier of the scenario that will be/is processed. */
    fun getScenarioId(): Identifier?

    /** @return true if the processing is currently running ([DetectionState.DETECTING]), false if not. */
    fun isRunning(): Boolean

    /**
     * Set the scenario to be processed.
     *
     * @param identifier the unique identifier for the scenario.
     * @param markAsUsed true if the scenario should be marked as used in database. Useful to filter try scenarios and
     * other temp scenario use cases.
     */
    fun setScenarioId(identifier: Identifier, markAsUsed: Boolean = false)

    /**
     * Set the callback upon Android Media Projection errors.
     *
     * @param handler the callback to be called.
     */
    fun setProjectionErrorHandler(handler: () -> Unit)

    /**
     * Start the screen recording.
     * This should be called after requesting the media projection permission to the user. Ignored if the detection
     * state is different than [DetectionState.INACTIVE].
     *
     * @param resultCode the result code provided by the media projection permission request.
     * @param data the result data provided by the media projection permission request.
     */
    fun startScreenRecord(resultCode: Int, data: Intent)

    /**
     * Start the processing for the current [Scenario].
     * Ignored if the state is different than [DetectionState.RECORDING].
     *
     * @param context the Android context.
     * @param autoStopDuration the duration after which the detection will be stopped.
     */
    suspend fun startDetection(context: Context, autoStopDuration: Duration? = null)

    /**
     * Stop the processing for the current [Scenario].
     * Ignored if the state is different than [DetectionState.DETECTING].
     */
    fun stopDetection()

    /**
     * Stop the processing for the current [Scenario].
     * Ignored if the state is [DetectionState.INACTIVE]. Also stops the processing if the state is
     * [DetectionState.DETECTING].
     */
    fun stopScreenRecord()

    /**
     * Creates a dedicated scenario containing only the [ImageEvent] to be tested.
     *
     * @param context the Android context.
     * @param scenario the original scenario containing the event to test
     * @param event the event to be tested.
     */
    fun tryEvent(context: Context, scenario: Scenario, event: ImageEvent)

    /**
     * Creates a dedicated scenario containing only an [ImageEvent] with the [ImageCondition] to be tested.
     *
     * @param context the Android context.
     * @param scenario the original scenario containing the condition to test
     * @param condition the condition to be tested.
     */
    fun tryImageCondition(context: Context, scenario: Scenario, condition: ImageCondition)

    /**
     * Creates a dedicated scenario containing only an Event with the [Action] to be tested.
     *
     * @param context the Android context.
     * @param scenario the original scenario containing the condition to test
     * @param action the action to be tested.
     */
    fun tryAction(context: Context,  scenario: Scenario, action: Action)
}
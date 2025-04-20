/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain

import android.graphics.Bitmap

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.Flow

/**
 * Repository storing the information about the scenarios and their events.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 */
interface IRepository {

    /** The list of scenarios. */
    val scenarios: Flow<List<Scenario>>
    /** All image events from all scenarios.  */
    val allScreenEvents: Flow<List<ScreenEvent>>
    /** All trigger events from all scenarios. */
    val allTriggerEvents: Flow<List<TriggerEvent>>
    /** All conditions from all events. */
    val allConditions: Flow<List<Condition>>
    /** All actions from all events. */
    val allActions: Flow<List<Action>>

    /**
     * Add a new scenario.
     *
     * @param scenario the scenario to add.
     * @return the identifier for the newly add scenario.
     */
    suspend fun addScenario(scenario: Scenario): Long

    /**
     * Create a copy of a scenario and insert it in the database.
     *
     * @param completeScenario the scenario to copy.
     *
     * @return the database id of the copy, or null if the copy has encountered an error.
     */
    suspend fun addScenarioCopy(completeScenario: CompleteScenario): Long?

    /**
     * Create a copy of a scenario and insert it in the database.
     *
     * @param scenarioId the identifier of scenario to copy.
     * @param copyName the name for the copy.
     *
     * @return the database id of the copy, or null if the copy has encountered an error.
     */
    suspend fun addScenarioCopy(scenarioId: Long, copyName: String): Long?

    /**
     * Update a scenario.
     *
     * @param scenario the scenario to update.
     * @param events the list of event for the scenario.
     */
    suspend fun updateScenario(scenario: Scenario, events: List<Event>): Boolean

    /**
     * Delete a scenario.
     * This will delete all of its actions and conditions as well. All associated bitmaps will be removed in unused.
     *
     * @param scenarioId the identifier of the scenario to delete.
     */
    suspend fun deleteScenario(scenarioId: Identifier)

    /**
     * Mark a scenario as used.
     * This will update the ScenarioStats for this scenario.
     *
     * @param scenarioId the identifier of the scenario to update the usage stats of.
     */
    suspend fun markAsUsed(scenarioId: Identifier)

    /**
     * Get the requested scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the scenario.
     */
    suspend fun getScenario(scenarioId: Long): Scenario?

    /**
     * Get the flow on th requested scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the scenario.
     */
    fun getScenarioFlow(scenarioId: Long): Flow<Scenario?>

    /**
     * Get the list of events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the list of image events.
     */
    fun getEventsFlow(scenarioId: Long): Flow<List<Event>>

    /**
     * Get the list of image events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the list of image events.
     */
    suspend fun getScreenEvents(scenarioId: Long): List<ScreenEvent>

    /**
     * Get the list of complete image events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to ge the events from.
     * @return the list of image events, ordered by execution priority.
     */
    fun getScreenEventsFlow(scenarioId: Long): Flow<List<ScreenEvent>>

    /**
     * Get the list of trigger events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the list of trigger events.
     */
    suspend fun getTriggerEvents(scenarioId: Long): List<TriggerEvent>

    /**
     * Get the list of complete trigger events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to ge the events from.
     * @return the list of trigger events.
     */
    fun getTriggerEventsFlow(scenarioId: Long): Flow<List<TriggerEvent>>


    /**
     * Save the provided bitmap into the persistent memory.
     * If the bitmap is already saved, does nothing.
     *
     * @param bitmap the bitmap to be saved on the persistent memory.
     *
     * @return the path of the bitmap.
     */
    suspend fun saveConditionBitmap(bitmap: Bitmap): String

    /**
     * Get the bitmap for the given image condition.
     * Bitmaps are automatically cached by the bitmap manager.
     *
     * @param condition the condition to get the bitmap from.
     *
     * @return the bitmap, or null if the path can't be found.
     */
    suspend fun getConditionBitmap(condition: ImageCondition): Bitmap?

    suspend fun cleanupUnusedBitmaps(removedPath: List<String>)

    fun startTutorialMode()

    fun stopTutorialMode()

    fun isTutorialModeEnabled(): Boolean

    fun isTutorialModeEnabledFlow(): Flow<Boolean>
}
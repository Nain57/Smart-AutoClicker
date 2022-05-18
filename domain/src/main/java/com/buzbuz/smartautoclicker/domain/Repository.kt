/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri

import com.buzbuz.smartautoclicker.backup.BackupEngine
import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase

import kotlinx.coroutines.flow.Flow

/**
 * Repository storing the information about the scenarios and their events.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 */
interface Repository {

    companion object {

        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: Repository? = null

        /**
         * Get the repository singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the repository singleton.
         */
        fun getRepository(context: Context): Repository {
            return INSTANCE ?: synchronized(this) {
                val instance = RepositoryImpl(
                    ClickDatabase.getDatabase(context),
                    BitmapManager.getBitmapManager(context),
                    BackupEngine.newBackupEngine(context),
                )
                INSTANCE = instance
                instance
            }
        }
    }

    /** The list of scenarios. */
    val scenarios: Flow<List<Scenario>>

    /**
     * Add a new scenario.
     *
     * @param scenario the scenario to add.
     * @return the identifier for the newly add scenario.
     */
    suspend fun addScenario(scenario: Scenario): Long

    /**
     * Update a scenario.
     *
     * @param scenario the scenario to update.
     */
    suspend fun updateScenario(scenario: Scenario)

    /**
     * Delete a scenario.
     * This will delete all of its actions and conditions as well. All associated bitmaps will be removed in unused.
     *
     * @param scenario the scenario to delete.
     */
    suspend fun deleteScenario(scenario: Scenario)

    /**
     * Get a flow on the specified scenario.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the flow on the scenario.
     */
    fun getScenario(scenarioId: Long): Flow<Scenario>

    /**
     * Get a flow on the scenario and its and conditions.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the flow on the scenario and its end conditions.
     */
    fun getScenarioWithEndConditionsFlow(scenarioId: Long): Flow<Pair<Scenario, List<EndCondition>>>

    /**
     * Get a flow on the scenario and its and conditions.
     *
     * @param scenarioId the identifier of the scenario.
     * @return the flow on the scenario and its end conditions.
     */
    suspend fun getScenarioWithEndConditions(scenarioId: Long): Pair<Scenario, List<EndCondition>>

    /**
     * Update an existing end condition.
     * @param endConditions the new list end condition to be updated.
     */
    suspend fun updateEndConditions(scenarioId: Long, endConditions: List<EndCondition>)

    /**
     * Get the total number of events in the database.
     * @return a flow on the total number of events.
     */
    fun getEventCount(): Flow<Int>

    /**
     * Get the total number of actions in the database.
     * @return a flow on the total number of actions.
     */
    fun getActionsCount(): Flow<Int>

    /**
     * Get the total number of conditions in the database.
     * @return a flow on the total number of conditions.
     */
    fun getConditionsCount(): Flow<Int>

    /**
     * Get the list of events for a given scenario.
     * Note that those events will not have their actions/conditions, use [getCompleteEventList] for that.
     *
     * @param scenarioId the identifier of the scenario to ge the events from.
     * @return the list of events, ordered by execution priority.
     */
    fun getEventList(scenarioId: Long): Flow<List<Event>>

    /**
     * Get the list of complete events for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to ge the events from.
     * @return the list of complete events, ordered by execution priority.
     */
    fun getCompleteEventList(scenarioId: Long): Flow<List<Event>>

    /**
     * Get the complete version of a given event.
     *
     * @param eventId the event identifier to get the complete version of.
     * @return the complete event.
     */
    suspend fun getCompleteEvent(eventId: Long): Event

    /**
     * Get all events from all scenarios.
     *
     * @return the list containing all events.
     */
    fun getAllEvents(): Flow<List<Event>>

    /**
     * Get all actions from all events.
     *
     * @return the list containing all actions.
     */
    fun getAllActions(): Flow<List<Action>>

    /**
     * Get all conditions from all events.
     *
     * @return the list containing all conditions.
     */
    fun getAllConditions(): Flow<List<Condition>>
    /**
     * Add a new event.
     * It must be complete in order to be added or it will be skipped.
     *
     * @param event the event to be added.
     * @return true if it has been added, false if not.
     */
    suspend fun addEvent(event: Event): Boolean

    /**
     * Update an event.
     * It must be complete in order to be updated or it will be skipped.
     *
     * @param event the event to update.
     */
    suspend fun updateEvent(event: Event)

    /**
     * Update the priorities of the event list.
     *
     * @param events the events, ordered by execution priority.
     */
    suspend fun updateEventsPriority(events: List<Event>)

    /**
     * Remove an event.
     *
     * @param event the event to remove.
     */
    suspend fun removeEvent(event: Event)

    /**
     * Get the bitmap for the given path.
     * Bitmaps are automatically cached by the bitmap manager.
     *
     * @param path the path of the bitmap on the application data folder.
     * @param width the width of the bitmap, in pixels.
     * @param height the height of the bitmap, in pixels.
     *
     * @return the bitmap, or null if the path can't be found.
     */
    suspend fun getBitmap(path: String, width: Int, height: Int): Bitmap?

    /** Clean the cache of this repository. */
    fun cleanCache()

    /**
     * Create a backup of the provided scenario into the provided file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param scenarios the scenarios to backup.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup creation progress.
     */
    fun createScenarioBackup(zipFileUri: Uri, scenarios: List<Long>, screenSize: Point): Flow<Backup>

    /**
     * Restore a backup of scenarios from the provided file.
     *
     * @param zipFileUri the uri of the file to read the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup import progress.
     */
    fun restoreScenarioBackup(zipFileUri: Uri, screenSize: Point): Flow<Backup>
}
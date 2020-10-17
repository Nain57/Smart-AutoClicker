/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData

import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.ClickWithConditions
import com.buzbuz.smartautoclicker.database.room.ScenarioEntity

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Repository for accessing the click scenarios and their clicks. Provide methods for creation/edition.
 *
 * @param database the database containing the clicks and scenarios.
 */
class ClickRepository private constructor(database: ClickDatabase) {

    companion object {

        /** Tag for logs */
        private const val TAG = "ClickRepository"
        /** Invalid scenario identifier, used when there is no scenario currently loaded. */
        private const val NO_SCENARIO = -1L
        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: ClickRepository? = null

        /**
         * Get the repository singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the repository singleton.
         */
        fun getRepository(context: Context): ClickRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ClickRepository(ClickDatabase.getDatabase(context))
                INSTANCE = instance
                instance
            }
        }
    }

    /** The Dao for accessing the click database. */
    private val clickDao = database.clickDao()
    /** The identifier of the scenario currently loaded in this repository. */
    val currentScenario = MutableLiveData<Long>().apply { value = NO_SCENARIO }
    /** The list of database clicks for the currently loaded scenario. */
    private val clicks: LiveData<List<ClickWithConditions>> = Transformations.switchMap(currentScenario) { scenarioId ->
        clickDao.getClicksWithConditions(scenarioId)
    }

    /** The list of scenario in the database. */
    val scenarios = Transformations.switchMap(clickDao.getClickScenarios()) {
        liveData { emit(ClickScenario.fromEntities(it)) }
    }

    /**
     * Creates a new click scenario.
     *
     * @param name the name of the new scenario.
     */
    suspend fun createScenario(name: String) {
        clickDao.addClickScenario(ScenarioEntity(0, name))
    }

    /**
     * Rename the selected scenario.
     *
     * @param scenarioId the identifier of the scenario to be renamed
     * @param name the new name of the scenario
     */
    suspend fun renameScenario(scenarioId: Long, name: String) {
        clickDao.renameScenario(scenarioId, name)
    }

    /**
     * Delete a click scenario and all related clicks.
     *
     * @param scenario the scenario to be deleted.
     */
    suspend fun deleteScenario(scenario: ClickScenario) {
        clickDao.deleteClickScenario(scenario.toEntity())
    }

    /**
     * Get the list of clicks for the specified scenario.
     *
     * This method is the entry point for the click management for a scenario. All click editions methods (i.e,
     * [addClick], [deleteClick], [updateClick] and [updateClicksPriority]) will have no effects until this method is
     * called with a valid scenario identifier.
     *
     * @param coroutineContext the coroutine context executing the asynchronous loading of clicks.
     * @param scenarioId the identifier of the scenario containing the clicks to be loaded.
     *
     * @return the livedata on the list of clicks.
     */
    fun loadScenario(coroutineContext: CoroutineContext, scenarioId: Long) : LiveData<List<ClickInfo>> {
        currentScenario.value = scenarioId
        return Transformations.switchMap(clicks) { clicksWithConditions ->
            liveData(coroutineContext) { emit(ClickInfo.fromEntities(clicksWithConditions)) }
        }
    }

    /**
     * Get the list of clicks for a scenario.
     *
     * @param scenarioId the identifier of the scenario to get the clicks of.
     *
     * @return the livedata on the list of clicks.
     */
    fun getClicks(scenarioId: Long): LiveData<List<ClickInfo>> {
        return Transformations.switchMap(clickDao.getClicksWithConditions(scenarioId)) { clicks ->
            liveData(Dispatchers.IO) { emit(ClickInfo.fromEntities(clicks)) }
        }
    }

    /**
     * Get the list of clicks for a scenario.
     *
     * @param scenarioId the identifier of the scenario to get the clicks of.
     *
     * @return the list of clicks.
     */
    suspend fun getClickList(scenarioId: Long): List<ClickInfo> {
        return ClickInfo.fromEntities(clickDao.getClicksWithConditionsList(scenarioId))
    }
    /**
     * Cleanup the cache of the repository.
     *
     * This will unload the current scenario, and perform a clean on the database and bitmaps for all clicks without
     * a scenario, as well as all conditions without a click. All bitmaps that are no longer related to a condition
     * will be deleted from the application data folder.
     *
     * After calling this method, you must call [loadScenario] again before calling any click edition method.
     */
    fun cleanupCache() {
        currentScenario.postValue(NO_SCENARIO)
    }

    suspend fun deleteClicklessConditions(): List<String> = clickDao.deleteClicklessConditions()

    /**
     * Add a click to the currently loaded scenario.
     *
     * @param click the click to be added. It must have its id and priority undefined.
     */
    suspend fun addClick(click: ClickInfo) {
        if (click.id != 0L || click.priority != 0) {
            Log.e(TAG, "Can't add this click, the id and priority must be equals to 0. Click=$click")
            return
        }

        click.priority = clickDao.getClickCount(click.scenarioId)
        clickDao.addClickWithConditions(click.toEntity())
        Log.d(TAG, "Added click: $click")
    }

    /**
     * Update an existing click.
     * If the click don't have an id, this method will have no effects.
     *
     * @param click the click to be updated.
     */
    suspend fun updateClick(click: ClickInfo) {
        if (click.id == 0L) {
            Log.e(TAG, "Can't update this click, the id must be defined. Click=$click")
            return
        }

        clickDao.updateClickWithConditions(click.toEntity())
        Log.d(TAG, "Updated click: $click")
    }

    /**
     * Delete an existing click.
     * If the click don't have an id, this method will have no effects.
     *
     * @param click the click to be deleted.
     */
    suspend fun deleteClick(click: ClickInfo) {
        if (click.id == 0L) {
            Log.e(TAG, "Can't delete this click, the id must be defined. Click=$click")
            return
        }

        // All clicks after the deleted one in the list must update their priorities
        clickDao.updateClicks(clickDao.getClicksLessPrioritized(click.scenarioId, click.priority).map { clickEntity ->
            clickEntity.priority -= 1
            clickEntity
        })

        // Then, delete the click
        clickDao.deleteClick(click.toEntity().click)

        Log.d(TAG, "Deleted click: $click")
    }

    /**
     * Update the priority of the clicks from the currently loaded scenario.
     *
     * @param newClicks the clicks, ordered with their new priority (first is highest, last is lowest).
     */
    suspend fun updateClicksPriority(newClicks: List<ClickInfo>) {
        if (newClicks.isEmpty()) {
            return
        } else if (clickDao.getClickCount(newClicks[0].scenarioId) != newClicks.size) {
            Log.e(TAG, "Can't update priorities, the click count doesn't match the database one.")
            return
        }

        clickDao.updateClicks(newClicks.mapIndexed { index, click ->
            click.priority = index
            click.toEntity().click
        })

        Log.d(TAG, "Updated click priorities: $newClicks")
    }
}

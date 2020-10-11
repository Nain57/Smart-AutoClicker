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
package com.buzbuz.smartautoclicker.clicks

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData

import com.buzbuz.smartautoclicker.clicks.database.ClickDatabase
import com.buzbuz.smartautoclicker.clicks.database.ClickWithConditions
import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity

import kotlin.coroutines.CoroutineContext

/**
 * Repository for accessing the click scenarios and their clicks. Provide methods for creation/edition.
 *
 * @param database the database containing the clicks and scenarios.
 * @param context the Android context.
 */
class ClickRepository(database: ClickDatabase, context: Context) {

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
                val instance = ClickRepository(ClickDatabase.getDatabase(context), context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** Manages the bitmaps of the click conditions. */
    private val bitmapManager = BitmapManager(context.filesDir)
    /** The Dao for accessing the click database. */
    private val clickDao = database.clickDao()
    /** The identifier of the scenario currently loaded in this repository. */
    private val currentScenario = MutableLiveData<Long>().apply { value = NO_SCENARIO }
    /** The list of database clicks for the currently loaded scenario. */
    private val clicks: LiveData<List<ClickWithConditions>> = Transformations.switchMap(currentScenario) { scenarioId ->
        clickDao.getClicksWithConditions(scenarioId)
    }

    /** The list of scenario in the database. */
    val scenarios = clickDao.getClickScenarios()

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
     * Rename the selected scenario.
     *
     * @param scenarioId the identifier of the scenario to be renamed
     * @param name the new name of the scenario
     */
    suspend fun renameScenario(scenarioId: Long, name: String) {
        clickDao.renameScenario(scenarioId, name)
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
    suspend fun cleanupCache() {
        currentScenario.postValue(NO_SCENARIO)
        bitmapManager.deleteBitmaps(clickDao.deleteClicklessConditions())
        bitmapManager.releaseCache()
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
     * Delete a click scenario and all related clicks.
     *
     * @param scenario the scenario to be deleted.
     */
    suspend fun deleteScenario(scenario: ScenarioEntity) {
        clickDao.deleteClickScenario(scenario)
    }

    /**
     * Add a click to the currently loaded scenario.
     * If no scenario were previously loaded with [loadScenario], this method will have no effects.
     *
     * @param click the click to be added.
     */
    suspend fun addClick(click: ClickInfo) {
        currentScenario.value?.let { scenarioId ->
            clickDao.addClickWithConditions(click.toEntity(scenarioId, clicks.value!!.size))
            Log.d(TAG, "Added click: $click")
        } ?: Log.w(TAG, "Can't add click $click without a current scenario")
    }

    /**
     * Update an existing click from the currently loaded scenario.
     * If no scenario were previously loaded with [loadScenario], this method will have no effects. Same if the click
     * isn't int the current scenario.
     *
     * @param click the click to be updated.
     */
    suspend fun updateClick(click: ClickInfo) {
        currentScenario.value?.let { scenarioId ->
            val index = clicks.value!!.indexOfFirst { it.click.clickId == click.id }
            if (index == -1) {
                Log.w(TAG, "Trying to update an unknown click, skipping.")
                return
            }

            clickDao.updateClickWithConditions(click.toEntity(scenarioId, index))
            Log.d(TAG, "Updated click: $click")
        } ?: Log.w(TAG, "Can't update click $click without a current scenario")
    }

    /**
     * Delete an existing click from the currently loaded scenario.
     * If no scenario were previously loaded with [loadScenario], this method will have no effects. Same if the click
     * isn't int the current scenario.
     *
     * @param click the click to be deleted.
     */
    suspend fun deleteClick(click: ClickInfo) {
        currentScenario.value?.let { scenarioId ->
            val newList = clicks.value!!.toMutableList()
            val priority = newList.indexOfFirst { it.click.clickId == click.id }
            if (priority == -1) {
                Log.w(TAG, "Trying to delete an unknown click, skipping.")
                return
            }

            newList.removeAt(priority)

            // Update priority of all clicks below the deleted one
            if (priority < newList.size) {
                updateClicksEntitiesPriority(newList.subList(priority, newList.size - 1))
            }
            clickDao.deleteClick(click.toEntity(scenarioId, priority).click)

            Log.d(TAG, "Deleted click: $click")
        } ?: Log.w(TAG, "Can't delete click $click without a current scenario")
    }

    /**
     * Update the priority of the clicks from the currently loaded scenario.
     * If no scenario were previously loaded with [loadScenario], this method will have no effects.
     *
     * @param newClicks the clicks, ordered with their new priority (first is highest, last is lowest).
     */
    suspend fun updateClicksPriority(newClicks: List<ClickInfo>) {
        currentScenario.value?.let { scenarioId ->
            updateClicksEntitiesPriority(newClicks.map { clickInfo ->
                clickInfo.toEntity(scenarioId, 0)
            })
        } ?: Log.w(TAG, "Can't update click priority without a current scenario")
    }

    /**
     * Update the database clicks with their new priority.
     *
     * @param newClicks the new clicks entities, containing their new priority.
     */
    private suspend fun updateClicksEntitiesPriority(newClicks: List<ClickWithConditions>) {
        currentScenario.value?.let { _ ->
            if (clicks.value!!.size < newClicks.size) {
                Log.e(TAG, "Trying to update priorities with an invalid new list.")
                return
            }

            clickDao.updateClicks(newClicks.mapIndexed { index, entity ->
                entity.click.priority = index
                entity.click
            })

            Log.d(TAG, "Updated click priorities: $newClicks")
        } ?: Log.w(TAG, "Can't update click priority without a current scenario")
    }
}

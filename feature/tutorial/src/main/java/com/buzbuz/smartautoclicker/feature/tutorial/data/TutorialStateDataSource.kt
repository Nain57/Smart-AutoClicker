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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
import com.buzbuz.smartautoclicker.core.database.entity.TutorialSuccessEntity
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.TutorialSuccessState
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Manages the tutorials user scenario and their success state. */
internal class TutorialStateDataSource(context: Context) {

    private val tutorialDatabase: TutorialDatabase = TutorialDatabase.getDatabase(context)
    private val bitmapManager: BitmapManager = BitmapManager.getBitmapManager(context)
    private val scenarioRepository: Repository = Repository.getRepository(context)

    /** The list of successes for the tutorials. */
    val tutorialSuccessList: Flow<List<TutorialSuccessState>> =
        tutorialDatabase.tutorialDao().getTutorialSuccessList()
            .map { list -> list.map { TutorialSuccessState(it.scenarioId) } }

    suspend fun initTutorialScenario(tutorialIndex: Int): Long? {
        val scenarioDbId = withContext(Dispatchers.IO) {
            if (tutorialIndex == 0) {
                scenarioRepository.addScenario(
                    Scenario(
                        id = Identifier(databaseId = DATABASE_ID_INSERTION, domainId = 0L),
                        name = "Tutorial",
                        detectionQuality = 1200,
                        endConditionOperator = OR,
                    )
                )
            } else {
                getTutorialScenarioDatabaseId(tutorialIndex - 1)?.databaseId?.let { id ->
                    tutorialDatabase.scenarioDao().getCompleteScenario(id)?.let { completeScenario ->
                        scenarioRepository.addScenarioCopy(
                            completeScenario.copy(
                                scenario = completeScenario.scenario.copy(detectionQuality = 1200)
                            )
                        )
                    }
                }
            }
        }

        if (scenarioDbId == null) Log.e(TAG, "Can't get the scenario for the tutorial $tutorialIndex")
        return scenarioDbId
    }

    suspend fun setTutorialSuccess(index: Int, scenarioId: Identifier) {
        Log.d(TAG, "Set tutorial success for tutorial $index with scenario $scenarioId")

        withContext(Dispatchers.IO) {
            if (getTutorialScenarioDatabaseId(index) != null) {
                Log.d(TAG, "Tutorial was already completed with another scenario, skip success update.")
                return@withContext
            }

            tutorialDatabase.tutorialDao().upsert(
                TutorialSuccessEntity(
                    tutorialIndex = index,
                    scenarioId = scenarioId.databaseId,
                )
            )
        }
    }

    suspend fun cleanupTutorialState(index: Int, scenarioId: Identifier) {
        withContext(Dispatchers.IO) {
            val successScenarioDbId = getTutorialScenarioDatabaseId(index)
            if (successScenarioDbId == scenarioId) {
                return@withContext
            }

            Log.d(TAG, "Tutorial wasn't completed, cleaning up state for tutorial $index with scenario $scenarioId")

            val removedConditionsPath = mutableListOf<String>()
            tutorialDatabase.eventDao().getEventsIds(scenarioId.databaseId).forEach { eventId ->
                tutorialDatabase.conditionDao().getConditionsPath(eventId).forEach { path ->
                    if (!removedConditionsPath.contains(path)) removedConditionsPath.add(path)
                }
            }

            tutorialDatabase.scenarioDao().delete(scenarioId.databaseId)
            val deletedPaths = removedConditionsPath.filter { path ->
                tutorialDatabase.conditionDao().getValidPathCount(path) == 0
            }
            bitmapManager.deleteBitmaps(deletedPaths)
        }
    }

    private suspend fun getTutorialScenarioDatabaseId(index: Int): Identifier? =
        tutorialDatabase.tutorialDao().getTutorialScenarioId(index)?.let {
            Identifier(databaseId = it)
        }
}

private const val TAG = "TutorialStateDataSource"
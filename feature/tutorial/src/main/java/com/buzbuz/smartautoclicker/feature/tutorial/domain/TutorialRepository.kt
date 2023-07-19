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
package com.buzbuz.smartautoclicker.feature.tutorial.domain

import android.content.Context
import android.graphics.Rect
import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialDataSource
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialEngine
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.Tutorial
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialOverlayState
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.toDomain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TutorialRepository private constructor(
    context: Context,
    private val dataSource: TutorialDataSource,
) {

    companion object {

        /** Singleton preventing multiple instances of the TutorialRepository at the same time. */
        @Volatile
        private var INSTANCE: TutorialRepository? = null

        /**
         * Get the TutorialRepository singleton, or instantiates it if it wasn't yet.
         *
         * @return the TutorialRepository singleton.
         */
        fun getTutorialRepository(context: Context): TutorialRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TutorialRepository(context, TutorialDataSource)
                INSTANCE = instance
                instance
            }
        }
    }

    private val scenarioRepository: Repository = Repository.getRepository(context)
    private val detectionRepository: DetectionRepository =  DetectionRepository.getDetectionRepository(context)

    private val tutorialEngine: TutorialEngine = TutorialEngine(context)

    /**
     * The identifier of the user scenario when he enters the tutorial mode.
     * Kept to be restored once he quit the tutorial.
     */
    private var scenarioId: Identifier? = null
    private var allStepsCompleted: Boolean = false

    private val activeTutorialIndex: MutableStateFlow<Int?> = MutableStateFlow(null)

    val tutorials: Flow<List<Tutorial>> = scenarioRepository.tutorialSuccessList
        .map { successList ->
            dataSource.tutorials.mapIndexed { index, tutorialData ->
                tutorialData.toDomain(index < successList.lastIndex)
            }
        }

    val activeTutorial: Flow<Tutorial?> = tutorials
        .combine(activeTutorialIndex) { tutorialList, activeIndex ->
            activeIndex ?: return@combine null
            tutorialList[activeIndex]
        }

    val tutorialOverlayState: Flow<TutorialOverlayState?> = tutorialEngine.currentStep
        .map { step ->
            Log.d(TAG, "Update overlay state for step $step")
            step?.toDomain()
        }

    fun setupTutorialMode() {
        if (scenarioId != null) return

        scenarioId = detectionRepository.getScenarioId()

        Log.d(TAG, "Setup tutorial mode, user scenario is $scenarioId")
        scenarioRepository.startTutorialMode()
    }

    fun stopTutorialMode() {
        scenarioId ?: return

        Log.d(TAG, "Stop tutorial mode, restoring user scenario $scenarioId")

        scenarioId?.let { detectionRepository.setScenarioId(it) }
        scenarioId = null
        allStepsCompleted = false
        activeTutorialIndex.value = null
        scenarioRepository.stopTutorialMode()
    }

    suspend fun startTutorial(index: Int) {
        if (scenarioId == null) {
            Log.e(TAG, "Tutorial mode is not setup, can't start tutorial $index")
            return
        }
        if (tutorialEngine.isStarted()) return
        if (index < 0 || index >= dataSource.tutorials.size) {
            Log.e(TAG, "Can't start tutorial, index is invalid $index")
            return
        }

        val tutoScenarioDbId = initTutorialScenario(index) ?: return
        Log.d(TAG, "Start tutorial $index, set current scenario to $tutoScenarioDbId")

        activeTutorialIndex.value = index
        allStepsCompleted = false
        detectionRepository.setScenarioId(Identifier(databaseId = tutoScenarioDbId))
        withContext(Dispatchers.Main) {
            tutorialEngine.startTutorial(dataSource.tutorials[index])
        }
    }

    suspend fun stopTutorial() {
        withContext(Dispatchers.Main) {
            tutorialEngine.stopTutorial()
        }

        val scenarioIdentifier = scenarioId ?: return
        val tutoIndex = activeTutorialIndex.value ?: return

        if (allStepsCompleted) {
            scenarioRepository.setTutorialSuccess(tutoIndex, scenarioIdentifier)
        } else {
            scenarioRepository.deleteScenario(scenarioIdentifier)
        }

        detectionRepository.setScenarioId(scenarioIdentifier)
        activeTutorialIndex.value = null
        allStepsCompleted = false
    }

    fun nextTutorialStep() {
        allStepsCompleted = tutorialEngine.nextStep()
    }

    fun skipAllTutorialSteps() {
        tutorialEngine.skipAllSteps()
    }

    fun startGame(scope: CoroutineScope, area: Rect, targetSize: Int) {
        tutorialEngine.startGame(scope, area, targetSize)
    }

    fun stopGame() {
        tutorialEngine.stopGame()
    }

    fun onGameTargetHit(targetType: TutorialGameTargetType) {
        tutorialEngine.onGameTargetHit(targetType)
    }

    private suspend fun initTutorialScenario(tutorialIndex: Int): Long? {
        val scenarioDbId =
            if (tutorialIndex == 0) {
                scenarioRepository.addScenario(
                    Scenario(
                        id = Identifier(databaseId = DATABASE_ID_INSERTION, domainId = 0L),
                        name = "Tutorial",
                        detectionQuality = 600,
                        endConditionOperator = OR,
                    )
                )
            } else scenarioRepository.getTutorialScenarioDatabaseId(tutorialIndex - 1)?.databaseId

        if (scenarioDbId == null) Log.e(TAG, "Can't get the scenario for the tutorial $tutorialIndex")
        return scenarioDbId
    }
}

private const val TAG = "TutorialRepository"
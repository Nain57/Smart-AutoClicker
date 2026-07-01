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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl

import android.content.Intent
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tip
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.data.TipsStateDataSource
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.data.TutorialCompletionStateDataSource
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.TutorialEngine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.max

@Singleton
internal class TutorialRepositoryImpl @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val displayConfigManager: DisplayConfigManager,
    private val accessibilityServiceConnection: LocalAccessibilityServiceConnection,
    private val smartRepository: IRepository,
    private val tutorialEngine: TutorialEngine,
    private val tutorialTipsStateDataSource: TipsStateDataSource,
    private val tutorialCompletionStateDataSource: TutorialCompletionStateDataSource,
) : TutorialRepository {

    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val tutorialScenarioDbId: MutableStateFlow<Identifier?> = MutableStateFlow(null)

    override val tutorialsCompletionState: Flow<Map<String, Boolean>>
        get() = tutorialCompletionStateDataSource.getTutorialsCompletionState()

    override val tutorialSubjectController: StateFlow<TutorialSubjectController?>
        get() = tutorialEngine.tutorialSubjectController

    override val tutorialState: StateFlow<TutorialState>
        get() = tutorialEngine.tutorialState


    override fun startTutorial(tutorial: Tutorial, mpResultCode: Int, mpData: Intent) {
        coroutineScope.launch {
            val localService = accessibilityServiceConnection.getLocalService()
            if (localService == null) {
                Log.e(TAG, "Can't start tutorial, accessibility service is not started")
                return@launch
            }

            // Add a dummy scenario to contains the user changes during the tutorial
            // Must be deleted once the tutorial is finished
            val scenario = Scenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
                name = "Tutorial",
                detectionQuality = getDefaultDetectionQuality(),
                randomize = false,
            )
            val scenarioDbId = smartRepository.addScenario(scenario)
            val scenarioId = Identifier(databaseId = scenarioDbId)

            // Use the database identifier
            tutorialScenarioDbId.update { scenarioId }
            val insertedScenario = scenario.copy(id = scenarioId)

            // Load the scenario
            localService.startSmartScenario(
                scenario = insertedScenario,
                resultCode = mpResultCode,
                data = mpData,
            )

            tutorialEngine.startTutorial(tutorial)
        }
    }

    override fun stopTutorial() {
        coroutineScope.launch {
            val localService = accessibilityServiceConnection.getLocalService()
            if (localService == null) {
                Log.e(TAG, "Can't stop tutorial, accessibility service is not started")
                return@launch
            }

            tutorialEngine.stopTutorial()
            localService.stopScenario()

            tutorialScenarioDbId.value?.let { scenarioDbId ->
                smartRepository.deleteScenario(scenarioDbId)
            }
        }
    }

    override fun nextTutorialStep() {
        tutorialEngine.nextStep()
    }

    override fun skipToLastTutorialStep() {
        tutorialEngine.lastStep()
    }

    override fun shouldShowTip(tip: Tip): Flow<Boolean> =
        tutorialTipsStateDataSource.getTipsDontShowAgainValue(tip)
            .combine(tutorialState) { dontShowAgain, state ->
                !dontShowAgain && state is TutorialState.Stopped
            }

    override fun dontShowTipAgain(tip: Tip) {
        coroutineScope.launch {
            tutorialTipsStateDataSource.setTipsDontShowAgain(tip)
        }
    }

    private fun getDefaultDetectionQuality(): Int {
        val displaySize = displayConfigManager.displayConfig.sizePx
        val biggestScreenSideSize: Int = max(displaySize.x, displaySize.y)

        return max(400, floor(biggestScreenSideSize / 2.05).toInt())
    }
}

private const val TAG = "TutorialRepositoryImpl"
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
import android.graphics.Rect
import android.util.Log

import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameStateData
import com.buzbuz.smartautoclicker.feature.tutorial.data.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class TutorialEngine(context: Context) {

    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val overlayManager: OverlayManager = OverlayManager.getInstance(context)
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    private val _tutorial: MutableStateFlow<TutorialData?> = MutableStateFlow(null)
    val tutorial: StateFlow<TutorialData?> = _tutorial

    private val stepIndex: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val stepStartBackStackTop: MutableStateFlow<Overlay?> = MutableStateFlow(null)
    private val isMonitoredViewClicked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val gameState: Flow<TutorialGameStateData?> = _tutorial
        .flatMapLatest { tutorial ->
            tutorial?.game?.gameState ?: flowOf(null)
        }

    val currentStep: Flow<TutorialStepData?> =
        combine(
            _tutorial,
            stepIndex,
            gameState,
            overlayManager.backStackTop,
            isMonitoredViewClicked,
        ) { tuto, index, gameState, top, isTargetClicked ->
            if (tuto == null || index == null) return@combine null
            if (index < 0 || index >= tuto.steps.size) return@combine null

            val step = tuto.steps[index]
            Log.d(TAG, "New tutorial step $index - $step; backStackTop=$top")

            when (step.stepStartCondition) {
                StepStartCondition.Immediate ->
                    step

                StepStartCondition.NextOverlay ->
                    if (stepStartBackStackTop.value != top) step
                    else null

                StepStartCondition.GameWon ->
                    if (gameState?.isWon == true) step
                    else null

                StepStartCondition.GameLost -> {
                    if (gameState?.isWon == false) step
                    else null
                }

                is StepStartCondition.MonitoredViewClicked -> {
                    if (isTargetClicked) step
                    else null
                }
            }
        }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(3_000), 1)

    fun startTutorial(newTutorial: TutorialData) {
        Log.d(TAG, "Start tutorial")

        // Keep track of current top of back stack value and monitored views
        setTutorialExpectedViews(newTutorial)
        stepStartBackStackTop.value = overlayManager.getBackStackTop()

        stepIndex.value = 0
        _tutorial.value = newTutorial
    }

    fun nextStep() {
        val step = getCurrentStep() ?: return
        val index = stepIndex.value ?: return

        // Keep track of current top of back stack value
        stepStartBackStackTop.value = overlayManager.getBackStackTop()

        isMonitoredViewClicked.value = false
        getNextStep()?.let { nextStep ->
            if (nextStep.stepStartCondition is StepStartCondition.MonitoredViewClicked) {
                monitoredViewsManager.monitorNextClick(nextStep.stepStartCondition.type) {
                    isMonitoredViewClicked.value = true
                }
            }
        }

        Log.d(TAG, "End current step: $index - $step; current stack top: ${stepStartBackStackTop.value}")

        stepIndex.value = index + 1

        val stepEndCondition = step.stepEndCondition
        if (stepEndCondition is StepEndCondition.MonitoredViewClicked) {
            monitoredViewsManager.performClick(stepEndCondition.type)
        }
    }

    fun skipAllSteps() {
        Log.d(TAG, "Skip all steps")

        stepIndex.value = null
        stepStartBackStackTop.value = null
        isMonitoredViewClicked.value = false
    }

    fun startGame(scope: CoroutineScope, area: Rect, targetSize: Int) {
        Log.d(TAG, "Start game on area $area with target size $targetSize")
        _tutorial.value?.game?.start(scope, area, targetSize)
    }

    fun onGameTargetHit(target: TutorialGameTargetType) {
        Log.d(TAG, "onTargetHit $target")
        _tutorial.value?.game?.onTargetHit(target)
    }

    fun stopGame() {
        Log.d(TAG, "Stop game")
        _tutorial.value?.game?.stop()
    }

    fun stopTutorial() {
        Log.d(TAG, "Stop tutorial")

        _tutorial.value?.game?.stop()
        monitoredViewsManager.clearExpectedViews()
        stepIndex.value = null
        _tutorial.value = null
    }

    private fun getCurrentStep(): TutorialStepData? {
        val index = stepIndex.value ?: return null
        return _tutorial.value?.steps?.get(index)
    }

    private fun getNextStep(): TutorialStepData? {
        val index = stepIndex.value ?: return null
        val steps = _tutorial.value?.steps ?: return null
        if (index + 1 > steps.lastIndex) return null

        return steps[index + 1]
    }

    private fun setTutorialExpectedViews(tutorial: TutorialData) {
        monitoredViewsManager.setExpectedViews(
            buildSet {
                tutorial.steps.forEach { step ->
                    if (step.stepEndCondition is StepEndCondition.MonitoredViewClicked)
                        add(step.stepEndCondition.type)
                }
            }
        )
    }
}

private const val TAG = "TutorialEngine"
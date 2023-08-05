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

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameStateData
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class TutorialEngine(context: Context, private val coroutineScope: CoroutineScope) {

    private val overlayManager: OverlayManager = OverlayManager.getInstance(context)
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    private val _tutorial: MutableStateFlow<TutorialData?> = MutableStateFlow(null)
    val tutorial: StateFlow<TutorialData?> = _tutorial

    private val stepState: MutableStateFlow<TutorialStepState?> = MutableStateFlow(null)

    val gameState: Flow<TutorialGameStateData?> = _tutorial
        .flatMapLatest { tutorial ->
            tutorial?.game?.gameState ?: flowOf(null)
        }

    val currentStep: Flow<TutorialStepData.TutorialOverlay?> = _tutorial
        .flowOnCurrentAndStartedStep()
        .onEach { step ->
            if (step is TutorialStepData.ChangeFloatingUiVisibility) {
                Log.d(TAG, "Started step is ChangeFloatingUiVisibility, update visibility and end step")
                if (step.isVisible) overlayManager.restoreVisibility() else overlayManager.hideAll()

                coroutineScope.launch { nextStep() }
            }
        }
        .map { step -> if (step is TutorialStepData.TutorialOverlay) step else null }
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(3_000), 1)

    fun isStarted(): Boolean = _tutorial.value != null

    fun startTutorial(newTutorial: TutorialData) {
        Log.d(TAG, "Start tutorial")

        // Keep track of current top of back stack value and monitored views
        setTutorialExpectedViews(newTutorial)

        _tutorial.value = newTutorial
        setStepIndex(0)
    }

    fun nextStep(): Boolean {
        val step = getCurrentStep() ?: return false
        val index = stepState.value?.index ?: return false

        val newIndex = index + 1
        setStepIndex(newIndex)

        // If step end condition is a click on the monitored view, execute it now
        if (step is TutorialStepData.TutorialOverlay && step.stepEndCondition is StepEndCondition.MonitoredViewClicked) {
            monitoredViewsManager.performClick(step.stepEndCondition.type)
        }

        val lastStepIndex = _tutorial.value?.steps?.lastIndex ?: return false
        return newIndex > lastStepIndex
    }

    private fun setStepIndex(newIndex: Int) {
        Log.d(TAG, "Set step index to $newIndex")

        // Get step state initial values
        val isMonitoredViewClicked = false
        val isGameWon = null
        val stackTop = overlayManager.getBackStackTop()

        // If the new step needs to monitor a view, do it here
        getNextStep()?.let { nextStep ->
            val startCondition = nextStep.stepStartCondition
            if (nextStep is TutorialStepData.TutorialOverlay && startCondition is StepStartCondition.MonitoredViewClicked) {
                monitoredViewsManager.monitorNextClick(startCondition.type) {
                    stepState.value = stepState.value?.copy(isMonitoredViewClicked = true)
                }
            }
        }

        // Update step state
        stepState.value = TutorialStepState(
            index = newIndex,
            isMonitoredViewClicked = isMonitoredViewClicked,
            isGameWon = isGameWon,
            stepStartStackTop = stackTop,
        )
    }

    fun skipAllSteps() {
        Log.d(TAG, "Skip all steps")
        stepState.value = null
    }

    fun startGame(area: Rect, targetSize: Int) {
        Log.d(TAG, "Start game on area $area with target size $targetSize")

        _tutorial.value?.game?.start(coroutineScope, area, targetSize) { isWon ->
            stepState.value = stepState.value?.copy(isGameWon = isWon)
        }
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
        stepState.value = null
        _tutorial.value = null
    }

    private fun getCurrentStep(): TutorialStepData? {
        val index = stepState.value?.index ?: return null
        return _tutorial.value?.steps?.get(index)
    }

    private fun getNextStep(): TutorialStepData? {
        val index = stepState.value?.index ?: return null
        val steps = _tutorial.value?.steps ?: return null
        if (index + 1 > steps.lastIndex) return null

        return steps[index + 1]
    }

    private fun setTutorialExpectedViews(tutorial: TutorialData) {
        monitoredViewsManager.setExpectedViews(
            buildSet {
                tutorial.steps.forEach { step ->
                    if (step is TutorialStepData.TutorialOverlay && step.stepEndCondition is StepEndCondition.MonitoredViewClicked)
                        add(step.stepEndCondition.type)
                }
            }
        )
    }

    private fun Flow<TutorialData?>.flowOnCurrentAndStartedStep(): Flow<TutorialStepData?> =
        combine(this, stepState, overlayManager.backStackTop) { tuto, state, stackTop ->
            tuto ?: return@combine null

            val index = state?.index
            if (index == null || index < 0 || index >= tuto.steps.size) return@combine null

            val step = tuto.steps[index]
            if (step.stepStartCondition.isFulfilled(state, stackTop)) {
                Log.d(TAG, "Current step is now started: $step")
                step
            } else {
                Log.d(TAG, "Current step is waiting for start condition: $step")
                null
            }
        }

    private fun StepStartCondition.isFulfilled(stepState: TutorialStepState, stackTop: Overlay?) : Boolean =
        when (this) {
            StepStartCondition.Immediate -> true
            StepStartCondition.NextOverlay -> stepState.stepStartStackTop != stackTop
            StepStartCondition.GameWon -> stepState.isGameWon == true
            StepStartCondition.GameLost -> stepState.isGameWon == false
            is StepStartCondition.MonitoredViewClicked -> stepState.isMonitoredViewClicked
        }
}

private data class TutorialStepState(
    val index: Int,
    val isMonitoredViewClicked: Boolean,
    val isGameWon: Boolean?,
    val stepStartStackTop: Overlay?,
)

private const val TAG = "TutorialEngine"
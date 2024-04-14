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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import android.graphics.Rect
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorialEngine @Inject constructor(
    private val overlayManager: OverlayManager,
    private val monitoredViewsManager: MonitoredViewsManager,
    @Dispatcher(Main) private val dispatcherMain: CoroutineDispatcher,
) {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherMain)

    private val _tutorial: MutableStateFlow<TutorialData?> = MutableStateFlow(null)
    internal val tutorial: StateFlow<TutorialData?> = _tutorial

    private val stepState: MutableStateFlow<TutorialStepState?> = MutableStateFlow(null)

    internal val currentStep: Flow<TutorialStepData?> = _tutorial
        .flowOnCurrentAndStartedStep()
        .shareIn(coroutineScopeMain, SharingStarted.WhileSubscribed(3_000), 1)

    internal fun isStarted(): Boolean = _tutorial.value != null

    internal fun startTutorial(newTutorial: TutorialData) {
        Log.d(TAG, "Start tutorial")

        // Keep track of current top of back stack value and monitored views
        setTutorialExpectedViews(newTutorial)

        _tutorial.value = newTutorial
        setStepIndex(0)
    }

    internal fun nextStep() {
        val step = getCurrentStep() ?: return
        val index = stepState.value?.index ?: return

        setStepIndex(index + 1)

        // If step end condition is a click on the monitored view, execute it now
        if (step is TutorialStepData.TutorialOverlay && step.stepEndCondition is StepEndCondition.MonitoredViewClicked) {
            monitoredViewsManager.performClick(step.stepEndCondition.type)
        }
    }

    internal fun lastStep() {
        val lastStepIndex = _tutorial.value?.steps?.lastIndex ?: return
        Log.d(TAG, "Go to last step")
        setStepIndex(lastStepIndex)
    }

    private fun setStepIndex(newIndex: Int) {
        val step = getStep(newIndex) ?: return

        Log.d(TAG, "Set step index to $newIndex")

        // Get step state initial values
        val isMonitoredViewClicked = false
        val isGameWon = null
        val stackTop = overlayManager.getBackStackTop()

        // If the new step needs to monitor a view, do it here
        val startCondition = step.stepStartCondition
        if (step is TutorialStepData.TutorialOverlay && startCondition is StepStartCondition.MonitoredViewClicked) {
            monitoredViewsManager.monitorNextClick(startCondition.type) {
                stepState.value = stepState.value?.copy(isMonitoredViewClicked = true)
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

    internal fun startGame(area: Rect, targetSize: Int) {
        Log.d(TAG, "Start game on area $area with target size $targetSize")

        _tutorial.value?.game?.start(coroutineScopeMain, area, targetSize) { isWon ->
            stepState.value = stepState.value?.copy(isGameWon = isWon)
        }
    }

    internal fun onGameTargetHit(target: TutorialGameTargetType) {
        Log.d(TAG, "onTargetHit $target")
        _tutorial.value?.game?.onTargetHit(target)
    }

    internal fun stopTutorial() {
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

    private fun getStep(index: Int): TutorialStepData? {
        val steps = _tutorial.value?.steps ?: return null
        if (index !in 0..steps.lastIndex) return null

        return steps[index]
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
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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject.TutorialGameEngine
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

internal class TutorialEngine @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val overlayManager: OverlayManager,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val stepsOrchestrator: TutorialStepsOrchestrator,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var stepConditionMonitoringJob: Job? = null

    private val _tutorialState: MutableStateFlow<TutorialState> = MutableStateFlow(TutorialState.Stopped)
    val tutorialState: StateFlow<TutorialState> = _tutorialState

    private val _tutorialSubjectController: MutableStateFlow<TutorialSubjectController?> = MutableStateFlow(null)
    val tutorialSubjectController: StateFlow<TutorialSubjectController?> =_tutorialSubjectController


    fun isStarted(): Boolean =
        tutorialState.value != TutorialState.Stopped

    fun startTutorial(newTutorial: Tutorial): TutorialSubjectController {
        val currentController = _tutorialSubjectController.value
        if (currentController != null) return currentController

        Log.d(TAG, "Start tutorial")

        // Keep track of current top of back stack value and monitored views
        monitoredViewsManager.setTutorialExpectedViews(newTutorial)

        val controller = createController(newTutorial)
        _tutorialSubjectController.update { controller }

        _tutorialState.update {
            TutorialState.Started(
                tutorial = newTutorial,
                isCompleted = false,
                isCurrentStepStarted = false,
                currentStep = null,
            )
        }

        // Init will set the index to 0 and trigger all transition callbacks, as any index changes.
        stepsOrchestrator.init(
            steps = newTutorial.steps,
            onStepEnded = ::onStepEnded,
            onNewStep = ::onNewStep,
            onCompleted = ::onTutorialCompleted,
        )

        return controller
    }

    fun stopTutorial() {
        Log.d(TAG, "Stop tutorial")

        stepConditionMonitoringJob?.cancel()
        stepConditionMonitoringJob = null
        monitoredViewsManager.clearExpectedViews()
        stepsOrchestrator.clear()

        _tutorialState.update { TutorialState.Stopped }
        _tutorialSubjectController.update { controller ->
            controller?.stop()
            null
        }
    }

    fun nextStep() {
        stepsOrchestrator.nextStep()
    }

    fun lastStep() {
        stepsOrchestrator.lastStep()
    }

    fun onNextStepButtonPressed() {
        val state = tutorialState.value
        if (state !is TutorialState.Started) return

        val currentStep = state.currentStep ?: return
        if (currentStep.stepEndCondition is TutorialStepEndCondition.NextButton) {
            onStepEndConditionReached(currentStep)
        }
    }

    private fun onTutorialCompleted() {
        Log.d(TAG, "onTutorialCompleted")

        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(isCompleted = true)
        }
    }

    private fun onStepEnded(step: TutorialStep) {
        Log.d(TAG, "onStepEnded: $step")

        when (val startCondition = step.stepStartCondition) {
            is TutorialStepStartCondition.MonitoredViewClicked -> {
                monitoredViewsManager.stopNextClickMonitoring(startCondition.type)
            }

            TutorialStepStartCondition.NextOverlay -> {
                stepConditionMonitoringJob?.cancel()
                stepConditionMonitoringJob = null
            }

            TutorialStepStartCondition.GameLost,
            TutorialStepStartCondition.GameWon -> {
                getSubjectController<TutorialGameEngine>()?.monitorNextCompletion(null)
            }

            TutorialStepStartCondition.Immediate -> Unit
        }

        when (val endCondition = step.stepEndCondition) {
            is TutorialStepEndCondition.MonitoredViewClicked -> {
                monitoredViewsManager.stopNextClickMonitoring(endCondition.type)
            }

            TutorialStepEndCondition.OverlayStackVisibilityChanged -> {
                stepConditionMonitoringJob?.cancel()
                stepConditionMonitoringJob = null
            }

            TutorialStepEndCondition.NextButton,
            TutorialStepEndCondition.Immediate -> Unit
        }
    }

    private fun onNewStep(step: TutorialStep) {
        Log.d(TAG, "onNewStep: $step")

        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(
                isCurrentStepStarted = false,
                currentStep = step,
            )
        }

        monitorStepStartCondition(
            step = step,
            onConditionReached = { onStepStartConditionReached(step) },
        )
    }

    private fun onStepStartConditionReached(step: TutorialStep) {
        Log.d(TAG, "onStepStartConditionReached: $step")

        // Update state as started
        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(isCurrentStepStarted = true)
        }

        // Start monitoring for end condition
        monitorStepEndCondition(
            step = step,
            onConditionReached = { onStepEndConditionReached(step) },
        )
    }

    private fun onStepEndConditionReached(step: TutorialStep) {
        Log.d(TAG, "onStepEndConditionReached: $step")

        nextStep()
    }

    private fun monitorStepStartCondition(step: TutorialStep, onConditionReached: () -> Unit) =
        when (val condition = step.stepStartCondition) {
            is TutorialStepStartCondition.MonitoredViewClicked -> {
                println("TOTO: step start monitor ${condition.type}")
                monitoredViewsManager.monitorNextClick(
                    type = condition.type,
                    listener = onConditionReached,
                )
            }

            TutorialStepStartCondition.NextOverlay -> {
                stepConditionMonitoringJob = coroutineScopeIo.launch {
                    val backstackTop = overlayManager.getBackStackTop()
                    overlayManager.backStackTopFlow.collect { newTop ->
                        if (backstackTop == newTop) return@collect

                        onConditionReached()
                        stepConditionMonitoringJob?.cancel()
                        stepConditionMonitoringJob = null
                    }
                }
            }

            TutorialStepStartCondition.GameLost ->
                getSubjectController<TutorialGameEngine>()?.monitorNextCompletion { isWon ->
                    if (isWon) return@monitorNextCompletion
                    onConditionReached()
                }

            TutorialStepStartCondition.GameWon ->
                getSubjectController<TutorialGameEngine>()?.monitorNextCompletion { isWon ->
                    if (!isWon) return@monitorNextCompletion
                    onConditionReached()
                }

            TutorialStepStartCondition.Immediate ->
                onConditionReached()
        }

    private fun monitorStepEndCondition(step: TutorialStep, onConditionReached: () -> Unit) =
        when (val condition = step.stepEndCondition) {
            is TutorialStepEndCondition.MonitoredViewClicked -> {
                monitoredViewsManager.monitorNextClick(
                    type = condition.type,
                    listener = onConditionReached,
                )
            }

            TutorialStepEndCondition.OverlayStackVisibilityChanged -> {
                val expectedVisibility = (step as? TutorialStep.ChangeFloatingUiVisibility)?.newVisibility
                stepConditionMonitoringJob = coroutineScopeIo.launch {
                    overlayManager.isStackHidden.collect { isStackHidden ->
                        if (isStackHidden == expectedVisibility) return@collect

                        onConditionReached()
                        stepConditionMonitoringJob?.cancel()
                        stepConditionMonitoringJob = null
                    }
                }
            }

            TutorialStepEndCondition.Immediate ->
                onConditionReached()

            TutorialStepEndCondition.NextButton -> Unit // handled via onNextStepButtonPressed
        }

    private fun createController(tutorial: Tutorial): TutorialSubjectController =
        when (val tutorialSubject = tutorial.subject) {
            is TutorialSubject.Game -> TutorialGameEngine(
                ioDispatcher = ioDispatcher,
                game = tutorialSubject,
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : TutorialSubjectController> getSubjectController(): T? =
        _tutorialSubjectController.value?.let { controller -> controller as? T }

}

private fun MonitoredViewsManager.setTutorialExpectedViews(tutorial: Tutorial) {
    setExpectedViews(
        buildSet {
            tutorial.steps.forEach { step ->
                val startCondition = step.stepStartCondition
                if (startCondition is TutorialStepStartCondition.MonitoredViewClicked)
                    add(startCondition.type)

                val endCondition = step.stepEndCondition
                if (endCondition is TutorialStepEndCondition.MonitoredViewClicked)
                    add(endCondition.type)
            }
        }
    )
}

private const val TAG = "TutorialEngine"

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
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.data.TutorialCompletionStateDataSource
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.step.TutorialStepEndConditionMonitor
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.step.TutorialStepStartConditionMonitor
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.step.TutorialStepsOrchestrator
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject.QuickClickGameEngine
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject.TimingGameEngine
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.MonitoredViewsManagerImpl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

internal class TutorialEngine @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val monitoredViewsManager: MonitoredViewsManagerImpl,
    private val stepsOrchestrator: TutorialStepsOrchestrator,
    private val completionStateDataSource: TutorialCompletionStateDataSource,
    private val stepEndConditionMonitor: TutorialStepEndConditionMonitor,
    private val stepStartConditionMonitor: TutorialStepStartConditionMonitor,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

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

        stepStartConditionMonitor.clearMonitoring()
        stepEndConditionMonitor.clearMonitoring()
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

    fun skipTutorial() {
        Log.d(TAG, "skipTutorial")

        stepStartConditionMonitor.clearMonitoring()
        stepEndConditionMonitor.clearMonitoring()
        stepsOrchestrator.clear()

        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(
                isCompleted = true,
                isCurrentStepStarted = true,
                currentStep = TutorialStep.EndStep(completed = false),
            )
        }
    }

    fun onNextStepButtonPressed() {
        val state = tutorialState.value
        if (state !is TutorialState.Started) return

        val currentStep = state.currentStep ?: return
        if (currentStep.stepEndCondition is TutorialStepEndCondition.NextButton) {
            onStepEndConditionReached(currentStep)
        }
    }

    private fun onStepEnded(step: TutorialStep) {
        val controller = _tutorialSubjectController.value ?: return

        Log.d(TAG, "onStepEnded: $step")

        stepStartConditionMonitor.stopMonitoring(
            condition = step.stepStartCondition,
            subjectController = controller,
        )

        stepEndConditionMonitor.stopMonitoring(
            condition = step.stepEndCondition,
        )
    }

    private fun onNewStep(step: TutorialStep) {
        val controller = _tutorialSubjectController.value ?: return

        Log.d(TAG, "onNewStep: $step")

        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(
                isCurrentStepStarted = false,
                currentStep = step,
            )
        }

        stepStartConditionMonitor.monitorCondition(
            condition = step.stepStartCondition,
            subjectController = controller,
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
        stepEndConditionMonitor.monitorCondition(
            condition = step.stepEndCondition,
            onConditionReached = { onStepEndConditionReached(step) },
        )
    }

    private fun onStepEndConditionReached(step: TutorialStep) {
        Log.d(TAG, "onStepEndConditionReached: $step")

        nextStep()
    }

    private fun onTutorialCompleted() {
        Log.d(TAG, "onTutorialCompleted")

        _tutorialState.update { old ->
            if (old !is TutorialState.Started) old
            else old.copy(
                isCompleted = true,
                isCurrentStepStarted = true,
                currentStep = TutorialStep.EndStep(completed = true),
            )
        }

        coroutineScopeIo.launch {
            val tutorial = (_tutorialState.value as? TutorialState.Started)?.tutorial ?: return@launch
            completionStateDataSource.setTutorialCompleted(tutorial)
        }
    }

    private fun createController(tutorial: Tutorial): TutorialSubjectController =
        when (val tutorialSubject = tutorial.subject) {
            is TutorialSubject.QuickClickGame -> QuickClickGameEngine(
                ioDispatcher = ioDispatcher,
                game = tutorialSubject,
            )

            is TutorialSubject.TimingGame -> TimingGameEngine(
                game = tutorialSubject,
            )
        }
}

private fun MonitoredViewsManagerImpl.setTutorialExpectedViews(tutorial: Tutorial) {
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

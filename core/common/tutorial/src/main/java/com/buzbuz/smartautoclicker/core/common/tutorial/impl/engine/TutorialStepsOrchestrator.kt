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

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


internal class TutorialStepsOrchestrator @Inject constructor() {

    private var stepEndedListener: ((TutorialStep) -> Unit)? = null
    private var newStepListener: ((TutorialStep) -> Unit)? = null
    private var completionListener: (() -> Unit)? = null

    private val tutorialSteps: MutableStateFlow<List<TutorialStep>> = MutableStateFlow(emptyList())
    private val stepIndex: MutableStateFlow<Int> = MutableStateFlow(INDEX_NO_STEPS)


    fun init(
        steps: List<TutorialStep>,
        onStepEnded: (TutorialStep) -> Unit,
        onNewStep: (TutorialStep) -> Unit,
        onCompleted: () -> Unit,
    ) {
        tutorialSteps.update { steps }
        stepEndedListener = onStepEnded
        newStepListener = onNewStep
        completionListener = onCompleted
        setStepIndex(0)
    }

    fun nextStep() {
        val index = stepIndex.value
        if (index == INDEX_NO_STEPS) return

        setStepIndex(index + 1)
    }

    fun lastStep() {
        val index = stepIndex.value
        if (index == INDEX_NO_STEPS) return

        Log.d(TAG, "Go to last step")
        setStepIndex(tutorialSteps.value.lastIndex)
    }

    fun setStepIndex(newIndex: Int) {
        val steps = tutorialSteps.value
        if (newIndex !in 0..(steps.lastIndex + 1)) return

        val oldStep = getCurrentStep()
        oldStep?.let { stepEnded ->
            stepEndedListener?.invoke(stepEnded)
        }

        if (newIndex == steps.lastIndex + 1) {
            completionListener?.invoke()
        } else {
            stepIndex.update { newIndex }
            getCurrentStep()?.let { newStep ->
                newStepListener?.invoke(newStep)
            }
        }
    }

    fun getCurrentStep(): TutorialStep? {
        return getStep(stepIndex.value)
    }

    fun getStep(index: Int): TutorialStep? {
        val steps = tutorialSteps.value
        if (index == INDEX_NO_STEPS || index !in steps.indices) return null

        return steps[index]
    }

    fun clear() {
        tutorialSteps.update { emptyList() }
        stepIndex.update { INDEX_NO_STEPS }
    }
}

private const val INDEX_NO_STEPS = -1
private const val TAG = "TutorialStepsOrchestrator"

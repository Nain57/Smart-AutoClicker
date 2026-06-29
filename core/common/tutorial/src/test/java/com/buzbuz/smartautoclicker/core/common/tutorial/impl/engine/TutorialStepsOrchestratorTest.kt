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

import android.os.Build

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TutorialStepsOrchestratorTest {

    private lateinit var orchestrator: TutorialStepsOrchestrator

    private val onStepEnded = mutableListOf<TutorialStep>()
    private val onNewStep = mutableListOf<TutorialStep>()
    private var completionCount = 0

    private fun makeStep(index: Int): TutorialStep.TutorialOverlay =
        TutorialStep.TutorialOverlay(
            stepStartCondition = TutorialStepStartCondition.Immediate,
            stepEndCondition = TutorialStepEndCondition.NextButton,
            contentTextResId = index,
        )

    @Before
    fun setUp() {
        orchestrator = TutorialStepsOrchestrator()
        onStepEnded.clear()
        onNewStep.clear()
        completionCount = 0
    }

    private fun initWith(vararg steps: TutorialStep) {
        orchestrator.init(
            steps = steps.toList(),
            onStepEnded = { onStepEnded.add(it) },
            onNewStep = { onNewStep.add(it) },
            onCompleted = { completionCount++ },
        )
    }

    // --- init ---

    @Test
    fun `init sets current step to first step`() {
        val step0 = makeStep(0)
        val step1 = makeStep(1)
        initWith(step0, step1)

        assertEquals(step0, orchestrator.getCurrentStep())
    }

    @Test
    fun `init invokes onNewStep with first step`() {
        val step0 = makeStep(0)
        initWith(step0)

        assertEquals(listOf(step0), onNewStep)
    }

    @Test
    fun `init does not invoke onStepEnded`() {
        initWith(makeStep(0))

        assertEquals(emptyList<TutorialStep>(), onStepEnded)
    }

    @Test
    fun `init does not invoke onCompleted`() {
        initWith(makeStep(0))

        assertEquals(0, completionCount)
    }

    // --- getStep ---

    @Test
    fun `getStep returns step at given valid index`() {
        val step0 = makeStep(0)
        val step1 = makeStep(1)
        initWith(step0, step1)

        assertEquals(step1, orchestrator.getStep(1))
    }

    @Test
    fun `getStep returns null for out-of-range index`() {
        initWith(makeStep(0))

        assertNull(orchestrator.getStep(5))
    }

    @Test
    fun `getStep returns null before init`() {
        assertNull(orchestrator.getStep(0))
    }

    // --- nextStep ---

    @Test
    fun `nextStep advances to second step`() {
        val step0 = makeStep(0)
        val step1 = makeStep(1)
        initWith(step0, step1)

        orchestrator.nextStep()

        assertEquals(step1, orchestrator.getCurrentStep())
    }

    @Test
    fun `nextStep invokes onStepEnded with old step`() {
        val step0 = makeStep(0)
        initWith(step0, makeStep(1))

        orchestrator.nextStep()

        assertEquals(listOf(step0), onStepEnded)
    }

    @Test
    fun `nextStep invokes onNewStep with new step`() {
        val step1 = makeStep(1)
        initWith(makeStep(0), step1)
        onNewStep.clear() // ignore init callback

        orchestrator.nextStep()

        assertEquals(listOf(step1), onNewStep)
    }

    @Test
    fun `nextStep on last step invokes onCompleted`() {
        initWith(makeStep(0))

        orchestrator.nextStep()

        assertEquals(1, completionCount)
    }

    @Test
    fun `nextStep on last step does not change current step`() {
        val step0 = makeStep(0)
        initWith(step0)

        orchestrator.nextStep()

        // stepIndex is never incremented past lastIndex; step is still accessible
        assertEquals(step0, orchestrator.getCurrentStep())
    }

    @Test
    fun `nextStep before init does nothing`() {
        orchestrator.nextStep()

        assertEquals(0, completionCount)
        assertEquals(emptyList<TutorialStep>(), onNewStep)
    }

    // --- lastStep ---

    @Test
    fun `lastStep jumps to last step`() {
        val step2 = makeStep(2)
        initWith(makeStep(0), makeStep(1), step2)

        orchestrator.lastStep()

        assertEquals(step2, orchestrator.getCurrentStep())
    }

    @Test
    fun `lastStep invokes onStepEnded with previous step`() {
        val step0 = makeStep(0)
        initWith(step0, makeStep(1), makeStep(2))

        orchestrator.lastStep()

        assertEquals(step0, onStepEnded.first())
    }

    @Test
    fun `lastStep invokes onNewStep with last step`() {
        val step2 = makeStep(2)
        initWith(makeStep(0), makeStep(1), step2)
        onNewStep.clear()

        orchestrator.lastStep()

        assertEquals(listOf(step2), onNewStep)
    }

    @Test
    fun `lastStep when already on last step invokes onStepEnded again`() {
        val step0 = makeStep(0)
        initWith(step0)

        orchestrator.lastStep()

        assertEquals(listOf(step0), onStepEnded)
    }

    @Test
    fun `lastStep before init does nothing`() {
        orchestrator.lastStep()

        assertEquals(0, completionCount)
        assertEquals(emptyList<TutorialStep>(), onNewStep)
    }

    // --- setStepIndex ---

    @Test
    fun `setStepIndex with valid index updates current step`() {
        val step1 = makeStep(1)
        initWith(makeStep(0), step1, makeStep(2))

        orchestrator.setStepIndex(1)

        assertEquals(step1, orchestrator.getCurrentStep())
    }

    @Test
    fun `setStepIndex invokes onStepEnded with previous step`() {
        val step0 = makeStep(0)
        initWith(step0, makeStep(1))

        orchestrator.setStepIndex(1)

        assertEquals(listOf(step0), onStepEnded)
    }

    @Test
    fun `setStepIndex invokes onNewStep with new step`() {
        val step1 = makeStep(1)
        initWith(makeStep(0), step1)
        onNewStep.clear()

        orchestrator.setStepIndex(1)

        assertEquals(listOf(step1), onNewStep)
    }

    @Test
    fun `setStepIndex with lastIndex plus one triggers completion`() {
        initWith(makeStep(0), makeStep(1))

        orchestrator.setStepIndex(2)

        assertEquals(1, completionCount)
    }

    @Test
    fun `setStepIndex with out-of-range index is ignored`() {
        val step0 = makeStep(0)
        initWith(step0)
        onNewStep.clear()

        orchestrator.setStepIndex(99)

        assertEquals(step0, orchestrator.getCurrentStep())
        assertEquals(emptyList<TutorialStep>(), onNewStep)
    }

    @Test
    fun `setStepIndex with negative index is ignored`() {
        val step0 = makeStep(0)
        initWith(step0)
        onNewStep.clear()

        orchestrator.setStepIndex(-1)

        assertEquals(step0, orchestrator.getCurrentStep())
        assertEquals(emptyList<TutorialStep>(), onNewStep)
    }

    // --- clear ---

    @Test
    fun `clear makes getCurrentStep return null`() {
        initWith(makeStep(0))

        orchestrator.clear()

        assertNull(orchestrator.getCurrentStep())
    }

    @Test
    fun `clear makes getStep return null`() {
        initWith(makeStep(0), makeStep(1))

        orchestrator.clear()

        assertNull(orchestrator.getStep(0))
    }

    @Test
    fun `nextStep after clear does nothing`() {
        initWith(makeStep(0))
        orchestrator.clear()
        completionCount = 0
        onNewStep.clear()

        orchestrator.nextStep()

        assertEquals(0, completionCount)
        assertEquals(emptyList<TutorialStep>(), onNewStep)
    }
}

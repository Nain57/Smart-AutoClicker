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

import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameRules
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.data.TutorialCompletionStateDataSource
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TutorialEngineTest {

    @Mock private lateinit var mockOverlayManager: OverlayManager
    @Mock private lateinit var mockMonitoredViewsManager: MonitoredViewsManager
    @Mock private lateinit var mockGameRules: TutorialGameRules
    @Mock private lateinit var mockOverlay: Overlay
    @Mock private lateinit var mockTutorialCompletionStateDataSource: TutorialCompletionStateDataSource

    private val backStackTopFlow = MutableStateFlow<Overlay?>(null)
    private val isStackHiddenFlow = MutableStateFlow(false)

    private lateinit var orchestrator: TutorialStepsOrchestrator
    private lateinit var engine: TutorialEngine
    private lateinit var mockCloseable: AutoCloseable

    @After
    fun tearDown() {
        mockCloseable.close()
    }

    @Before
    fun setUp() {
        mockCloseable = MockitoAnnotations.openMocks(this)

        whenever(mockOverlayManager.backStackTopFlow).thenReturn(backStackTopFlow)
        whenever(mockOverlayManager.isStackHidden).thenReturn(isStackHiddenFlow)
        whenever(mockGameRules.onStart(any(), any())).thenReturn(emptyMap())
        whenever(mockGameRules.onTimerTick(any(), any())).thenReturn(emptyMap())
        whenever(mockGameRules.onTargetHit(any(), any())).thenReturn(emptyMap())
        whenever(mockGameRules.getScore()).thenReturn(0)

        orchestrator = TutorialStepsOrchestrator()
        engine = TutorialEngine(
            ioDispatcher = UnconfinedTestDispatcher(),
            overlayManager = mockOverlayManager,
            monitoredViewsManager = mockMonitoredViewsManager,
            stepsOrchestrator = orchestrator,
            tutorialCompletionStateDataSource = mockTutorialCompletionStateDataSource,
        )
    }

    // --- helpers ---

    private fun makeGameSubject(): TutorialSubject.Game =
        TutorialSubject.Game(
            instructionsResId = 0,
            scoreToReach = 3,
            durationSeconds = 10L,
            rules = mockGameRules,
        )

    private fun makeStep(
        startCondition: TutorialStepStartCondition = TutorialStepStartCondition.Immediate,
        endCondition: TutorialStepEndCondition = TutorialStepEndCondition.NextButton,
        textResId: Int = 0,
    ): TutorialStep.TutorialOverlay =
        TutorialStep.TutorialOverlay(
            stepStartCondition = startCondition,
            stepEndCondition = endCondition,
            contentTextResId = textResId,
        )

    private fun makeTutorial(vararg steps: TutorialStep): Tutorial =
        Tutorial(
            info = TutorialInfo(id = "test", nameResId = 0, descResId = 0),
            subject = makeGameSubject(),
            steps = steps.toList(),
        )

    // --- isStarted ---

    @Test
    fun `isStarted returns false before any tutorial is started`() {
        assertFalse(engine.isStarted())
    }

    @Test
    fun `isStarted returns true after startTutorial`() {
        engine.startTutorial(makeTutorial(makeStep()))

        assertTrue(engine.isStarted())
    }

    @Test
    fun `isStarted returns false after stopTutorial`() {
        engine.startTutorial(makeTutorial(makeStep()))
        engine.stopTutorial()

        assertFalse(engine.isStarted())
    }

    // --- startTutorial ---

    @Test
    fun `startTutorial emits Started state`() {
        engine.startTutorial(makeTutorial(makeStep()))

        assertTrue(engine.tutorialState.value is TutorialState.Started)
    }

    @Test
    fun `startTutorial sets tutorial in Started state`() {
        val tutorial = makeTutorial(makeStep())

        engine.startTutorial(tutorial)

        assertEquals(tutorial, (engine.tutorialState.value as TutorialState.Started).tutorial)
    }

    @Test
    fun `startTutorial creates a non-null subject controller`() {
        engine.startTutorial(makeTutorial(makeStep()))

        assertNotNull(engine.tutorialSubjectController.value)
    }

    @Test
    fun `startTutorial returns the subject controller`() {
        val controller = engine.startTutorial(makeTutorial(makeStep()))

        assertSame(engine.tutorialSubjectController.value, controller)
    }

    @Test
    fun `startTutorial calls setExpectedViews on MonitoredViewsManager`() {
        engine.startTutorial(makeTutorial(makeStep()))

        verify(mockMonitoredViewsManager).setExpectedViews(any())
    }

    @Test
    fun `startTutorial is idempotent - second call returns same controller`() {
        val tutorial = makeTutorial(makeStep())
        val firstController = engine.startTutorial(tutorial)
        val secondController = engine.startTutorial(tutorial)

        assertSame(firstController, secondController)
    }

    @Test
    fun `startTutorial is idempotent - second call does not change state`() {
        val tutorial = makeTutorial(makeStep())
        engine.startTutorial(tutorial)
        val stateAfterFirst = engine.tutorialState.value

        engine.startTutorial(tutorial)

        assertEquals(stateAfterFirst, engine.tutorialState.value)
    }

    // --- stopTutorial ---

    @Test
    fun `stopTutorial emits Stopped state`() {
        engine.startTutorial(makeTutorial(makeStep()))
        engine.stopTutorial()

        assertEquals(TutorialState.Stopped, engine.tutorialState.value)
    }

    @Test
    fun `stopTutorial sets tutorialSubjectController to null`() {
        engine.startTutorial(makeTutorial(makeStep()))
        engine.stopTutorial()

        assertNull(engine.tutorialSubjectController.value)
    }

    @Test
    fun `stopTutorial calls clearExpectedViews on MonitoredViewsManager`() {
        engine.startTutorial(makeTutorial(makeStep()))
        engine.stopTutorial()

        verify(mockMonitoredViewsManager).clearExpectedViews()
    }

    // --- step start condition: Immediate ---

    @Test
    fun `Immediate start condition marks step as started`() {
        val step = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        val state = engine.tutorialState.value as TutorialState.Started
        assertTrue(state.isCurrentStepStarted)
    }

    @Test
    fun `Immediate start condition sets currentStep in state`() {
        val step = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step, state.currentStep)
    }

    // --- step end condition: Immediate ---

    @Test
    fun `Immediate start and Immediate end auto-advances to second step`() {
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.Immediate,
            textResId = 0,
        )
        val step1 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 1,
        )
        engine.startTutorial(makeTutorial(step0, step1))

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }

    @Test
    fun `single Immediate-start Immediate-end step completes the tutorial`() {
        engine.startTutorial(
            makeTutorial(
                makeStep(
                    startCondition = TutorialStepStartCondition.Immediate,
                    endCondition = TutorialStepEndCondition.Immediate,
                )
            )
        )

        val state = engine.tutorialState.value as TutorialState.Started
        assertTrue(state.isCompleted)
    }

    // --- onNextStepButtonPressed ---

    @Test
    fun `onNextStepButtonPressed does nothing when engine is stopped`() {
        engine.onNextStepButtonPressed()

        assertEquals(TutorialState.Stopped, engine.tutorialState.value)
    }

    @Test
    fun `onNextStepButtonPressed with NextButton end condition advances to next step`() {
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 0,
        )
        val step1 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 1,
        )
        engine.startTutorial(makeTutorial(step0, step1))

        engine.onNextStepButtonPressed()

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }

    @Test
    fun `onNextStepButtonPressed on last NextButton step completes the tutorial`() {
        engine.startTutorial(
            makeTutorial(
                makeStep(
                    startCondition = TutorialStepStartCondition.Immediate,
                    endCondition = TutorialStepEndCondition.NextButton,
                )
            )
        )

        engine.onNextStepButtonPressed()

        val state = engine.tutorialState.value as TutorialState.Started
        assertTrue(state.isCompleted)
    }

    @Test
    fun `onNextStepButtonPressed does nothing when step end condition is not NextButton`() {
        // MonitoredViewClicked end condition — pressing Next should not advance
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.MonitoredViewClicked(viewType),
        )
        engine.startTutorial(makeTutorial(step))

        engine.onNextStepButtonPressed()

        // Step should still be the same (not advanced to completion)
        val state = engine.tutorialState.value as TutorialState.Started
        assertFalse(state.isCompleted)
        assertEquals(step, state.currentStep)
    }

    // --- lastStep ---

    @Test
    fun `lastStep jumps to the last step`() {
        val step0 = makeStep(startCondition = TutorialStepStartCondition.Immediate, textResId = 0)
        val step1 = makeStep(startCondition = TutorialStepStartCondition.Immediate, textResId = 1)
        val step2 = makeStep(startCondition = TutorialStepStartCondition.Immediate, textResId = 2)
        engine.startTutorial(makeTutorial(step0, step1, step2))

        engine.lastStep()

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step2, state.currentStep)
    }

    // --- MonitoredViewClicked start condition ---

    @Test
    fun `MonitoredViewClicked start condition calls monitorNextClick on MonitoredViewsManager`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredViewClicked(viewType),
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), any())
    }

    @Test
    fun `MonitoredViewClicked start condition fires start when listener invoked`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredViewClicked(viewType),
            endCondition = TutorialStepEndCondition.NextButton,
        )

        val listenerCaptor = argumentCaptor<() -> Unit>()
        doAnswer { listenerCaptor.capture().invoke() }.`when`(mockMonitoredViewsManager)

        engine.startTutorial(makeTutorial(step))

        val stateBefore = engine.tutorialState.value as TutorialState.Started
        assertFalse(stateBefore.isCurrentStepStarted)

        // Capture the registered listener and invoke it
        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), listenerCaptor.capture())
        listenerCaptor.firstValue.invoke()

        val stateAfter = engine.tutorialState.value as TutorialState.Started
        assertTrue(stateAfter.isCurrentStepStarted)
    }

    // --- MonitoredViewClicked end condition ---

    @Test
    fun `MonitoredViewClicked end condition calls monitorNextClick after step starts`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.MonitoredViewClicked(viewType),
        )
        engine.startTutorial(makeTutorial(step))

        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), any())
    }

    @Test
    fun `MonitoredViewClicked end condition advances step when listener invoked`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.MonitoredViewClicked(viewType),
            textResId = 0,
        )
        val step1 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 1,
        )

        val listenerCaptor = argumentCaptor<() -> Unit>()
        engine.startTutorial(makeTutorial(step0, step1))

        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), listenerCaptor.capture())
        listenerCaptor.firstValue.invoke()

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }

    // --- onStepEnded cleanup ---

    @Test
    fun `onStepEnded with MonitoredViewClicked start condition stops click monitoring`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredViewClicked(viewType),
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 0,
        )
        val step1 = makeStep(textResId = 1)
        val listenerCaptor = argumentCaptor<() -> Unit>()

        engine.startTutorial(makeTutorial(step0, step1))
        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), listenerCaptor.capture())

        // Fire start condition so step0 becomes active
        listenerCaptor.firstValue.invoke()

        // Advance past step0 via NextButton
        engine.onNextStepButtonPressed()

        verify(mockMonitoredViewsManager).stopNextClickMonitoring(viewType)
    }

    @Test
    fun `onStepEnded with MonitoredViewClicked end condition stops click monitoring`() {
        val viewType = MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.Immediate,
            endCondition = TutorialStepEndCondition.MonitoredViewClicked(viewType),
            textResId = 0,
        )
        val step1 = makeStep(textResId = 1)
        val listenerCaptor = argumentCaptor<() -> Unit>()

        engine.startTutorial(makeTutorial(step0, step1))
        verify(mockMonitoredViewsManager).monitorNextClick(eq(viewType), listenerCaptor.capture())

        // Fire the end-condition listener to advance
        listenerCaptor.firstValue.invoke()

        verify(mockMonitoredViewsManager).stopNextClickMonitoring(viewType)
    }

    // --- NextOverlay start condition ---

    @Test
    fun `NextOverlay start condition does not mark step as started initially`() {
        val step = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(MonitoredOverlayType.MAIN_MENU),
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        val state = engine.tutorialState.value as TutorialState.Started
        assertFalse(state.isCurrentStepStarted)
    }

    @Test
    fun `NextOverlay start condition marks step as started when backStackTopFlow emits matching overlay`() {
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(MonitoredOverlayType.MAIN_MENU.name)
        val step = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(MonitoredOverlayType.MAIN_MENU),
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        backStackTopFlow.value = mockOverlay

        val state = engine.tutorialState.value as TutorialState.Started
        assertTrue(state.isCurrentStepStarted)
    }

    @Test
    fun `NextOverlay start condition does not fire when backStackTopFlow emits non-matching overlay`() {
        // mockOverlay returns "" by default from tutorialMonitoringTag(), which won't match MAIN_MENU
        val step = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(MonitoredOverlayType.MAIN_MENU),
            endCondition = TutorialStepEndCondition.NextButton,
        )
        engine.startTutorial(makeTutorial(step))

        // Emitting an overlay whose tag doesn't match MAIN_MENU must not trigger the condition
        backStackTopFlow.value = mockOverlay

        val state = engine.tutorialState.value as TutorialState.Started
        assertFalse(state.isCurrentStepStarted)
    }

    @Test
    fun `NextOverlay onStepEnded cancels the monitoring job`() {
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(MonitoredOverlayType.MAIN_MENU.name)
        val step0 = makeStep(
            startCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(MonitoredOverlayType.MAIN_MENU),
            endCondition = TutorialStepEndCondition.NextButton,
            textResId = 0,
        )
        val step1 = makeStep(textResId = 1)
        engine.startTutorial(makeTutorial(step0, step1))

        // Fire the MonitoredOverlayDisplayed condition and then advance via NextButton
        backStackTopFlow.value = mockOverlay
        engine.onNextStepButtonPressed()

        // After advancing, emitting another overlay change must NOT advance again
        backStackTopFlow.value = null
        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }

    // --- OverlayStackVisibilityChanged end condition ---

    @Test
    fun `OverlayStackVisibilityChanged end condition does not advance step while isStackHidden matches newVisibility`() {
        // newVisibility=false → expectedVisibility=false; isStackHiddenFlow starts at false → skip
        val step = TutorialStep.ChangeFloatingUiVisibility(
            stepStartCondition = TutorialStepStartCondition.Immediate,
            newVisibility = false,
        )
        engine.startTutorial(makeTutorial(step, makeStep()))

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step, state.currentStep)
    }

    @Test
    fun `OverlayStackVisibilityChanged end condition advances step when isStackHidden differs from newVisibility`() {
        // newVisibility=false → expectedVisibility=false; condition fires when isStackHidden=true
        val step0 = TutorialStep.ChangeFloatingUiVisibility(
            stepStartCondition = TutorialStepStartCondition.Immediate,
            newVisibility = false,
        )
        val step1 = makeStep(textResId = 1)
        engine.startTutorial(makeTutorial(step0, step1))

        isStackHiddenFlow.value = true

        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }

    @Test
    fun `OverlayStackVisibilityChanged onStepEnded cancels the monitoring job`() {
        val step0 = TutorialStep.ChangeFloatingUiVisibility(
            stepStartCondition = TutorialStepStartCondition.Immediate,
            newVisibility = false,
        )
        val step1 = makeStep(textResId = 1)
        engine.startTutorial(makeTutorial(step0, step1))

        // Advance by triggering the end condition
        isStackHiddenFlow.value = true

        // Further changes must NOT advance beyond step1
        isStackHiddenFlow.value = false
        val state = engine.tutorialState.value as TutorialState.Started
        assertEquals(step1, state.currentStep)
    }
}

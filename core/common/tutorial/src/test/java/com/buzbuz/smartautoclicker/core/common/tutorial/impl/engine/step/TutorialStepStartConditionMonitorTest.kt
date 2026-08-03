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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.step

import android.os.Build

import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialSubjectController
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.subject.QuickClickGameEngine
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.MonitoredViewsManagerImpl

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify as mockitoVerify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TutorialStepStartConditionMonitorTest {

    @Mock private lateinit var mockOverlayManager: OverlayManager
    @Mock private lateinit var mockMonitoredViewsManager: MonitoredViewsManagerImpl
    @Mock private lateinit var mockOverlay: Overlay

    /** Used for conditions that don't need a game engine. */
    private val mockSubjectController: TutorialSubjectController = mockk(relaxed = true)

    /** Concrete game engine mock — must be used when the condition casts to QuickClickGameEngine. */
    private val mockGameEngine: QuickClickGameEngine = mockk(relaxed = true)

    private val backStackTopFlow = MutableStateFlow<Overlay?>(null)

    private lateinit var monitor: TutorialStepStartConditionMonitor
    private lateinit var mockCloseable: AutoCloseable

    @Before
    fun setUp() {
        mockCloseable = MockitoAnnotations.openMocks(this)
        whenever(mockOverlayManager.backStackTopFlow).thenReturn(backStackTopFlow)
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)

        monitor = TutorialStepStartConditionMonitor(
            ioDispatcher = UnconfinedTestDispatcher(),
            monitoredViewsManager = mockMonitoredViewsManager,
            overlayManager = mockOverlayManager,
        )
    }

    @After
    fun tearDown() {
        mockCloseable.close()
    }

    // --- Immediate ---

    @Test
    fun `monitorCondition Immediate calls onConditionReached immediately`() {
        var reached = false
        monitor.monitorCondition(TutorialStepStartCondition.Immediate, mockSubjectController) { reached = true }
        assertEquals(true, reached)
    }

    @Test
    fun `stopMonitoring Immediate does nothing`() {
        monitor.stopMonitoring(TutorialStepStartCondition.Immediate, mockSubjectController)
        // no crash expected
    }

    // --- MonitoredViewClicked ---

    @Test
    fun `monitorCondition MonitoredViewClicked registers click listener`() {
        val condition = TutorialStepStartCondition.MonitoredViewClicked(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
        val listenerCaptor = argumentCaptor<() -> Unit>()

        monitor.monitorCondition(condition, mockSubjectController) {}

        mockitoVerify(mockMonitoredViewsManager).monitorNextClick(eq(MonitoredViewType.MAIN_MENU_BUTTON_PLAY), listenerCaptor.capture())
    }

    @Test
    fun `monitorCondition MonitoredViewClicked listener triggers onConditionReached`() {
        val condition = TutorialStepStartCondition.MonitoredViewClicked(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
        val listenerCaptor = argumentCaptor<() -> Unit>()
        var reached = false

        monitor.monitorCondition(condition, mockSubjectController) { reached = true }
        mockitoVerify(mockMonitoredViewsManager).monitorNextClick(eq(MonitoredViewType.MAIN_MENU_BUTTON_PLAY), listenerCaptor.capture())
        listenerCaptor.firstValue.invoke()

        assertEquals(true, reached)
    }

    @Test
    fun `stopMonitoring MonitoredViewClicked stops click monitoring`() {
        val condition = TutorialStepStartCondition.MonitoredViewClicked(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
        monitor.stopMonitoring(condition, mockSubjectController)
        mockitoVerify(mockMonitoredViewsManager).stopNextClickMonitoring(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
    }

    // --- MonitoredTextInput ---

    @Test
    fun `monitorCondition MonitoredTextInput registers text listener`() {
        val condition = TutorialStepStartCondition.MonitoredTextInput(
            type = MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_TEXT_TO_DETECT,
            expectedText = "hello",
        )
        val listenerCaptor = argumentCaptor<() -> Unit>()

        monitor.monitorCondition(condition, mockSubjectController) {}

        mockitoVerify(mockMonitoredViewsManager).monitorText(
            eq(MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_TEXT_TO_DETECT),
            eq("hello"),
            listenerCaptor.capture(),
        )
    }

    @Test
    fun `monitorCondition MonitoredTextInput listener triggers onConditionReached`() {
        val condition = TutorialStepStartCondition.MonitoredTextInput(
            type = MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_TEXT_TO_DETECT,
            expectedText = "hello",
        )
        val listenerCaptor = argumentCaptor<() -> Unit>()
        var reached = false

        monitor.monitorCondition(condition, mockSubjectController) { reached = true }
        mockitoVerify(mockMonitoredViewsManager).monitorText(any(), any(), listenerCaptor.capture())
        listenerCaptor.firstValue.invoke()

        assertEquals(true, reached)
    }

    @Test
    fun `stopMonitoring MonitoredTextInput stops monitoring`() {
        val condition = TutorialStepStartCondition.MonitoredTextInput(
            type = MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_TEXT_TO_DETECT,
            expectedText = "hello",
        )
        monitor.stopMonitoring(condition, mockSubjectController)
        mockitoVerify(mockMonitoredViewsManager).stopMonitoring(MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_TEXT_TO_DETECT)
    }

    // --- MonitoredNumberInput ---

    @Test
    fun `monitorCondition MonitoredNumberInput registers number listener`() {
        val condition = TutorialStepStartCondition.MonitoredNumberInput(
            type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
            expectedNumber = 42.0,
        )
        val listenerCaptor = argumentCaptor<() -> Unit>()

        monitor.monitorCondition(condition, mockSubjectController) {}

        mockitoVerify(mockMonitoredViewsManager).monitorNumber(
            eq(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT),
            eq(42.0),
            listenerCaptor.capture(),
        )
    }

    @Test
    fun `monitorCondition MonitoredNumberInput listener triggers onConditionReached`() {
        val condition = TutorialStepStartCondition.MonitoredNumberInput(
            type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
            expectedNumber = 42.0,
        )
        val listenerCaptor = argumentCaptor<() -> Unit>()
        var reached = false

        monitor.monitorCondition(condition, mockSubjectController) { reached = true }
        mockitoVerify(mockMonitoredViewsManager).monitorNumber(any(), any(), listenerCaptor.capture())
        listenerCaptor.firstValue.invoke()

        assertEquals(true, reached)
    }

    @Test
    fun `stopMonitoring MonitoredNumberInput stops monitoring`() {
        val condition = TutorialStepStartCondition.MonitoredNumberInput(
            type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
            expectedNumber = 42.0,
        )
        monitor.stopMonitoring(condition, mockSubjectController)
        mockitoVerify(mockMonitoredViewsManager).stopMonitoring(MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT)
    }

    // --- GameWon ---

    @Test
    fun `monitorCondition GameWon calls onConditionReached when game is won`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit
        var reached = false

        monitor.monitorCondition(TutorialStepStartCondition.GameWon, mockGameEngine) { reached = true }
        listenerSlot.captured?.invoke(true)

        assertEquals(true, reached)
    }

    @Test
    fun `monitorCondition GameWon returns true when consumed`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit

        monitor.monitorCondition(TutorialStepStartCondition.GameWon, mockGameEngine) {}
        val consumed = listenerSlot.captured?.invoke(true)

        assertEquals(true, consumed)
    }

    @Test
    fun `monitorCondition GameWon does not call onConditionReached when game is lost`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit
        var reached = false

        monitor.monitorCondition(TutorialStepStartCondition.GameWon, mockGameEngine) { reached = true }
        listenerSlot.captured?.invoke(false)

        assertEquals(false, reached)
    }

    @Test
    fun `monitorCondition GameWon returns false when not consumed`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit

        monitor.monitorCondition(TutorialStepStartCondition.GameWon, mockGameEngine) {}
        val consumed = listenerSlot.captured?.invoke(false)

        assertEquals(false, consumed)
    }

    @Test
    fun `stopMonitoring GameWon clears game completion listener`() {
        monitor.stopMonitoring(TutorialStepStartCondition.GameWon, mockGameEngine)
        verify { mockGameEngine.monitorNextCompletion(null) }
    }

    // --- GameLost ---

    @Test
    fun `monitorCondition GameLost calls onConditionReached when game is lost`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit
        var reached = false

        monitor.monitorCondition(TutorialStepStartCondition.GameLost, mockGameEngine) { reached = true }
        listenerSlot.captured?.invoke(false)

        assertEquals(true, reached)
    }

    @Test
    fun `monitorCondition GameLost returns true when consumed`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit

        monitor.monitorCondition(TutorialStepStartCondition.GameLost, mockGameEngine) {}
        val consumed = listenerSlot.captured?.invoke(false)

        assertEquals(true, consumed)
    }

    @Test
    fun `monitorCondition GameLost does not call onConditionReached when game is won`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit
        var reached = false

        monitor.monitorCondition(TutorialStepStartCondition.GameLost, mockGameEngine) { reached = true }
        listenerSlot.captured?.invoke(true)

        assertEquals(false, reached)
    }

    @Test
    fun `monitorCondition GameLost returns false when not consumed`() {
        val listenerSlot = slot<((Boolean) -> Boolean)?>()
        every { mockGameEngine.monitorNextCompletion(captureNullable(listenerSlot)) } returns Unit

        monitor.monitorCondition(TutorialStepStartCondition.GameLost, mockGameEngine) {}
        val consumed = listenerSlot.captured?.invoke(true)

        assertEquals(false, consumed)
    }

    @Test
    fun `stopMonitoring GameLost clears game completion listener`() {
        monitor.stopMonitoring(TutorialStepStartCondition.GameLost, mockGameEngine)
        verify { mockGameEngine.monitorNextCompletion(null) }
    }

    // --- MonitoredOverlayDisplayed: normal case (not already on overlay) ---

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed triggers when expected overlay appears`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        backStackTopFlow.value = mockOverlay

        assertEquals(true, reached)
    }

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed does not trigger for unrelated overlay`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        val otherOverlay = org.mockito.Mockito.mock(Overlay::class.java)
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)
        whenever(otherOverlay.tutorialMonitoringTag()).thenReturn(MonitoredOverlayType.EVENT.name)

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        backStackTopFlow.value = otherOverlay

        assertEquals(false, reached)
    }

    // --- MonitoredOverlayDisplayed: already on the expected overlay (new use case) ---

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed does not trigger immediately when already on expected overlay`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(mockOverlay)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)
        backStackTopFlow.value = mockOverlay

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        assertEquals(false, reached)
    }

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed does not trigger when user stays on expected overlay`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(mockOverlay)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)
        backStackTopFlow.value = mockOverlay

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        // Emit the same overlay again — user hasn't left
        backStackTopFlow.value = mockOverlay

        assertEquals(false, reached)
    }

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed triggers after user leaves then returns to expected overlay`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        val otherOverlay = org.mockito.Mockito.mock(Overlay::class.java)
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(mockOverlay)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)
        whenever(otherOverlay.tutorialMonitoringTag()).thenReturn(MonitoredOverlayType.EVENT.name)
        backStackTopFlow.value = mockOverlay

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        // User navigates away
        backStackTopFlow.value = otherOverlay
        assertEquals(false, reached)

        // User comes back to the expected overlay
        backStackTopFlow.value = mockOverlay
        assertEquals(true, reached)
    }

    @Test
    fun `monitorCondition MonitoredOverlayDisplayed does not trigger again after condition reached`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        val otherOverlay = org.mockito.Mockito.mock(Overlay::class.java)
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)
        whenever(otherOverlay.tutorialMonitoringTag()).thenReturn(MonitoredOverlayType.EVENT.name)

        var reachedCount = 0
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reachedCount++ }

        backStackTopFlow.value = mockOverlay
        assertEquals(1, reachedCount)

        // Navigate away and back — job should be cancelled, no second callback
        backStackTopFlow.value = otherOverlay
        backStackTopFlow.value = mockOverlay
        assertEquals(1, reachedCount)
    }

    @Test
    fun `stopMonitoring MonitoredOverlayDisplayed cancels the monitoring job`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        monitor.stopMonitoring(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        )

        backStackTopFlow.value = mockOverlay
        assertEquals(false, reached)
    }

    @Test
    fun `clearMonitoring cancels overlay monitoring job`() {
        val overlayType = MonitoredOverlayType.SCENARIO
        whenever(mockOverlayManager.getBackStackTop()).thenReturn(null)
        whenever(mockOverlay.tutorialMonitoringTag()).thenReturn(overlayType.name)

        var reached = false
        monitor.monitorCondition(
            TutorialStepStartCondition.MonitoredOverlayDisplayed(overlayType),
            mockSubjectController,
        ) { reached = true }

        monitor.clearMonitoring()

        backStackTopFlow.value = mockOverlay
        assertEquals(false, reached)
    }
}

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
package com.buzbuz.smartautoclicker.core.processing.tests.processor

import android.graphics.Bitmap
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter.OperationType
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation.EQUALS
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.utils.anyNotNull
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doAnswer
import org.robolectric.annotation.Config


/** Test file for advanced [ScenarioProcessor] tests with complex use cases. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ProcessingTests {

    /** Interface to be mocked in order to verify the calls to the stop listener. */
    private interface StopRequestListener {
        fun onStopRequested()
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    internal interface BitmapSupplier {
        suspend fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }

    /** Provides the tests scenarios.  */
    private val testsData: ProcessingTestData = ProcessingTestData

    @Mock private lateinit var mockScalingManager: ScalingManager
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockImageDetector: ImageDetector
    @Mock private lateinit var mockAndroidExecutor: AndroidActionExecutor
    @Mock private lateinit var mockEndListener: StopRequestListener
    @Mock private lateinit var mockProcessingListener: SmartProcessingListener

    /** The object under test. */
    private lateinit var scenarioProcessor: ScenarioProcessor

    private fun createScenarioProcessor(testScenario: TestScenario) =
        ScenarioProcessor(
            processingTag = "tests",
            scalingManager = mockScalingManager,
            randomize = testScenario.scenario.randomize,
            screenEvents = testScenario.screenEvents,
            triggerEvents = testScenario.triggerEvents,
            counters = testScenario.counters,
            imageDetector = mockImageDetector,
            androidExecutor = mockAndroidExecutor,
            bitmapSupplier = mockBitmapSupplier::getBitmap,
            onStopRequested = mockEndListener::onStopRequested,
            progressListener = mockProcessingListener,
        )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())

        `when`(mockScalingManager.scaleUpDetectionResult(anyNotNull()))
            .doAnswer { invocation -> invocation.getArgument(0) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testsData.reset()
    }

    /**
     * Use case: 3 events, 1st one starts enabled, 2nd one starts disabled.
     * First frame: The 1st event is detected, disables itself and enables the 2nd one.
     * Second frame: The 2nd event is detected, disables itself and enables the 1st one.
     * Repeat that twice and check events states each time.
     *
     * Unit test for [#501](https://github.com/Nain57/Smart-AutoClicker/issues/501)
     */
    @Test
    fun `Event ordering is kept after changing event enabled states`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val eventId3 = testsData.newEventId()
        val testConditionEvt1 = testsData.newTestImageCondition(eventId1)
        val testConditionEvt2 = testsData.newTestImageCondition(eventId2)
        val testConditionEvt3 = testsData.newTestImageCondition(eventId3)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId1,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    conditions = listOf(testConditionEvt1),
                    actions = listOf(
                        testsData.newToggleEventAction(
                            eventId = eventId1,
                            toggles = listOf(
                                TestEventToggle(eventId1, ToggleEvent.ToggleType.DISABLE),
                                TestEventToggle(eventId2, ToggleEvent.ToggleType.ENABLE)
                            ),
                        )
                    ),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId2,
                    scenarioId = scenarioId,
                    enabledOnStart = false,
                    conditions = listOf(testConditionEvt2),
                    actions = listOf(
                        testsData.newToggleEventAction(
                            eventId = eventId2,
                            toggles = listOf(
                                TestEventToggle(eventId1, ToggleEvent.ToggleType.ENABLE),
                                TestEventToggle(eventId2, ToggleEvent.ToggleType.DISABLE)
                            ),
                        )
                    ),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId3,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    conditions = listOf(testConditionEvt3),
                    actions = listOf(),
                ),
            ),
        )

        // Mock the bitmaps for each image conditions
        mockBitmapSupplier.apply {
            mockBitmapProviding(testConditionEvt1)
            mockBitmapProviding(testConditionEvt2)
            mockBitmapProviding(testConditionEvt3)
        }
        // Mock the scale size for each image conditions
        mockScalingManager.apply {
            mockScaling(testConditionEvt1)
            mockScaling(testConditionEvt2)
            mockScaling(testConditionEvt3)
        }
        // Mock that all bitmaps are matching
        mockImageDetector.mockAllDetectionResult(
            testConditions = listOf(testConditionEvt1, testConditionEvt2, testConditionEvt3),
            areAllDetected = true,
        )

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: First frame, event1 is detected.
        // Then: Event1 actions are executed: event1 is disabled, event2 is enabled
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        assertFalse("First frame: event1 should be disabled",
            scenarioProcessor.processingState.isEventEnabled(eventId1.databaseId))
        assertTrue("First frame: event2 should be enabled",
            scenarioProcessor.processingState.isEventEnabled(eventId2.databaseId))

        // When: Second frame, event1 is disabled allowing event2 to be detected.
        // Then: Event2 actions are executed: event1 is enabled, event2 is disabled
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        assertTrue("Second frame: event1 should be enabled",
            scenarioProcessor.processingState.isEventEnabled(eventId1.databaseId))
        assertFalse("Second frame: event2 should be disabled",
            scenarioProcessor.processingState.isEventEnabled(eventId2.databaseId))

        // When: Third frame, event1 is detected.
        // Then: Event1 actions are executed: event1 is disabled, event2 is enabled
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        assertFalse("Third frame: event1 should be disabled",
            scenarioProcessor.processingState.isEventEnabled(eventId1.databaseId))
        assertTrue("Third frame: event2 should be enabled",
            scenarioProcessor.processingState.isEventEnabled(eventId2.databaseId))

        // When: Fourth frame, event1 is disabled allowing event2 to be detected.
        // Then: Event2 actions are executed: event1 is enabled, event2 is disabled
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        assertTrue("Fourth frame: event1 should be enabled",
            scenarioProcessor.processingState.isEventEnabled(eventId1.databaseId))
        assertFalse("Fourth frame: event2 should be disabled",
            scenarioProcessor.processingState.isEventEnabled(eventId2.databaseId))
    }

    /**
     * Use case: 3 events, all enabled. The first one have keepDetecting set to true, the others to false. All
     * image conditions will be detected.
     * Expected behaviour: The first event is detected; as it is keepDetecting, it continues and the second event is
     * detected; as it is not keepDetecting, it stops here and event3 is not checked.
     */
    @Test
    fun `Event keepDetecting property behaviour`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val eventId3 = testsData.newEventId()
        val testConditionEvt1 = testsData.newTestImageCondition(eventId1)
        val testConditionEvt2 = testsData.newTestImageCondition(eventId2)
        val testConditionEvt3 = testsData.newTestImageCondition(eventId3)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId1,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    keepDetecting = true,
                    conditions = listOf(testConditionEvt1),
                    actions = listOf(testsData.newPauseAction(eventId1)),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId2,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    keepDetecting = false,
                    conditions = listOf(testConditionEvt2),
                    actions = listOf(testsData.newPauseAction(eventId2)),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId3,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    keepDetecting = false,
                    conditions = listOf(testConditionEvt3),
                    actions = listOf(testsData.newPauseAction(eventId3)),
                ),
            ),
        )

        // Mock the bitmaps for each image conditions
        mockBitmapSupplier.apply {
            mockBitmapProviding(testConditionEvt1)
            mockBitmapProviding(testConditionEvt2)
            mockBitmapProviding(testConditionEvt3)
        }
        // Mock the scale size for each image conditions
        mockScalingManager.apply {
            mockScaling(testConditionEvt1)
            mockScaling(testConditionEvt2)
            mockScaling(testConditionEvt3)
        }
        // Mock detection results for each condition.
        mockImageDetector.apply {
            mockDetectionResult(testConditionEvt1, true)
            mockDetectionResult(testConditionEvt2, true)
        }

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // Then: Event1 and Event2 should be triggered, Event3 should not
        inOrder(mockProcessingListener).apply {
            mockProcessingListener.verifyImageConditionProcessed(testConditionEvt1, true)
            mockProcessingListener.verifyImageConditionProcessed(testConditionEvt2, true)
        }
        mockImageDetector.verifyConditionNeverProcessed(testConditionEvt3)
    }

    /**
     * Use case: 3 trigger events, all enabled. The first and last one are already fulfilled (=0 counters), the second
     * one is not.
     * Expected behaviour: all trigger events are verified, only 1 & 3 are fulfilled
     */
    @Test
    fun `Processor checks all TriggerEvents even when one is triggered`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val eventId3 = testsData.newEventId()
        val counterA = Counter("A", 0.0, scenarioId)
        val counterB = Counter("B", 0.0, scenarioId)
        val counterC = Counter("C", 0.0, scenarioId)

        val testEvent1 = testsData.newTestTriggerEvent(
            eventId = eventId1,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId1, "A", EQUALS, 0.0)),
            actions = listOf(testsData.newCounterAction(eventId1, "A", OperationType.ADD, 0.0)),
        )
        val testEvent2 = testsData.newTestTriggerEvent(
            eventId = eventId2,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId2, "B", EQUALS, 10.0)),
            actions = listOf(testsData.newCounterAction(eventId1, "B", OperationType.ADD, 0.0)),
        )
        val testEvent3 = testsData.newTestTriggerEvent(
            eventId = eventId3,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId3, "C", EQUALS, 0.0)),
            actions = listOf(testsData.newCounterAction(eventId1, "C", OperationType.ADD, 0.0)),
        )
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            triggerEvents = listOf(testEvent1, testEvent2, testEvent3),
            counters = listOf(counterA, counterB, counterC),
        )

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // All trigger should be interpreted, no keepDetecting value in TriggerEvents
        mockProcessingListener.verifyTriggerEventFulfilled(testEvent1, true)
        mockProcessingListener.verifyTriggerEventFulfilled(testEvent2, false)
        mockProcessingListener.verifyTriggerEventFulfilled(testEvent3, true)
    }

    /**
     * Use case: 2 image events, all enabled. Both should not be detected. First one is present on the frame, second is not
     * Expected behaviour: First one should not be fulfilled, second one should
     *
     * Unit test for [#551](https://github.com/Nain57/Smart-AutoClicker/issues/551)
     */
    @Test
    fun `isAbsent ImageConditions should be triggered when not detected`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val testConditionEvt1 = testsData.newTestImageCondition(eventId1, shouldBeDetected = false)
        val testConditionEvt2 = testsData.newTestImageCondition(eventId2, shouldBeDetected = false)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId1,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    keepDetecting = true,
                    conditions = listOf(testConditionEvt1),
                    actions = listOf(testsData.newPauseAction(eventId1)),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId2,
                    scenarioId = scenarioId,
                    enabledOnStart = true,
                    keepDetecting = true,
                    conditions = listOf(testConditionEvt2),
                    actions = listOf(testsData.newPauseAction(eventId2)),
                ),
            ),
        )

        // Mock the bitmaps for each image conditions
        mockBitmapSupplier.apply {
            mockBitmapProviding(testConditionEvt1)
            mockBitmapProviding(testConditionEvt2)
        }
        // Mock the scale size for each image conditions
        mockScalingManager.apply {
            mockScaling(testConditionEvt1)
            mockScaling(testConditionEvt2)
        }
        // Mock detection results for each condition.
        mockImageDetector.apply {
            mockDetectionResult(testConditionEvt1, true)
            mockDetectionResult(testConditionEvt2, false)
        }
        // Keep track of event fulfillment results
        val eventsFulfilled = mockProcessingListener.monitorImageEventProcessing(testScenario.screenEvents)

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // Then
        // Event1 should be detected, Event2 should not
        inOrder(mockProcessingListener).apply {
            mockProcessingListener.verifyImageConditionProcessed(testConditionEvt1, true)
            mockProcessingListener.verifyImageConditionProcessed(testConditionEvt2, false)
        }
        // Event1 not be triggered, Event2 should
        assertTrue(eventsFulfilled[eventId1.databaseId] == false)
        assertTrue(eventsFulfilled[eventId2.databaseId] == true)
    }

    // ---- ScreenEvent.cooldownMs tests ----

    /**
     * Use case: 1 event with no cooldown, conditions always detected.
     * Expected behaviour: the event is processed on every frame with no skips.
     */
    @Test
    fun `Event with no cooldown is processed on every frame`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId = testsData.newEventId()
        val testCondition = testsData.newTestImageCondition(eventId)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId,
                    scenarioId = scenarioId,
                    cooldownMs = 0L,
                    conditions = listOf(testCondition),
                    actions = listOf(testsData.newPauseAction(eventId)),
                ),
            ),
        )

        mockBitmapSupplier.mockBitmapProviding(testCondition)
        mockScalingManager.mockScaling(testCondition)
        mockImageDetector.mockDetectionResult(testCondition, true)

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: process 2 frames
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // Then: condition is checked on both frames
        mockProcessingListener.verifyImageConditionProcessed(testCondition, true, processedCount = 2)
    }

    /**
     * Use case: 1 event with a long cooldown, conditions always detected.
     * Expected behaviour: the event is processed on the first frame and its cooldown starts. On the second frame, the
     * cooldown is still active so the event is skipped entirely.
     */
    @Test
    fun `Event with active cooldown is skipped on the next frame`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId = testsData.newEventId()
        val testCondition = testsData.newTestImageCondition(eventId)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId,
                    scenarioId = scenarioId,
                    cooldownMs = 10_000L,
                    conditions = listOf(testCondition),
                    actions = listOf(testsData.newPauseAction(eventId)),
                ),
            ),
        )

        mockBitmapSupplier.mockBitmapProviding(testCondition)
        mockScalingManager.mockScaling(testCondition)
        mockImageDetector.mockDetectionResult(testCondition, true)

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: process frame 1 (event detected, cooldown starts), then frame 2 immediately
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // Then: condition is only checked once — the event is skipped on frame 2 due to the active cooldown
        mockProcessingListener.verifyImageConditionProcessed(testCondition, true, processedCount = 1)
    }

    /**
     * Use case: 1 event with a very short cooldown, conditions always detected.
     * Expected behaviour: after the cooldown expires between two frames, the event is processed again.
     */
    @Test
    fun `Event is processed again after its cooldown expires`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId = testsData.newEventId()
        val testCondition = testsData.newTestImageCondition(eventId)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId,
                    scenarioId = scenarioId,
                    cooldownMs = 1L,
                    conditions = listOf(testCondition),
                    actions = listOf(testsData.newPauseAction(eventId)),
                ),
            ),
        )

        mockBitmapSupplier.mockBitmapProviding(testCondition)
        mockScalingManager.mockScaling(testCondition)
        mockImageDetector.mockDetectionResult(testCondition, true)

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: process frame 1 (event detected, cooldown starts), wait for cooldown to expire, then frame 2
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        Thread.sleep(10L)
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // Then: condition is checked on both frames — the expired cooldown does not block the second frame
        mockProcessingListener.verifyImageConditionProcessed(testCondition, true, processedCount = 2)
    }

    /**
     * Use case: 2 events, event1 has a long cooldown, event2 has no cooldown. Both are detected.
     * event1 has keepDetecting = true so event2 is also evaluated on the same frame.
     * Expected behaviour: on frame 2, event1 is skipped due to cooldown but event2 is still processed normally.
     */
    @Test
    fun `Cooldown on one event does not affect sibling events`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val testCondition1 = testsData.newTestImageCondition(eventId1)
        val testCondition2 = testsData.newTestImageCondition(eventId2)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId1,
                    scenarioId = scenarioId,
                    keepDetecting = true,
                    cooldownMs = 10_000L,
                    conditions = listOf(testCondition1),
                    actions = listOf(testsData.newPauseAction(eventId1)),
                ),
                testsData.newTestImageEvent(
                    eventId = eventId2,
                    scenarioId = scenarioId,
                    keepDetecting = true,
                    cooldownMs = 0L,
                    conditions = listOf(testCondition2),
                    actions = listOf(testsData.newPauseAction(eventId2)),
                ),
            ),
        )

        mockBitmapSupplier.apply {
            mockBitmapProviding(testCondition1)
            mockBitmapProviding(testCondition2)
        }
        mockScalingManager.apply {
            mockScaling(testCondition1)
            mockScaling(testCondition2)
        }
        mockImageDetector.apply {
            mockDetectionResult(testCondition1, true)
            mockDetectionResult(testCondition2, true)
        }

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: process 2 frames; event1 cooldown is active on frame 2
        scenarioProcessor.process(testsData.newMockedScreenBitmap())
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // Then: event1 condition is only checked once (skipped on frame 2); event2 is checked on both frames
        mockProcessingListener.verifyImageConditionProcessed(testCondition1, true, processedCount = 1)
        mockProcessingListener.verifyImageConditionProcessed(testCondition2, true, processedCount = 2)
    }

    /**
     * Use case: 1 event with a long cooldown. On the first frame the condition is NOT detected so the event is not
     * fulfilled. On the second frame the condition IS detected and the event is fulfilled.
     * Expected behaviour: the cooldown only starts when the event is fulfilled, not when it is merely checked.
     * So after the second frame the cooldown starts, and the third frame should be skipped.
     */
    @Test
    fun `Cooldown starts only when event is fulfilled, not when conditions are checked`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId = testsData.newEventId()
        val testCondition = testsData.newTestImageCondition(eventId)
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            screenEvents = listOf(
                testsData.newTestImageEvent(
                    eventId = eventId,
                    scenarioId = scenarioId,
                    cooldownMs = 10_000L,
                    conditions = listOf(testCondition),
                    actions = listOf(testsData.newPauseAction(eventId)),
                ),
            ),
        )

        mockBitmapSupplier.mockBitmapProviding(testCondition)
        mockScalingManager.mockScaling(testCondition)

        scenarioProcessor = createScenarioProcessor(testScenario)

        // When: frame 1 — condition not detected (no fulfillment, no cooldown)
        mockImageDetector.mockDetectionResult(testCondition, false)
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // When: frame 2 — condition detected (event fulfilled, cooldown now starts)
        mockImageDetector.mockDetectionResult(testCondition, true)
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // When: frame 3 — cooldown is active, event should be skipped
        scenarioProcessor.process(testsData.newMockedScreenBitmap())

        // Then: condition is checked on frames 1 and 2 (not detected, then detected), but not on frame 3
        mockProcessingListener.verifyImageConditionProcessed(testCondition, false, processedCount = 1)
        mockProcessingListener.verifyImageConditionProcessed(testCondition, true, processedCount = 1)
    }

    /**
     * Use case: 2 trigger events, all enabled. First one is fulfilled and disables the second one.
     * This verifies the case where the trigger event list is modifying itself during the same iteration.
     */
    @Test
    fun `TriggerEvent concurrent modification`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val counterA = Counter("A", 0.0, scenarioId)
        val testEvent1 = testsData.newTestTriggerEvent(
            eventId = eventId1,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId1, "A", EQUALS, 0.0)),
            actions = listOf(
                testsData.newToggleEventAction(
                    eventId = eventId1,
                    toggles = listOf(
                        TestEventToggle(eventId2, ToggleEvent.ToggleType.DISABLE)
                    ),
                )
            ),
        )
        val testEvent2 = testsData.newTestTriggerEvent(
            eventId = eventId2,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId2, "A", EQUALS, 0.0)),
            actions = listOf(testsData.newCounterAction(eventId2, "A", OperationType.ADD, 10.0)),
        )
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            triggerEvents = listOf(testEvent1, testEvent2),
            counters = listOf(counterA),
        )

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // Then: Should not crash (ConcurrentModification), event1 is fulfilled, event2 is disabled by event1
        mockProcessingListener.verifyTriggerEventFulfilled(testEvent1, true)
        mockProcessingListener.verifyTriggerEventNotProcessed(testEvent2)
    }
}
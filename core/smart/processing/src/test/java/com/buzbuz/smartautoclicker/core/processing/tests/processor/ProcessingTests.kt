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
import com.buzbuz.smartautoclicker.core.detection.ScreenDetector
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter.OperationType
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.EQUALS
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener
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
import org.mockito.MockitoAnnotations
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
        suspend fun getBitmap(condition: ImageCondition): Bitmap
    }

    /** Provides the tests scenarios.  */
    private val testsData: ProcessingTestData = ProcessingTestData

    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockScreenDetector: ScreenDetector
    @Mock private lateinit var mockAndroidExecutor: SmartActionExecutor
    @Mock private lateinit var mockEndListener: StopRequestListener
    @Mock private lateinit var mockProcessingListener: ScenarioProcessingListener

    /** The object under test. */
    private lateinit var scenarioProcessor: ScenarioProcessor

    private fun createScenarioProcessor(testScenario: TestScenario) =
        ScenarioProcessor(
            processingTag = "tests",
            detectionQuality = testScenario.scenario.detectionQuality,
            randomize = testScenario.scenario.randomize,
            imageEvents = testScenario.imageEvents,
            triggerEvents = testScenario.triggerEvents,
            screenDetector = mockScreenDetector,
            androidExecutor = mockAndroidExecutor,
            bitmapSupplier = mockBitmapSupplier::getBitmap,
            onStopRequested = mockEndListener::onStopRequested,
            progressListener = mockProcessingListener,
        )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())
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
            imageEvents = listOf(
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
        // Mock that all bitmaps are matching
        mockScreenDetector.mockAllDetectionResult(
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
            imageEvents = listOf(
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
        // Mock detection results for each condition.
        mockScreenDetector.apply {
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
        mockScreenDetector.verifyConditionNeverProcessed(testConditionEvt3)
    }

    /**
     * Use case: 3 trigger events, all enabled. The first and last one are already fulfilled (=0 counters), the second
     * one is not.
     * Expected behaviour: all trigger events are verify, only 1 & 3 are fulfilled
     */
    @Test
    fun `Processor checks all TriggerEvents even when one is triggered`() = runTest {
        // Given
        val scenarioId = testsData.newScenarioId()
        val eventId1 = testsData.newEventId()
        val eventId2 = testsData.newEventId()
        val eventId3 = testsData.newEventId()
        val testEvent1 = testsData.newTestTriggerEvent(
            eventId = eventId1,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId1, "A", EQUALS, 0)),
            actions = listOf(testsData.newCounterAction(eventId1, "A", OperationType.ADD, 0)),
        )
        val testEvent2 = testsData.newTestTriggerEvent(
            eventId = eventId2,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId2, "B", EQUALS, 10)),
            actions = listOf(testsData.newCounterAction(eventId1, "B", OperationType.ADD, 0)),
        )
        val testEvent3 = testsData.newTestTriggerEvent(
            eventId = eventId3,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId3, "C", EQUALS, 0)),
            actions = listOf(testsData.newCounterAction(eventId1, "C", OperationType.ADD, 0)),
        )
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            triggerEvents = listOf(testEvent1, testEvent2, testEvent3)
        )

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // All trigger should be interpreted, no keepDetecting value in TriggerEvents
        mockProcessingListener.verifyTriggerEventProcessed(testEvent1, true)
        mockProcessingListener.verifyTriggerEventProcessed(testEvent2, false)
        mockProcessingListener.verifyTriggerEventProcessed(testEvent3, true)
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
            imageEvents = listOf(
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
        // Mock detection results for each condition.
        mockScreenDetector.apply {
            mockDetectionResult(testConditionEvt1, true)
            mockDetectionResult(testConditionEvt2, false)
        }
        // Keep track of event fulfillment results
        val eventsFulfilled = mockProcessingListener.monitorImageEventProcessing(testScenario.imageEvents)

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
        assertFalse(eventsFulfilled[0])
        assertTrue(eventsFulfilled[1])
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
        val testEvent1 = testsData.newTestTriggerEvent(
            eventId = eventId1,
            scenarioId = scenarioId,
            enabledOnStart = true,
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId1, "A", EQUALS, 0)),
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
            conditions = listOf(testsData.newTestCounterTriggerCondition(eventId2, "A", EQUALS, 0)),
            actions = listOf(testsData.newCounterAction(eventId2, "A", OperationType.ADD, 10)),
        )
        val testScenario = testsData.newTestScenario(
            scenarioId = scenarioId,
            triggerEvents = listOf(testEvent1, testEvent2)
        )

        // When: Only verify on one frame here
        scenarioProcessor = createScenarioProcessor(testScenario).apply {
            process(testsData.newMockedScreenBitmap())
        }

        // Then: Should not crash (ConcurrentModification), event1 is fulfilled, event2 is disabled by event1
        mockProcessingListener.verifyTriggerEventProcessed(testEvent1, true)
        mockProcessingListener.verifyTriggerEventNotProcessed(testEvent2)
    }
}
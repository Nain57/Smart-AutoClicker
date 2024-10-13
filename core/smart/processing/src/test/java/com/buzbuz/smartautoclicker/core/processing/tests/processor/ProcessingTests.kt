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
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
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
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

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
    @Mock private lateinit var mockImageDetector: ImageDetector
    @Mock private lateinit var mockAndroidExecutor: AndroidExecutor
    @Mock private lateinit var mockEndListener: StopRequestListener

    /** The object under test. */
    private lateinit var scenarioProcessor: ScenarioProcessor

    private fun createScenarioProcessor(testScenario: TestScenario) =
        ScenarioProcessor(
            processingTag = "tests",
            detectionQuality = testScenario.scenario.detectionQuality,
            randomize = testScenario.scenario.randomize,
            imageEvents = testScenario.imageEvents,
            triggerEvents = testScenario.triggerEvents,
            imageDetector = mockImageDetector,
            androidExecutor = mockAndroidExecutor,
            bitmapSupplier = mockBitmapSupplier::getBitmap,
            onStopRequested = mockEndListener::onStopRequested,
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
                                TestEventToggle(eventId1, Action.ToggleEvent.ToggleType.DISABLE),
                                TestEventToggle(eventId2, Action.ToggleEvent.ToggleType.ENABLE)
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
                                TestEventToggle(eventId1, Action.ToggleEvent.ToggleType.ENABLE),
                                TestEventToggle(eventId2, Action.ToggleEvent.ToggleType.DISABLE)
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
            triggerEvents = emptyList(),
        )

        // Mock the bitmaps for each image conditions
        mockBitmapSupplier.apply {
            mockBitmapProviding(testConditionEvt1)
            mockBitmapProviding(testConditionEvt2)
            mockBitmapProviding(testConditionEvt3)
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
}
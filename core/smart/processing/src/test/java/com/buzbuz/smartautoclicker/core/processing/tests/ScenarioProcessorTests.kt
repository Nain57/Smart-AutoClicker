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
package com.buzbuz.smartautoclicker.core.processing.tests

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.detection.DetectionResult
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release.Hold
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.ScenarioProcessor
import com.buzbuz.smartautoclicker.core.processing.data.processor.actions.GestureHold
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScreenConditionScalingInfo
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.domain.HoldReleaseReason
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.processing.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newFinger
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.newGesture
import com.buzbuz.smartautoclicker.core.processing.tests.processor.gesture.FingerResolverTestData.press
import com.buzbuz.smartautoclicker.core.processing.utils.ProcessingData.newCondition
import com.buzbuz.smartautoclicker.core.processing.utils.ProcessingData.newEvent
import com.buzbuz.smartautoclicker.core.processing.utils.anyNotNull

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as mockWhen

/** Test the [ScenarioProcessor] class. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ShadowBitmapCreator::class])
class ScenarioProcessorTests {

    private companion object {
        private const val TEST_DATA_SCREEN_IMAGE_WIDTH = 800
        private const val TEST_DATA_SCREEN_IMAGE_HEIGHT = 600

        private const val TEST_CONDITION_PATH_1 = "TOTO1"
        private val TEST_CONDITION_AREA_1 = Rect(0 , 1, 2, 3)
        private const val TEST_CONDITION_THRESHOLD_1 = 1
        private const val TEST_CONDITION_PATH_2 = "TOTO2"
        private val TEST_CONDITION_AREA_2 = Rect(4 , 5, 6, 7)
        private const val TEST_CONDITION_THRESHOLD_2 = 2
        private const val TEST_CONDITION_PATH_3 = "TOTO3"
        private val TEST_CONDITION_AREA_3 = Rect(8 , 9, 10, 11)
        private const val TEST_CONDITION_THRESHOLD_3 = 3

        private val TEST_DETECTION_OK = DetectionResult(true)
        private val TEST_DETECTION_KO = DetectionResult(false)

        private const val TEST_COOLDOWN_MS = 60_000L

        private fun newDefaultClickAction(duration: Long = 1) =
            Click(
                id = Identifier(databaseId = 1),
                eventId = Identifier(databaseId = 1),
                priority = 0,
                pressDuration = duration,
                position = Position.Coordinates(
                    x = CoordinatesValue.Literal(10),
                    y = CoordinatesValue.Literal(10),
                ),
            )
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        suspend fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }
    /** Interface to be mocked in order to verify the calls to the stop listener. */
    interface StopRequestListener {
        fun onStopRequested()
    }

    @Mock private lateinit var mockScalingManager: ScalingManager
    @Mock private lateinit var mockImageDetector: ImageDetector
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockAndroidExecutor: AndroidActionExecutor
    @Mock private lateinit var mockEndListener: StopRequestListener
    @Mock private lateinit var mockProgressListener: SmartProcessingListener

    @Mock private lateinit var mockScreenBitmap: Bitmap

    /** The object under test. */
    private lateinit var scenarioProcessor: ScenarioProcessor

    /** Creates and initialize mocks for a new condition. */
    private fun createTestCondition(
        path: String,
        area: Rect,
        threshold: Int,
        @DetectionType detectionType: Int,
        shouldBeOnScreen: Boolean,
        isDetected: Boolean,
    ) : ScreenCondition.Image = runBlocking {
        val condition = newCondition(path, area, threshold, detectionType, shouldBeOnScreen)
        val conditionBitmap = mock(Bitmap::class.java)

        mockWhen(mockBitmapSupplier.getBitmap(condition.path, area.width(), area.height())).thenReturn(conditionBitmap)

        val pass = if (isDetected) TEST_DETECTION_OK else TEST_DETECTION_KO
        mockWhen(mockImageDetector.detectImage(eq(conditionBitmap), anyInt(),anyInt(), anyNotNull(), anyInt())).thenReturn(pass)
        mockWhen(mockScalingManager.getScreenConditionScalingInfo(condition))
            .thenReturn(ScreenConditionScalingInfo.Image(condition, area, area))

        condition
    }

    /** */
    private suspend fun assertActionGesture(expectedDuration: Long) {
        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())

        // A click is a single finger pressing and lifting, so it is built as one chain holding one gesture.
        val chains = gestureCaptor.lastValue.chains
        Assert.assertEquals("Click should be built as a single chain", 1, chains.size)
        Assert.assertEquals("Click chain should contains only one gesture", 1, chains.first().size)
        val gesture = chains.first().first()

        Assert.assertEquals("Gesture should contains only one stroke", 1, gesture.strokeCount)
        gesture.getStroke(0).let { stroke ->
            Assert.assertEquals("Gesture duration is invalid", expectedDuration, stroke.duration)
            Assert.assertEquals("Gesture start time is invalid", 0, stroke.startTime)
        }
    }

    /** @return a new SceanarioProcessor with all necessary mocks. */
    private fun createNewScenarioProcessor(
        events: List<ScreenEvent>,
        triggerEvent: List<TriggerEvent>,
        counters: List<Counter> = emptyList(),
    ) : ScenarioProcessor {
        val processor = ScenarioProcessor(
            processingTag = "",
            imageDetector = mockImageDetector,
            scalingManager = mockScalingManager,
            randomize = false,
            screenEvents = events,
            triggerEvents = triggerEvent,
            counters = counters,
            bitmapSupplier = mockBitmapSupplier::getBitmap,
            androidExecutor = mockAndroidExecutor,
            currentTimeMs = { currentTimeMs() },
            onStopRequested = mockEndListener::onStopRequested,
            progressListener = mockProgressListener,
        )

        Mockito.clearInvocations(mockAndroidExecutor)
        return processor
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())

        // Mock screen bitmap creation from screen image
        mockWhen(mockScreenBitmap.width).thenReturn(TEST_DATA_SCREEN_IMAGE_WIDTH)
        mockWhen(mockScreenBitmap.height).thenReturn(TEST_DATA_SCREEN_IMAGE_HEIGHT)
        mockWhen(mockScreenBitmap.config).thenReturn(Bitmap.Config.ARGB_8888)

        mockWhen(mockScalingManager.scaleUpDetectionResult(anyNotNull()))
            .doAnswer { invocation -> invocation.getArgument(0) }

        // Emulates the pointer held on the screen, like the real executor does
        heldPointer = null
        areGesturesCancelled = false
        isPointerLiftedByUser = false
        currentTimeMs = { 0L }
        mockWhen(mockAndroidExecutor.getHeldPointer()).doAnswer { heldPointer }
        runBlocking {
            mockWhen(mockAndroidExecutor.dispatchBuiltGesture(anyNotNull())).doAnswer { invocation ->
                heldPointer =
                    if (areGesturesCancelled) null
                    else invocation.getArgument<BuiltGesture>(0).heldPointer
            }
            mockWhen(mockAndroidExecutor.releaseHeldPointer()).doAnswer { heldPointer = null }
            mockWhen(mockAndroidExecutor.checkHeldPointer()).doAnswer {
                if (isPointerLiftedByUser) heldPointer = null
                heldPointer != null
            }
        }
    }

    /** The pointer the mocked android executor is holding, if any. */
    private var heldPointer: HeldPointer? = null
    /** True to have the gestures dispatched by the mocked android executor cancelled by the system. */
    private var areGesturesCancelled: Boolean = false
    /** True to have the held pointer found lifted by the user when the mocked android executor checks it. */
    private var isPointerLiftedByUser: Boolean = false
    /** The clock used by the processor to time the holds. */
    private var currentTimeMs: () -> Long = { 0L }

    /** The event owning the pointer the mocked android executor is holding, if any. */
    private fun heldEvent(): Event? =
        (heldPointer?.owner as? GestureHold)?.event

    private fun newHoldGestureAction(hold: Hold = Hold.UntilUnfulfilled): Gesture =
        newGesture(newFinger(0L, press(10, 10), GestureFingerMotion.Release(Identifier(databaseId = 3L), hold)))

    /** Verify no action was executed. The held pointer can still be read, it is the held event source of truth. */
    private fun assertNoActionExecuted() {
        verify(mockAndroidExecutor, atLeast(0)).getHeldPointer()
        verifyNoMoreInteractions(mockAndroidExecutor)
    }

    private fun createHeldTestCondition(isDetected: Boolean): ScreenCondition.Image =
        createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = isDetected,
            shouldBeOnScreen = true,
        )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noEvent() = runTest{
        scenarioProcessor = createNewScenarioProcessor(emptyList(), emptyList())
        scenarioProcessor.process(mockScreenBitmap)


        verify(mockEndListener).onStopRequested()
        assertNoActionExecuted()
        verifyNoInteractions(mockBitmapSupplier, mockImageDetector)
    }

    @Test
    fun noConditions_withActions() = runTest {
        val event = newEvent(
            conditions = emptyList(),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockBitmapSupplier, mockEndListener)
    }

    @Test
    fun oneCondition_exact_noMatch_shouldBeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event = newEvent(
            operator = AND,

            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_exact_noMatch_should_Not_BeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_exact_match_shouldBeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_exact_match_should_Not_BeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = false,
        )
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_wholeScreen_noMatch_shouldBeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true
        )
        val event = newEvent(
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_wholeScreen_noMatch_should_Not_BeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false
        )
        val expectedDuration = 1L
        val event = newEvent(
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_wholeScreen_match_shouldBeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun oneCondition_wholeScreen_match_should_Not_BeDetected() = runTest {
        val condition = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = false,
        )
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_allNoMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )

        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_allNoMatch_oneShould_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_allNoMatch_allShould_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_oneNoMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_oneNoMatch_should_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_allMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)


        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_AND_allMatch_oneShouldNotBeOnScreen() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)


        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_allNoMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )

        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_allNoMatch_oneShould_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_allNoMatch_allShould_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = false,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_oneNoMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_oneNoMatch_should_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_allMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(expectedDuration)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalConditions_OR_allMatch_allShould_Not_BeDetected() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = false,
        )
        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = false,
        )
        val condition3 = createTestCondition(
            TEST_CONDITION_PATH_3,
            TEST_CONDITION_AREA_3,
            TEST_CONDITION_THRESHOLD_3,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = false,
        )

        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalEvents_noneMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(newDefaultClickAction()),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event2 = newEvent(
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalEvents_firstMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val actionDuration1 = 1L
        val event1 = newEvent(
            id = 10L,
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(newDefaultClickAction(actionDuration1)),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val actionDuration2 = 3L
        val event2 = newEvent(
            id = 11L,
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(newDefaultClickAction(actionDuration2)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(actionDuration1)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalEvents_secondMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val actionDuration1 = 1L
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(newDefaultClickAction(actionDuration1)),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val actionDuration2 = 3L
        val event2 = newEvent(
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(newDefaultClickAction(actionDuration2)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(actionDuration2)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun severalEvents_allMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val actionDuration1 = 1L
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(newDefaultClickAction(actionDuration1)),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val actionDuration2 = 1L
        val event2 = newEvent(
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(newDefaultClickAction(actionDuration2)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertActionGesture(actionDuration1)
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun stopAfter_one_noMatch() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(newDefaultClickAction()),
            enableOnStart = true,
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockImageDetector).setScreenBitmap(mockScreenBitmap, "")
        assertNoActionExecuted()
        verifyNoInteractions(mockEndListener)
    }

    @Test
    fun heldEvent_conditionsFulfilled_isNotExecutedAgain() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        Assert.assertEquals("The event should hold the pointer", event, heldEvent())
    }

    @Test
    fun heldEvent_conditionsUnfulfilled_isReleased() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).releaseHeldPointer()
        Assert.assertNull("The pointer should have been released", heldPointer)
    }

    @Test
    fun heldEvent_pointerDroppedBySystem_isExecutedAgain() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        heldPointer = null
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(anyNotNull())
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
    }

    @Test
    fun heldEvent_isCheckedOnEachIteration() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        // Nothing is held during the first iteration, the pointer is checked on the two following ones
        verify(mockAndroidExecutor, times(2)).checkHeldPointer()
        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        Assert.assertEquals("The event should still hold the pointer", event, heldEvent())
    }

    @Test
    fun heldEvent_pointerLiftedByUser_isUnderInterruptionCooldown() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        isPointerLiftedByUser = true
        scenarioProcessor.process(mockScreenBitmap)

        // The pointer is already up, there is nothing to release. The event has no cooldown of its own, but holding
        // again right away could be ignored by the system: it is not executed again.
        verify(mockAndroidExecutor, times(1)).checkHeldPointer()
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.Interrupted)
        Assert.assertTrue(scenarioProcessor.processingState.isCooldownRunning(event))
        Assert.assertNull("Nothing should be held", heldPointer)
    }

    @Test
    fun heldTriggerEvent_pointerLiftedByUser_isUnderInterruptionCooldown() = runTest {
        val counterName = "counter"
        val triggerEvent = TriggerEvent(
            id = Identifier(databaseId = 10L),
            scenarioId = Identifier(databaseId = 1L),
            name = "Trigger",
            conditionOperator = AND,
            actions = listOf(newHoldGestureAction()),
            conditions = listOf(
                TriggerCondition.OnCounterCountReached(
                    id = Identifier(databaseId = 11L),
                    eventId = Identifier(databaseId = 10L),
                    name = "Counter reached",
                    counterName = counterName,
                    comparisonOperation = ComparisonOperation.EQUALS,
                    counterValue = CounterOperationValue.Number(1.0),
                )
            ),
        )

        scenarioProcessor = createNewScenarioProcessor(
            events = emptyList(),
            triggerEvent = listOf(triggerEvent),
            counters = listOf(Counter(counterName, 1.0, Identifier(databaseId = 1L))),
        )
        scenarioProcessor.process(mockScreenBitmap)

        isPointerLiftedByUser = true
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        Assert.assertTrue(scenarioProcessor.processingState.isCooldownRunning(triggerEvent))
        Assert.assertNull("Nothing should be held", heldPointer)
    }

    @Test
    fun heldEvent_pointerLiftedByUserDuringDetection_isUnderInterruptionCooldown() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newDefaultClickAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        // The frame check passes, then the user touches the screen before event2 is executed
        var checkCount = 0
        mockWhen(mockAndroidExecutor.checkHeldPointer()).doAnswer {
            if (++checkCount > 1) heldPointer = null
            heldPointer != null
        }
        scenarioProcessor.process(mockScreenBitmap)

        val gestureCaptor = argumentCaptor<BuiltGesture>()
        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(gestureCaptor.capture())
        Assert.assertNull("Event2 click should not continue the lifted pointer", gestureCaptor.lastValue.heldPointer)
        verify(mockProgressListener).onHeldEventReleased(event1, HoldReleaseReason.Interrupted)
        Assert.assertTrue(scenarioProcessor.processingState.isCooldownRunning(event1))
    }

    @Test
    fun heldEvent_pointerLiftedByUser_startsCooldown() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        ).copy(cooldownMs = TEST_COOLDOWN_MS)

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        isPointerLiftedByUser = true
        scenarioProcessor.process(mockScreenBitmap)

        // The event is still detected, but under cooldown since the user lifted its finger
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        Assert.assertNull("Nothing should be held", heldPointer)
    }

    @Test
    fun heldEvent_disabled_isReleased() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newDefaultClickAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        Assert.assertEquals("Event1 should hold the pointer", event1, heldEvent())

        scenarioProcessor.processingState.disableEvent(event1.id.databaseId)
        scenarioProcessor.process(mockScreenBitmap)

        // Only checked right before the release, the disabled event is not verified as a still held one
        verify(mockAndroidExecutor, times(1)).checkHeldPointer()
        verify(mockAndroidExecutor, times(1)).releaseHeldPointer()
    }

    @Test
    fun heldEvent_pointerLiftedByUserBeforeRelease_isNotReleased() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        // The frame check passes, then the user touches the screen while the held event is verified
        var checkCount = 0
        mockWhen(mockAndroidExecutor.checkHeldPointer()).doAnswer {
            if (++checkCount > 1) heldPointer = null
            heldPointer != null
        }
        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.Interrupted)
        Assert.assertTrue(scenarioProcessor.processingState.isCooldownRunning(event))
        Assert.assertNull("Nothing should be held", heldPointer)
    }

    @Test
    fun holdingGesture_cancelled_eventIsExecutedAgain() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        areGesturesCancelled = true
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(anyNotNull())
    }

    @Test
    fun heldEvent_takenOverByAnotherEvent_isExecutedAgain() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())

        // Event1 holds, then event2 is the only one detected and takes the hold over
        scenarioProcessor.process(mockScreenBitmap)
        Assert.assertEquals("Event1 should hold the pointer", event1, heldEvent())
        scenarioProcessor.process(mockScreenBitmap)
        Assert.assertEquals("Event2 should have taken the hold over", event2, heldEvent())

        // Event1 is no longer held, it is detected again
        scenarioProcessor.process(mockScreenBitmap)
        Assert.assertEquals("Event1 should have taken the hold back", event1, heldEvent())
        verify(mockAndroidExecutor, times(3)).dispatchBuiltGesture(anyNotNull())
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
    }

    @Test
    fun heldEvent_carriedByAnotherEvent_staysHeld() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newDefaultClickAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        Assert.assertEquals("Event1 should still hold the pointer", event1, heldEvent())
        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(anyNotNull())
    }

    @Test
    fun heldTriggerEvent_releasedWhenConditionsUnfulfilled() = runTest {
        val counterName = "counter"
        val triggerEvent = TriggerEvent(
            id = Identifier(databaseId = 10L),
            scenarioId = Identifier(databaseId = 1L),
            name = "Trigger",
            conditionOperator = AND,
            actions = listOf(newHoldGestureAction()),
            conditions = listOf(
                TriggerCondition.OnCounterCountReached(
                    id = Identifier(databaseId = 11L),
                    eventId = Identifier(databaseId = 10L),
                    name = "Counter reached",
                    counterName = counterName,
                    comparisonOperation = ComparisonOperation.EQUALS,
                    counterValue = CounterOperationValue.Number(1.0),
                )
            ),
        )

        scenarioProcessor = createNewScenarioProcessor(
            events = emptyList(),
            triggerEvent = listOf(triggerEvent),
            counters = listOf(Counter(counterName, 1.0, Identifier(databaseId = 1L))),
        )
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        Assert.assertEquals("The trigger event should hold the pointer", triggerEvent, heldEvent())

        scenarioProcessor.processingState.setCounterValue(counterName, 0.0)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).releaseHeldPointer()
        Assert.assertNull("The pointer should have been released", heldPointer)
    }

    @Test
    fun heldEvent_released_startsCooldown() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        ).copy(cooldownMs = TEST_COOLDOWN_MS)

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        // Conditions are no longer fulfilled, the finger is released
        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        // Conditions are fulfilled again, but the event is under cooldown
        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_OK)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
        verify(mockAndroidExecutor, times(1)).releaseHeldPointer()
    }

    @Test
    fun heldEvent_takenOver_startsCooldown() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
            .copy(cooldownMs = TEST_COOLDOWN_MS)
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)
        // Event1 lost its hold to event2 and is under cooldown, event2 is held: nothing is executed
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(anyNotNull())
        Assert.assertEquals("Event2 should hold the pointer", event2, heldEvent())
    }

    @Test
    fun heldEvent_holdStarted_notifiesHeldEventChangedOnce() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockProgressListener, times(1)).onHeldEventChanged(event)
        verify(mockProgressListener, never()).onHeldEventChanged(null)
    }

    @Test
    fun heldEvent_conditionsFulfilled_notifiesHeldCheckOnly() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        // Only the first detection, before the hold, is notified as an event processing
        verify(mockProgressListener, times(1)).onEventProcessingCompleted(eq(event), anyBoolean(), anyNotNull(), eq(false))
        verify(mockProgressListener, times(2)).onEventProcessingCompleted(eq(event), eq(true), anyNotNull(), eq(true))
        verify(mockProgressListener, never()).onEventProcessingCompleted(eq(event), eq(false), anyNotNull(), eq(true))
    }

    @Test
    fun heldEvent_conditionsUnfulfilled_notifiesReleasedCheckBeforeRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(event)
            verify(mockProgressListener).onEventProcessingCompleted(eq(event), eq(false), anyNotNull(), eq(true))
            verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.Unfulfilled)
            verify(mockProgressListener).onHeldEventChanged(null)
        }
    }

    @Test
    fun heldTriggerEvent_conditionsUnfulfilled_notifiesReleasedCheck() = runTest {
        val counterName = "counter"
        val triggerEvent = TriggerEvent(
            id = Identifier(databaseId = 10L),
            scenarioId = Identifier(databaseId = 1L),
            name = "Trigger",
            conditionOperator = AND,
            actions = listOf(newHoldGestureAction()),
            conditions = listOf(
                TriggerCondition.OnCounterCountReached(
                    id = Identifier(databaseId = 11L),
                    eventId = Identifier(databaseId = 10L),
                    name = "Counter reached",
                    counterName = counterName,
                    comparisonOperation = ComparisonOperation.EQUALS,
                    counterValue = CounterOperationValue.Number(1.0),
                )
            ),
        )

        scenarioProcessor = createNewScenarioProcessor(
            events = emptyList(),
            triggerEvent = listOf(triggerEvent),
            counters = listOf(Counter(counterName, 1.0, Identifier(databaseId = 1L))),
        )
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.processingState.setCounterValue(counterName, 0.0)
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(triggerEvent)
            verify(mockProgressListener).onEventProcessingCompleted(eq(triggerEvent), eq(true), anyNotNull(), eq(true))
            verify(mockProgressListener).onEventProcessingCompleted(eq(triggerEvent), eq(false), anyNotNull(), eq(true))
            verify(mockProgressListener).onHeldEventReleased(triggerEvent, HoldReleaseReason.Unfulfilled)
            verify(mockProgressListener).onHeldEventChanged(null)
        }
    }

    @Test
    fun heldEvent_pointerLiftedByUser_notifiesRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        ).copy(cooldownMs = TEST_COOLDOWN_MS)

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        isPointerLiftedByUser = true
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.Interrupted)
            verify(mockProgressListener).onHeldEventChanged(null)
        }
        verify(mockProgressListener, never()).onEventProcessingCompleted(anyNotNull(), anyBoolean(), anyNotNull(), eq(true))
    }

    @Test
    fun heldEvent_disabled_notifiesReleaseWithoutCheck() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newDefaultClickAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.processingState.disableEvent(event1.id.databaseId)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockProgressListener).onHeldEventReleased(event1, HoldReleaseReason.EventDisabled)
        verify(mockProgressListener).onHeldEventChanged(null)
        verify(mockProgressListener, never()).onEventProcessingCompleted(anyNotNull(), anyBoolean(), anyNotNull(), eq(true))
    }

    @Test
    fun heldEvent_pointerDroppedBySystem_notifiesRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        ).copy(cooldownMs = TEST_COOLDOWN_MS)

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        heldPointer = null
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.Interrupted)
        verify(mockProgressListener).onHeldEventChanged(null)
    }

    @Test
    fun heldEvent_pointerLiftedByUser_heldAgain_notifiesNewHold() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        isPointerLiftedByUser = true
        scenarioProcessor.process(mockScreenBitmap)

        // The interruption cooldown has elapsed, the event holds again
        scenarioProcessor.processingState.removeCooldown(event)
        isPointerLiftedByUser = false
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(event)
            verify(mockProgressListener).onHeldEventChanged(null)
            verify(mockProgressListener).onHeldEventChanged(event)
        }
    }

    @Test
    fun heldEvent_pointerDroppedBySystem_heldAgain_notifiesNewHold() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        heldPointer = null
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(event)
            verify(mockProgressListener).onHeldEventChanged(null)
            verify(mockProgressListener).onHeldEventChanged(event)
        }
    }

    @Test
    fun heldEvent_takenOver_notifiesNewHeldEvent() = runTest {
        val condition = createHeldTestCondition(isDetected = true)
        val event1 = newEvent(id = 1L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))
        val event2 = newEvent(id = 2L, conditions = listOf(condition), actions = listOf(newHoldGestureAction()))

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(event1)
            verify(mockProgressListener).onHeldEventReleased(event1, HoldReleaseReason.OtherEvent)
            verify(mockProgressListener).onHeldEventChanged(event2)
        }
        verify(mockProgressListener, never()).onHeldEventChanged(null)
    }

    @Test
    fun heldEvent_scenarioEnd_notifiesRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.onScenarioEnd()

        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.ScenarioStopped)
        verify(mockProgressListener).onHeldEventChanged(null)
    }

    @Test
    fun heldEvent_screenRotation_notifiesRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.onScreenRotation()

        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.ScreenRotation)
        verify(mockProgressListener).onHeldEventChanged(null)
    }

    @Test
    fun heldEvent_holdStarted_notifiesNoRelease() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockProgressListener, never()).onHeldEventReleased(anyNotNull(), anyNotNull())
    }

    @Test
    fun notHoldingEvent_startsCooldown() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newDefaultClickAction()),
        ).copy(cooldownMs = TEST_COOLDOWN_MS)

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).dispatchBuiltGesture(anyNotNull())
    }

    @Test
    fun heldUntilActionsEnd_isReleasedOnceActionsAreExecuted() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction(Hold.UntilActionsEnd), newDefaultClickAction()),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        inOrder(mockAndroidExecutor) {
            verify(mockAndroidExecutor, times(2)).dispatchBuiltGesture(anyNotNull())
            verify(mockAndroidExecutor).releaseHeldPointer()
        }
        Assert.assertNull("The pointer should have been released", heldPointer)
        inOrder(mockProgressListener) {
            verify(mockProgressListener).onHeldEventChanged(event)
            verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.ActionsCompleted)
            verify(mockProgressListener).onHeldEventChanged(null)
        }
    }

    @Test
    fun heldForDuration_notElapsed_isKeptEvenIfUnfulfilled() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction(Hold.ForDuration(500L))),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        currentTimeMs = { 499L }
        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, never()).releaseHeldPointer()
        Assert.assertEquals("The event should still hold the pointer", event, heldEvent())
    }

    @Test
    fun heldForDuration_elapsed_isReleasedOnNextFrame() = runTest {
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(newHoldGestureAction(Hold.ForDuration(500L))),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        currentTimeMs = { 500L }
        mockWhen(mockImageDetector.detectImage(anyNotNull(), anyInt(), anyInt(), anyNotNull(), anyInt()))
            .thenReturn(TEST_DETECTION_KO)
        scenarioProcessor.process(mockScreenBitmap)

        verify(mockAndroidExecutor, times(1)).releaseHeldPointer()
        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.DurationElapsed)
        Assert.assertNull("The pointer should have been released", heldPointer)
    }

    @Test
    fun heldForDuration_elapsedDuringPause_isReleasedBeforeNextAction() = runTest {
        currentTimeMs = { testScheduler.currentTime }
        val event = newEvent(
            conditions = listOf(createHeldTestCondition(isDetected = true)),
            actions = listOf(
                newHoldGestureAction(Hold.ForDuration(100L)),
                Pause(
                    id = Identifier(databaseId = 4L),
                    eventId = Identifier(databaseId = 1L),
                    priority = 1,
                    pauseDuration = 1_000L,
                ),
                newDefaultClickAction(),
            ),
        )

        var releaseTimeMs = -1L
        mockWhen(mockAndroidExecutor.releaseHeldPointer()).doAnswer {
            releaseTimeMs = testScheduler.currentTime
            heldPointer = null
        }

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList())
        scenarioProcessor.process(mockScreenBitmap)

        Assert.assertEquals("The pointer should be released during the pause", 100L, releaseTimeMs)
        val gestureCaptor = argumentCaptor<BuiltGesture>()
        inOrder(mockAndroidExecutor) {
            verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
            verify(mockAndroidExecutor).releaseHeldPointer()
            verify(mockAndroidExecutor).dispatchBuiltGesture(gestureCaptor.capture())
        }
        Assert.assertNull("The click should not carry the released pointer", gestureCaptor.lastValue.heldPointer)
        verify(mockProgressListener).onHeldEventReleased(event, HoldReleaseReason.DurationElapsed)
    }
}

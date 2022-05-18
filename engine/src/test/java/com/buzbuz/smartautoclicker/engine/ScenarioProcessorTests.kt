/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.domain.*

import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.detection.ImageDetector
import com.buzbuz.smartautoclicker.engine.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.engine.utils.ProcessingData.newCondition
import com.buzbuz.smartautoclicker.engine.utils.ProcessingData.newEvent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor

import org.robolectric.annotation.Config

import java.nio.ByteBuffer

/** Test the [ScenarioProcessor] class. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ShadowBitmapCreator::class])
class ScenarioProcessorTests {

    private companion object {
        private const val TEST_DATA_DETECTION_QUALITY = 600.0

        private const val TEST_DATA_SCREEN_IMAGE_WIDTH = 800
        private const val TEST_DATA_SCREEN_IMAGE_HEIGHT = 600
        private const val TEST_DATA_SCREEN_IMAGE_PIXEL_STRIDE = 1
        private const val TEST_DATA_SCREEN_IMAGE_ROW_STRIDE = TEST_DATA_SCREEN_IMAGE_WIDTH

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
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }
    /** Interface to be mocked in order to verify the calls to the end condition listener. */
    interface EndConditionListener {
        fun onEndConditionReached()
    }

    @Mock private lateinit var mockBitmapCreator: ShadowBitmapCreator.BitmapCreator

    @Mock private lateinit var mockImageDetector: ImageDetector
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockAndroidExecutor: AndroidExecutor
    @Mock private lateinit var mockEndListener: EndConditionListener

    @Mock private lateinit var mockScreenImage: Image
    @Mock private lateinit var mockScreenImagePlane: Image.Plane
    @Mock private lateinit var mockScreenImagePlaneBuffer: ByteBuffer
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
    ) : Condition {
        val conditionBitmap = mock(Bitmap::class.java)
        mockWhen(mockBitmapSupplier.getBitmap(path, area.width(), area.height())).thenReturn(conditionBitmap)

        val pass = if (isDetected) TEST_DETECTION_OK else TEST_DETECTION_KO
        when (detectionType) {
            EXACT -> mockWhen(mockImageDetector.detectCondition(conditionBitmap, area, threshold)).thenReturn(pass)
            WHOLE_SCREEN -> mockWhen(mockImageDetector.detectCondition(conditionBitmap, threshold)).thenReturn(pass)
        }
        return newCondition(path, area, threshold, detectionType, shouldBeOnScreen)
    }

    /** */
    private fun assertActionGesture(expectedDuration: Long) {
        val gestureCaptor = argumentCaptor<GestureDescription>()
        verify(mockAndroidExecutor).executeGesture(gestureCaptor.capture())
        val gesture = gestureCaptor.lastValue

        Assert.assertEquals("Gesture should contains only one stroke", 1, gesture.strokeCount)
        gesture.getStroke(0).let { stroke ->
            Assert.assertEquals("Gesture duration is invalid", expectedDuration, stroke.duration)
            Assert.assertEquals("Gesture start time is invalid", 0, stroke.startTime)
        }
    }

    /** @return a new SceanarioProcessor with all necessary mocks. */
    private fun createNewScenarioProcessor(
        events: List<Event>,
        endConditions: List<EndCondition>,
        endConditionOperator: Int
    ) = ScenarioProcessor(
        mockImageDetector,
        TEST_DATA_DETECTION_QUALITY.toInt(),
        events,
        mockBitmapSupplier::getBitmap,
        mockAndroidExecutor,
        endConditionOperator,
        endConditions,
        mockEndListener::onEndConditionReached,
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())
        ShadowBitmapCreator.setMockInstance(mockBitmapCreator)

        // Mock screen bitmap creation from screen image
        mockWhen(mockScreenImage.width).thenReturn(TEST_DATA_SCREEN_IMAGE_WIDTH)
        mockWhen(mockScreenImage.height).thenReturn(TEST_DATA_SCREEN_IMAGE_HEIGHT)
        mockWhen(mockScreenImage.planes).thenReturn(arrayOf(mockScreenImagePlane))
        mockWhen(mockScreenImagePlane.pixelStride).thenReturn(TEST_DATA_SCREEN_IMAGE_PIXEL_STRIDE)
        mockWhen(mockScreenImagePlane.rowStride).thenReturn(TEST_DATA_SCREEN_IMAGE_ROW_STRIDE)
        mockWhen(mockScreenImagePlane.buffer).thenReturn(mockScreenImagePlaneBuffer)
        mockWhen(mockBitmapCreator.createBitmap(
            TEST_DATA_SCREEN_IMAGE_WIDTH,
            TEST_DATA_SCREEN_IMAGE_HEIGHT,
            Bitmap.Config.ARGB_8888
        )).thenReturn(mockScreenBitmap)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noEvent() = runTest{
        scenarioProcessor = createNewScenarioProcessor(emptyList(), emptyList(), OR)
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockBitmapSupplier, mockAndroidExecutor, mockEndListener)
    }

    @Test
    fun noConditions_withActions() = runTest {
        val event = newEvent(
            conditions = emptyList(),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockBitmapSupplier, mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)


        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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

        val expectedDuration = 1L
        val event = newEvent(
            operator = AND,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)


        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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

        val expectedDuration = 1L
        val event = newEvent(
            operator = OR,
            conditions = listOf(condition1, condition2, condition3),
            actions = listOf(Action.Click(eventId = 1, pressDuration = expectedDuration, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
        val actionDuration1 = 1L
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val actionDuration2 = 1L
        val event2 = newEvent(
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration2, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
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
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
        )

        val condition2 = createTestCondition(
            TEST_CONDITION_PATH_2,
            TEST_CONDITION_AREA_2,
            TEST_CONDITION_THRESHOLD_2,
            WHOLE_SCREEN,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val actionDuration2 = 1L
        val event2 = newEvent(
            operator = AND,
            conditions = listOf(condition2),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration2, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration2, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration2, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1, event2), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
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
        val actionDuration1 = 1L
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
            stopAfter = 1,
        )

        scenarioProcessor = createNewScenarioProcessor(listOf(event1), emptyList(),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        verifyNoInteractions(mockAndroidExecutor, mockEndListener)
    }

    @Test
    fun endConditionReached_or_one() = runTest {
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
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 1
                )
            ),
            OR
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        assertActionGesture(actionDuration1)
        verify(mockEndListener).onEndConditionReached()
    }

    @Test
    fun endConditionReached_or_three() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 3
                )
            ),
            OR
        )

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, never()).onEndConditionReached()

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, never()).onEndConditionReached()

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, times(1)).onEndConditionReached()
    }

    @Test
    fun endConditionReached_or_twoEndCondition_oneOk() = runTest {
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
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            id = 42,
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )
        val event2 = newEvent(
            id = 85,
            operator = OR,
            conditions = listOf(condition2),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1, event2),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 1
                ),
                EndCondition(
                    scenarioId = event2.scenarioId,
                    eventId = event2.id,
                    eventName = event2.name,
                    executions = 3
                )
            ),
            OR,
        )

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, times(1)).onEndConditionReached()
    }

    @Test
    fun endConditionReached_and_one() = runTest {
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
            operator = AND,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = actionDuration1, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 1
                ),
            ),
            AND,
        )
        scenarioProcessor.process(mockScreenImage)

        verify(mockImageDetector).setupDetection(mockScreenBitmap, TEST_DATA_DETECTION_QUALITY)
        assertActionGesture(actionDuration1)
        verify(mockEndListener).onEndConditionReached()
    }

    @Test
    fun endConditionReached_and_three() = runTest {
        val condition1 = createTestCondition(
            TEST_CONDITION_PATH_1,
            TEST_CONDITION_AREA_1,
            TEST_CONDITION_THRESHOLD_1,
            EXACT,
            isDetected = true,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            operator = AND,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 3
                ),
            ),
            AND,
        )
        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, never()).onEndConditionReached()

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, never()).onEndConditionReached()

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, times(1)).onEndConditionReached()
    }

    @Test
    fun endConditionReached_and_twoEndCondition_oneOk() = runTest {
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
            EXACT,
            isDetected = false,
            shouldBeOnScreen = true,
        )
        val event1 = newEvent(
            id = 42,
            operator = OR,
            conditions = listOf(condition1),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )
        val event2 = newEvent(
            id = 85,
            operator = OR,
            conditions = listOf(condition2),
            actions = listOf(Action.Click(eventId = 1, pressDuration = 1L, x = 10, y = 10, clickOnCondition = false)),
        )

        scenarioProcessor = createNewScenarioProcessor(
            listOf(event1, event2),
            listOf(
                EndCondition(
                    scenarioId = event1.scenarioId,
                    eventId = event1.id,
                    eventName = event1.name,
                    executions = 1
                ),
                EndCondition(
                    scenarioId = event2.scenarioId,
                    eventId = event2.id,
                    eventName = event2.name,
                    executions = 3
                )
            ),
            AND,
        )

        scenarioProcessor.process(mockScreenImage)
        verify(mockEndListener, never()).onEndConditionReached()
    }
}
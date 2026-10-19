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
package com.buzbuz.smartautoclicker.core.processing.tests

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.detection.ImageDetector
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.DetectorState
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScreenConditionScalingInfo
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.mockk.verifyOrder

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the bitmap cache sizing performed by the [DetectorEngine] when starting and stopping a detection. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DetectorEngineBitmapCacheTests {

    private companion object {
        private val TEST_DISPLAY_SIZE = Point(1080, 1920)
        private val TEST_SCALED_DISPLAY_SIZE = Point(540, 960)
        private const val BYTES_PER_PIXEL = 4L

        private val TEST_SCENARIO = Scenario(
            id = Identifier(databaseId = 1L),
            name = "Test Scenario",
            detectionQuality = 960,
        )
    }

    @RelaxedMockK private lateinit var mockDisplayConfigManager: DisplayConfigManager
    @RelaxedMockK private lateinit var mockBitmapRepository: BitmapRepository
    @RelaxedMockK private lateinit var mockScalingManager: ScalingManager
    @RelaxedMockK private lateinit var mockDisplayRecorder: DisplayRecorder
    @RelaxedMockK private lateinit var mockActionExecutor: AndroidActionExecutor
    @RelaxedMockK private lateinit var mockSettingsRepository: SettingsRepository
    @RelaxedMockK private lateinit var mockAppComponentsProvider: AppComponentsProvider
    @RelaxedMockK private lateinit var mockDebuggingListener: SmartProcessingListener
    @RelaxedMockK private lateinit var mockOcrModelsRepository: OCRModelsRepository
    @RelaxedMockK private lateinit var mockImageDetector: ImageDetector
    @RelaxedMockK private lateinit var mockContext: Context
    @RelaxedMockK private lateinit var mockIntent: Intent

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockDisplayConfigManager.displayConfig } returns DisplayConfig(
            sizePx = TEST_DISPLAY_SIZE,
            orientation = Configuration.ORIENTATION_PORTRAIT,
            safeInsetTopPx = 0,
            roundedCorners = emptyMap(),
        )
        every { mockScalingManager.startScaling(any(), any()) } returns TEST_SCALED_DISPLAY_SIZE
        every { mockScalingManager.getScreenConditionScalingInfo(any()) } returns null
        coEvery { mockDisplayRecorder.validateScreenCapture() } returns true
        every { mockSettingsRepository.isInputBlockWorkaroundEnabled() } returns false
        every { mockAppComponentsProvider.originalAppId } returns "test.app"
        coEvery { mockDisplayRecorder.acquireLatestBitmap() } returns null
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cache is sized for the screen capture only when there is no image conditions`() = runTest {
        val engine = startDetection(screenEvents = emptyList())

        verify(exactly = 1) {
            mockBitmapRepository.resizeCacheForDetection(2 * TEST_SCALED_DISPLAY_SIZE.toBytes())
        }

        stopDetection(engine)
    }

    @Test
    fun `cache is sized for all image conditions at their scaled size`() = runTest {
        val condition1 = newImageCondition(id = 1, path = "a.png").withScaledArea(width = 50, height = 40)
        val condition2 = newImageCondition(id = 2, path = "b.png").withScaledArea(width = 100, height = 20)
        val engine = startDetection(
            screenEvents = listOf(newEvent(id = 1, condition1), newEvent(id = 2, condition2)),
        )

        verify(exactly = 1) {
            mockBitmapRepository.resizeCacheForDetection(
                (50 * 40 + 100 * 20) * BYTES_PER_PIXEL + 2 * TEST_SCALED_DISPLAY_SIZE.toBytes()
            )
        }

        stopDetection(engine)
    }

    @Test
    fun `image conditions sharing the same bitmap are counted once`() = runTest {
        val condition1 = newImageCondition(id = 1, path = "a.png").withScaledArea(width = 50, height = 40)
        val condition2 = newImageCondition(id = 2, path = "a.png").withScaledArea(width = 50, height = 40)
        val engine = startDetection(
            screenEvents = listOf(newEvent(id = 1, condition1, condition2)),
        )

        verify(exactly = 1) {
            mockBitmapRepository.resizeCacheForDetection(
                50 * 40 * BYTES_PER_PIXEL + 2 * TEST_SCALED_DISPLAY_SIZE.toBytes()
            )
        }

        stopDetection(engine)
    }

    @Test
    fun `image conditions without scaling info are ignored`() = runTest {
        val condition = newImageCondition(id = 1, path = "a.png")
        val engine = startDetection(screenEvents = listOf(newEvent(id = 1, condition)))

        verify(exactly = 1) {
            mockBitmapRepository.resizeCacheForDetection(2 * TEST_SCALED_DISPLAY_SIZE.toBytes())
        }

        stopDetection(engine)
    }

    @Test
    fun `cache is cleared and reset to default size when detection stops`() = runTest {
        val engine = startDetection(screenEvents = emptyList())

        stopDetection(engine)

        verifyOrder {
            mockBitmapRepository.clearCache()
            mockBitmapRepository.resizeCacheForDetection(any())
            mockBitmapRepository.clearCache()
            mockBitmapRepository.resetCacheSize()
        }
    }

    // ---- helpers ----

    private fun newImageCondition(id: Long, path: String) = ScreenCondition.Image(
        id = Identifier(databaseId = id),
        eventId = Identifier(databaseId = 1L),
        name = "Condition $id",
        threshold = 10,
        shouldBeDetected = true,
        priority = 0,
        path = path,
        area = Rect(0, 0, 100, 80),
        detectionType = EXACT,
    )

    private fun ScreenCondition.Image.withScaledArea(width: Int, height: Int): ScreenCondition.Image {
        every { mockScalingManager.getScreenConditionScalingInfo(this@withScaledArea) } returns
                ScreenConditionScalingInfo.Image(
                    screenCondition = this,
                    detectionArea = Rect(0, 0, TEST_SCALED_DISPLAY_SIZE.x, TEST_SCALED_DISPLAY_SIZE.y),
                    imageArea = Rect(0, 0, width, height),
                )
        return this
    }

    private fun newEvent(id: Long, vararg conditions: ScreenCondition.Image) = ScreenEvent(
        id = Identifier(databaseId = id),
        scenarioId = TEST_SCENARIO.id,
        name = "Event $id",
        conditionOperator = AND,
        priority = 0,
        conditions = conditions.toList(),
        actions = emptyList(),
        enabledOnStart = true,
        keepDetecting = false,
        cooldownMs = 0L,
    )

    private fun Point.toBytes(): Long = x * y * BYTES_PER_PIXEL

    private fun TestScope.startDetection(screenEvents: List<ScreenEvent>): DetectorEngine {
        val engine = DetectorEngine(
            ioDispatcher = StandardTestDispatcher(testScheduler),
            displayConfigManager = mockDisplayConfigManager,
            bitmapRepository = mockBitmapRepository,
            scalingManager = mockScalingManager,
            displayRecorder = mockDisplayRecorder,
            actionExecutor = mockActionExecutor,
            settingsRepository = mockSettingsRepository,
            appComponentsProvider = mockAppComponentsProvider,
            debuggingListener = mockDebuggingListener,
            ocrModelsRepository = mockOcrModelsRepository,
        )

        engine.startScreenRecord(0, mockIntent, null)
        advanceTimeBy(1)

        engine.startDetection(
            context = mockContext,
            scenario = TEST_SCENARIO,
            screenEvents = screenEvents,
            triggerEvents = emptyList(),
            counters = emptyList(),
            liveDebugging = false,
            generateReport = false,
            imageDetectorFactory = { mockImageDetector },
        )
        advanceTimeBy(1)

        check(engine.state.value == DetectorState.DETECTING) {
            "Expected DETECTING after startDetection, got ${engine.state.value}"
        }
        return engine
    }

    /** Stops the infinite processing loop so runTest cleanup can terminate. */
    private fun TestScope.stopDetection(engine: DetectorEngine) {
        engine.stopDetection()
        advanceTimeBy(1)
    }
}

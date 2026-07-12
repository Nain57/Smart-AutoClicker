/*
 * Copyright (C) 2025 Kevin Buzeau
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
import android.graphics.Bitmap
import android.content.res.Configuration
import android.graphics.Point
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
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.DetectorState
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify

import kotlinx.coroutines.channels.Channel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests verifying that orientation changes during DETECTING state allow the current frame to
 * complete before the display is resized and detection is restarted.
 *
 * The fix uses an orientationChangeRequested flag and join() instead of cancelAndJoin(), so
 * in-progress action sequences are never interrupted mid-execution.
 *
 * MockK is used for all mocks — it handles suspend functions natively without runBlocking,
 * which is critical because acquireLatestBitmap() is called on every loop iteration.
 * The imageDetectorFactory parameter is used to inject a plain ImageDetector mock, so
 * NativeDetector (a final class with native methods) is never subclassed or loaded.
 *
 * advanceUntilIdle() is intentionally avoided — the detection loop is infinite and would hang.
 * All time advancement uses advanceTimeBy() with explicit margins instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DetectorEngineDetectionOrientationTests {

    private companion object {
        private val TEST_DISPLAY_SIZE = Point(1080, 1920)
        // Must match the private constants in DetectorEngine.
        private const val ORIENTATION_DEBOUNCE_MS = 100L
        private const val NO_IMAGE_DELAY_MS = 20L
        // Headroom: debounce fires at 100 ms, then the loop's current null-delay (≤ 20 ms)
        // must also expire before the loop checks the flag and exits.
        private const val ADVANCE_MS = ORIENTATION_DEBOUNCE_MS + NO_IMAGE_DELAY_MS + 20L

        private val TEST_SCENARIO = Scenario(
            id = Identifier(databaseId = 1L),
            name = "Test Scenario",
            detectionQuality = 600,
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
        every { mockScalingManager.startScaling(any(), any()) } returns TEST_DISPLAY_SIZE
        every { mockScalingManager.refreshScaling() } returns TEST_DISPLAY_SIZE
        coEvery { mockDisplayRecorder.validateScreenCapture() } returns true
        every { mockSettingsRepository.isInputBlockWorkaroundEnabled() } returns false
        every { mockAppComponentsProvider.originalAppId } returns "test.app"
        // Return null so the processing loop stays in the lightweight null-delay branch.
        // MockK handles suspend functions natively — no runBlocking wrapper per call,
        // so this is safe to call on every iteration of the infinite detection loop.
        coEvery { mockDisplayRecorder.acquireLatestBitmap() } returns null
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    /**
     * Regression test for the original bug: actions were interrupted on rotation.
     *
     * Simulates a frame that is mid-execution when the orientation change fires: the loop is
     * suspended inside acquireLatestBitmap() waiting for a frame from the channel. The orientation
     * handler sets the flag and calls join() — but it must wait for the frame to finish.
     *
     * Verify that resizeDisplay is NOT called while the frame is in progress, and IS called only
     * after the frame is released and the loop exits cleanly via the flag.
     */
    @Test
    fun `in-progress frame completes before display is resized on orientation change`() = runTest {
        // Replace the null stub with a channel so we control exactly when each frame is delivered.
        val frameChannel = Channel<Bitmap?>(capacity = Channel.UNLIMITED)
        coEvery { mockDisplayRecorder.acquireLatestBitmap() } coAnswers { frameChannel.receive() }

        val (engine, orientationListener) = startDetectionAndCaptureOrientationListener()
        // Loop is now blocked in frameChannel.receive() — simulating a frame in progress.

        orientationListener(mockContext)
        // Advance past the debounce: the orientation handler sets the flag and enters join(),
        // but the processing loop is still suspended in frameChannel.receive() — join() waits.
        advanceTimeBy(ADVANCE_MS)

        // Display must NOT have been resized yet — the frame is still "in progress".
        verify(exactly = 0) { mockScalingManager.refreshScaling() }

        // Release the blocked frame. The loop resumes, returns null from acquireLatestBitmap(),
        // waits through the null-image delay, then checks the flag and exits cleanly.
        frameChannel.send(null)
        advanceTimeBy(NO_IMAGE_DELAY_MS + 5)

        // Frame is done — resize must have happened now.
        verify(exactly = 1) { mockScalingManager.refreshScaling() }

        // The restarted loop is blocked in frameChannel.receive() again. Stop to let runTest clean up.
        stopDetection(engine)
    }

    /**
     * Verify that an orientation change during DETECTING calls refreshScaling and
     * onEventsProcessingCancelled, then resumes detection (state returns to DETECTING).
     */
    @Test
    fun `orientation change during detecting resumes detection after resize`() = runTest {
        val (engine, orientationListener) = startDetectionAndCaptureOrientationListener()

        orientationListener(mockContext)
        advanceTimeBy(ADVANCE_MS)

        verify(exactly = 1) { mockScalingManager.refreshScaling() }
        verify(exactly = 1) { mockDebuggingListener.onEventsProcessingCancelled() }
        assertEquals(DetectorState.DETECTING, engine.state.value)

        stopDetection(engine)
    }

    /**
     * Rapid orientation changes during DETECTING must be debounced to a single
     * refreshScaling call, matching the behaviour already verified for RECORDING state.
     */
    @Test
    fun `rapid orientation changes during detecting are debounced to a single resize`() = runTest {
        val (engine, orientationListener) = startDetectionAndCaptureOrientationListener()

        orientationListener(mockContext)
        orientationListener(mockContext)
        orientationListener(mockContext)
        advanceTimeBy(ADVANCE_MS)

        verify(exactly = 1) { mockScalingManager.refreshScaling() }

        stopDetection(engine)
    }

    /**
     * After the first orientation change is fully handled the orientationChangeRequested flag
     * must be reset, so a subsequent rotation also triggers a resize.
     */
    @Test
    fun `orientation change flag resets allowing a second change to also trigger resize`() = runTest {
        val (engine, orientationListener) = startDetectionAndCaptureOrientationListener()

        orientationListener(mockContext)
        advanceTimeBy(ADVANCE_MS)

        orientationListener(mockContext)
        advanceTimeBy(ADVANCE_MS)

        verify(exactly = 2) { mockScalingManager.refreshScaling() }

        stopDetection(engine)
    }

    // ---- helpers ----

    private fun TestScope.startDetectionAndCaptureOrientationListener(): Pair<DetectorEngine, (Context) -> Unit> {
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

        var capturedListener: ((Context) -> Unit)? = null
        every { mockDisplayConfigManager.addOrientationListener(any()) } answers {
            capturedListener = firstArg()
        }

        engine.startScreenRecord(0, mockIntent, null)
        // 1 ms: enough to run the startScreenRecord coroutine (no internal delays),
        // reaching state = RECORDING without advancing any detection-loop delays.
        advanceTimeBy(1)

        engine.startDetection(
            context = mockContext,
            scenario = TEST_SCENARIO,
            screenEvents = emptyList(),
            triggerEvents = emptyList(),
            counters = emptyList(),
            liveDebugging = false,
            generateReport = false,
            // Inject a plain ImageDetector mock so NativeDetector is never subclassed or loaded.
            imageDetectorFactory = { mockImageDetector },
        )
        // 1 ms: runs startDetection setup (no internal delays), enters processScreenImages,
        // emits DETECTING, then suspends on the first null-image delay.
        advanceTimeBy(1)

        check(engine.state.value == DetectorState.DETECTING) {
            "Expected DETECTING after startDetection, got ${engine.state.value}"
        }

        return engine to checkNotNull(capturedListener) { "Orientation listener was not registered" }
    }

    /**
     * Cancels the active detection so the infinite processing loop is gone before runTest's
     * implicit advanceUntilIdle() cleanup runs. Without this, the loop keeps scheduling more
     * null-image delays and advanceUntilIdle() never terminates (causing OOM or a hang).
     */
    private fun TestScope.stopDetection(engine: DetectorEngine) {
        engine.stopDetection()
        // A single tick is enough: stopDetection launches a coroutine that immediately
        // calls cancelAndJoin() on the processing job (no internal delays), after which
        // the loop is gone and advanceUntilIdle() will terminate cleanly.
        advanceTimeBy(1)
    }
}

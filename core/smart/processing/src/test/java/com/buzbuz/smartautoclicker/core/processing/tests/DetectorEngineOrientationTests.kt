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
import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.OCRModelsRepository
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.common.actions.AndroidActionExecutor
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder
import com.buzbuz.smartautoclicker.core.processing.data.DetectorEngine
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingListener
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.robolectric.annotation.Config

import org.mockito.Mockito.`when` as mockWhen

/**
 * Regression tests for the ConcurrentModificationException caused by rapid orientation changes
 * launching multiple coroutines on processingScope that concurrently accessed ScalingManager.
 *
 * Fixed by: limitedParallelism(1) on processingScope + debouncing in onScreenOrientationChanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DetectorEngineOrientationTests {

    private companion object {
        private val TEST_DISPLAY_SIZE = Point(1080, 1920)
        private const val ORIENTATION_DEBOUNCE_MS = 100L
    }

    @Mock private lateinit var mockDisplayConfigManager: DisplayConfigManager
    @Mock private lateinit var mockBitmapRepository: BitmapRepository
    @Mock private lateinit var mockScalingManager: ScalingManager
    @Mock private lateinit var mockDisplayRecorder: DisplayRecorder
    @Mock private lateinit var mockActionExecutor: AndroidActionExecutor
    @Mock private lateinit var mockSettingsRepository: SettingsRepository
    @Mock private lateinit var mockAppComponentsProvider: AppComponentsProvider
    @Mock private lateinit var mockDebuggingListener: SmartProcessingListener
    @Mock private lateinit var mockOcrModelsRepository: OCRModelsRepository

    private val mockContext: Context = mock(Context::class.java)
    private val mockIntent: Intent = mock(Intent::class.java)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockWhen(mockDisplayConfigManager.displayConfig).thenReturn(
            DisplayConfig(
                sizePx = TEST_DISPLAY_SIZE,
                orientation = android.content.res.Configuration.ORIENTATION_PORTRAIT,
                safeInsetTopPx = 0,
                roundedCorners = emptyMap(),
            )
        )
    }

    /**
     * Regression test: before the fix, two concurrent orientation-change coroutines would race on
     * ScalingManager's internal map, causing a ConcurrentModificationException. Verify that N rapid
     * orientation changes result in exactly one refreshScaling/resizeDisplay call.
     */
    @Test
    fun `rapid orientation changes are debounced to a single resize call`() = runTest {
        val orientationListener = startRecordingAndCaptureOrientationListener()

        // Simulate 3 rapid orientation changes (e.g. device tumbling during rotation)
        orientationListener(mockContext)
        orientationListener(mockContext)
        orientationListener(mockContext)

        // Let the debounce settle — advanceUntilIdle advances virtual time past the delay
        advanceUntilIdle()

        // Only one resize should occur despite 3 orientation events
        verify(mockScalingManager, times(1)).refreshScaling()
    }

    /**
     * Verify that a new orientation event within the debounce window resets the timer, so the
     * resize is delayed until the window has been quiet for the full debounce duration.
     */
    @Test
    fun `orientation change within debounce window resets the timer`() = runTest {
        val orientationListener = startRecordingAndCaptureOrientationListener()

        // First orientation event
        orientationListener(mockContext)

        // Advance almost to the debounce deadline (does NOT call advanceUntilIdle — that would
        // advance time further and prematurely trigger the delay)
        advanceTimeBy(ORIENTATION_DEBOUNCE_MS - 1)

        // Second event fires just before the first would settle — resets the debounce timer
        orientationListener(mockContext)

        // The original deadline has now passed, but the second event's window has not
        advanceTimeBy(1)
        verify(mockScalingManager, never()).refreshScaling()

        // Only after the second event's full debounce window expires does the resize happen
        advanceUntilIdle()
        verify(mockScalingManager, times(1)).refreshScaling()
    }

    // ---- helpers ----

    private fun TestScope.startRecordingAndCaptureOrientationListener(): (Context) -> Unit {
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

        // The orientation listener is registered synchronously before the coroutine launch
        val listenerCaptor = argumentCaptor<(Context) -> Unit>()
        verify(mockDisplayConfigManager).addOrientationListener(listenerCaptor.capture())

        advanceUntilIdle() // Complete startProjection + startScreenRecord → state = RECORDING

        return listenerCaptor.firstValue
    }
}

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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.os.Build

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.recorder.DisplayRecorder

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ColorCaptureViewModelTest {

    // Shared scheduler so advanceTimeBy in runTest controls delays in the ViewModel's coroutines.
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    private val mockDisplayConfigManager: DisplayConfigManager = mockk()
    private val mockDisplayRecorder: DisplayRecorder = mockk()
    private val mockMonitoredViewsManager: MonitoredViewsManager = mockk(relaxed = true)

    private lateinit var viewModel: ColorCaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockDisplayConfigManager.displayConfig } returns displayConfig(1080, 1920)
        viewModel = ColorCaptureViewModel(
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            displayConfigManager = mockDisplayConfigManager,
            displayRecorder = mockDisplayRecorder,
            monitoredViewsManager = mockMonitoredViewsManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Initial state

    @Test
    fun initialState_captureStep_isScreenshotSelection() {
        assertEquals(ColorCaptureMenuStep.SCREENSHOT_SELECTION, viewModel.uiState.value.captureStep)
    }

    @Test
    fun initialState_menuIsVisible() {
        assertEquals(true, viewModel.uiState.value.menuVisibility)
    }

    @Test
    fun initialState_topButtonIsEnabled() {
        assertEquals(true, viewModel.uiState.value.topButtonEnabled)
    }

    @Test
    fun initialState_showHideButtonIsDisabled() {
        assertEquals(false, viewModel.uiState.value.showHideButtonEnabled)
    }

    @Test
    fun initialState_pixelSelectionUiState_isNull() {
        assertNull(viewModel.uiState.value.pixelSelectionUiState)
    }

    // endregion

    // region captureScreen

    @Test
    fun captureScreen_immediatelyTransitionsToCapturingStep() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()

        assertEquals(ColorCaptureMenuStep.CAPTURING, viewModel.uiState.value.captureStep)
    }

    @Test
    fun captureScreen_hidesMenuDuringCapture() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()

        assertEquals(false, viewModel.uiState.value.menuVisibility)
    }

    @Test
    fun captureScreen_withSuccessfulScreenshot_transitionsToPixelSelectionStep() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        assertEquals(ColorCaptureMenuStep.PIXEL_SELECTION, viewModel.uiState.value.captureStep)
    }

    @Test
    fun captureScreen_withSuccessfulScreenshot_showsMenu() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        assertEquals(true, viewModel.uiState.value.menuVisibility)
    }

    @Test
    fun captureScreen_withSuccessfulScreenshot_enablesShowHideButton() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        assertEquals(true, viewModel.uiState.value.showHideButtonEnabled)
    }

    @Test
    fun captureScreen_withSuccessfulScreenshot_populatesPixelSelectionUiState() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        assertNotNull(viewModel.uiState.value.pixelSelectionUiState)
    }

    @Test
    fun captureScreen_withNullScreenshot_staysInCapturingStep() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns null

        viewModel.captureScreen()
        advanceTimeBy(201)

        assertEquals(ColorCaptureMenuStep.CAPTURING, viewModel.uiState.value.captureStep)
    }


    @Test
    fun captureScreen_withNullInitialPosition_usesCenterOfDisplay() = runTest(testDispatcher) {
        val displaySize = Point(1080, 1920)
        every { mockDisplayConfigManager.displayConfig } returns displayConfig(displaySize.x, displaySize.y)
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        val expected = PointF(displaySize.x / 2f, displaySize.y / 2f)
        assertEquals(expected, viewModel.uiState.value.pixelSelectionUiState?.selectedPosition)
    }

    @Test
    fun captureScreen_withSuccessfulScreenshot_notifiesMonitoredViewsManager() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()

        viewModel.captureScreen()
        advanceTimeBy(201)

        verify { mockMonitoredViewsManager.notifyClick(MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE) }
    }

    @Test
    fun captureScreen_withNullScreenshot_doesNotNotifyMonitoredViewsManager() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns null

        viewModel.captureScreen()
        advanceTimeBy(201)

        verify(exactly = 0) { mockMonitoredViewsManager.notifyClick(any()) }
    }

    // endregion

    // region cancelCapture

    @Test
    fun cancelCapture_resetsToScreenshotSelectionStep() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns null
        viewModel.captureScreen() // Go to CAPTURING

        viewModel.cancelCapture()

        assertEquals(ColorCaptureMenuStep.SCREENSHOT_SELECTION, viewModel.uiState.value.captureStep)
    }

    @Test
    fun cancelCapture_clearsPixelSelectionUiState() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()
        viewModel.captureScreen()
        advanceTimeBy(201) // Reach PIXEL_SELECTION

        viewModel.cancelCapture()

        assertNull(viewModel.uiState.value.pixelSelectionUiState)
    }

    // endregion

    // region getPixelSelection

    @Test
    fun getPixelSelection_returnsNull_inInitialState() {
        assertNull(viewModel.getPixelSelection())
    }

    @Test
    fun getPixelSelection_returnsNull_inCapturingState() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns null
        viewModel.captureScreen()

        assertNull(viewModel.getPixelSelection())
    }

    // endregion

    // region updateSelectedPosition

    @Test
    fun updateSelectedPosition_updatesPositionInPixelSelectionUiState() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()
        viewModel.captureScreen()
        advanceTimeBy(201)

        val newPosition = PointF(300f, 400f)
        viewModel.updateSelectedPosition(newPosition)

        assertEquals(newPosition, viewModel.uiState.value.pixelSelectionUiState?.selectedPosition)
    }

    @Test
    fun updateSelectedPosition_withNonNullPosition_enablesTopButton() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()
        viewModel.captureScreen()
        advanceTimeBy(201)

        viewModel.updateSelectedPosition(PointF(100f, 100f))

        assertEquals(true, viewModel.uiState.value.topButtonEnabled)
    }

    @Test
    fun updateSelectedPosition_withNull_disablesTopButton() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()
        viewModel.captureScreen()
        advanceTimeBy(201)

        viewModel.updateSelectedPosition(null)

        assertEquals(false, viewModel.uiState.value.topButtonEnabled)
    }

    @Test
    fun updateSelectedPosition_withNull_setsSelectedPositionToNull() = runTest(testDispatcher) {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockBitmap()
        viewModel.captureScreen()
        advanceTimeBy(201)

        viewModel.updateSelectedPosition(null)

        assertNull(viewModel.uiState.value.pixelSelectionUiState?.selectedPosition)
    }

    @Test
    fun updateSelectedPosition_notInPixelSelectionState_doesNotChangeState() {
        val stateBefore = viewModel.uiState.value

        viewModel.updateSelectedPosition(PointF(100f, 200f))

        assertEquals(stateBefore, viewModel.uiState.value)
    }

    // endregion

    // region helpers

    private fun mockBitmap(pixelColor: Int = Color.RED): Bitmap = mockk {
        every { width } returns 1080
        every { height } returns 1920
        every { getPixel(any(), any()) } returns pixelColor
    }

    private fun displayConfig(width: Int, height: Int): DisplayConfig = mockk {
        every { sizePx } returns Point(width, height)
    }

    // endregion
}

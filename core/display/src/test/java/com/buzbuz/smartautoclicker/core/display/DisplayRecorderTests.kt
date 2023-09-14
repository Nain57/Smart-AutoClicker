/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.display

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Surface

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.display.shadows.ShadowImageReader
import com.buzbuz.smartautoclicker.core.display.utils.anyNotNull

import kotlinx.coroutines.runBlocking

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

/** Test the [DisplayRecorder] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ ShadowImageReader::class ])
class DisplayRecorderTests {

    private companion object {
        private const val TEST_DATA_RESULT_CODE = 42
        private val TEST_DATA_PROJECTION_DATA_INTENT = Intent()

        private const val TEST_DATA_DENSITY_DPI = 180
        private val TEST_DATA_CONFIGURATION = Configuration().apply { densityDpi = TEST_DATA_DENSITY_DPI }

        private const val TEST_DATA_DISPLAY_SIZE_WIDTH = 800
        private const val TEST_DATA_DISPLAY_SIZE_HEIGHT = 600
        private val TEST_DATA_DISPLAY_SIZE = Point(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT)
    }

    /** Interface to be mocked in order to verify the calls to the stop listener. */
    interface StoppedListener {
        fun onStopped()
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockMediaProjectionManager: MediaProjectionManager
    @Mock private lateinit var mockMediaProjection: MediaProjection
    @Mock private lateinit var mockImageReader: ImageReader
    @Mock private lateinit var mockScreenImage: Image
    @Mock private lateinit var mockSurface: Surface
    @Mock private lateinit var mockVirtualDisplay: VirtualDisplay
    @Mock private lateinit var mockStoppedListener: StoppedListener

    /** The object under tests. */
    private lateinit var displayRecorder: DisplayRecorder

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Setup context mocks and display metrics
        mockWhen(mockContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).thenReturn(mockMediaProjectionManager)
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockResources.configuration).thenReturn(TEST_DATA_CONFIGURATION)

        // Setup Image reader
        ShadowImageReader.setMockInstance(mockImageReader)
        mockWhen(mockImageReader.surface).thenReturn(mockSurface)
        mockWhen(mockImageReader.acquireLatestImage()).thenReturn(mockScreenImage)

        // Setup projection mocks
        mockWhen(mockMediaProjectionManager.getMediaProjection(TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT))
            .thenReturn(mockMediaProjection)
        mockWhen(mockMediaProjection.createVirtualDisplay(
            DisplayRecorder.VIRTUAL_DISPLAY_NAME,
            TEST_DATA_DISPLAY_SIZE_WIDTH,
            TEST_DATA_DISPLAY_SIZE_HEIGHT,
            TEST_DATA_DENSITY_DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mockSurface,
            null,
            null)
        ).thenReturn(mockVirtualDisplay)

        displayRecorder = DisplayRecorder()
    }

    @After
    fun tearDown() {
        ShadowImageReader.reset()
    }

    @Test
    fun startProjection() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)

        verify(mockMediaProjection).registerCallback(anyNotNull(), anyNotNull())
    }

    @Test
    fun startProjection_alreadyStarted() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)

        verify(mockMediaProjection, times(1)).registerCallback(anyNotNull(), anyNotNull())
    }

    @Test
    fun startScreenRecord() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)
        displayRecorder.startScreenRecord(mockContext, TEST_DATA_DISPLAY_SIZE)

        assertEquals(1, ShadowImageReader.getInstanceCreationCount())
    }

    @Test
    fun startScreenRecord_alreadyStarted() = runBlocking  {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)
        displayRecorder.startScreenRecord(mockContext, TEST_DATA_DISPLAY_SIZE)
        displayRecorder.startScreenRecord(mockContext, TEST_DATA_DISPLAY_SIZE)

        assertEquals(1, ShadowImageReader.getInstanceCreationCount())
    }

    @Test
    fun stopProjection() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)
        displayRecorder.startScreenRecord(mockContext, TEST_DATA_DISPLAY_SIZE)

        displayRecorder.stopProjection()

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).close()
            verify(mockMediaProjection).unregisterCallback(anyNotNull())
            verify(mockMediaProjection).stop()
        }
        Unit
    }

    @Test
    fun stopProjection_notStarted() = runBlocking {
        displayRecorder.stopProjection()

        verify(mockVirtualDisplay, never()).release()
        verify(mockImageReader, never()).setOnImageAvailableListener(null, null)
        verify(mockImageReader, never()).close()
        verify(mockMediaProjection, never()).unregisterCallback(anyNotNull())
        verify(mockMediaProjection, never()).stop()
    }

    @Test
    fun stopScreenRecord() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)
        displayRecorder.startScreenRecord(mockContext, TEST_DATA_DISPLAY_SIZE)

        displayRecorder.stopScreenRecord()

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).close()
        }
        Unit
    }

    @Test
    fun stopScreenRecord_notStarted() = runBlocking {
        displayRecorder.stopScreenRecord()

        verify(mockVirtualDisplay, never()).release()
        verify(mockImageReader, never()).setOnImageAvailableListener(null, null)
        verify(mockImageReader, never()).close()
    }

    @Test
    fun onStoppedCallback() = runBlocking {
        displayRecorder.startProjection(mockContext, TEST_DATA_RESULT_CODE, TEST_DATA_PROJECTION_DATA_INTENT,
            mockStoppedListener::onStopped)

        val projectionCbCaptor = ArgumentCaptor.forClass(MediaProjection.Callback::class.java)
        verify(mockMediaProjection).registerCallback(projectionCbCaptor.capture(), anyNotNull())
        projectionCbCaptor.value.onStop()

        verify(mockStoppedListener).onStopped()
    }
}
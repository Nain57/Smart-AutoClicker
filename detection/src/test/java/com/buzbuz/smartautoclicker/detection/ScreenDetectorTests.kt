/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.detection

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.detection.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.detection.shadows.ShadowImageReader
import com.buzbuz.smartautoclicker.detection.utils.ProcessingData
import com.buzbuz.smartautoclicker.detection.utils.anyNotNull
import com.buzbuz.smartautoclicker.detection.utils.getOrAwaitValue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations
import org.robolectric.Shadows.shadowOf

import org.robolectric.annotation.Config

import java.nio.ByteBuffer

/** Test the [ScenarioProcessor] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ ShadowImageReader::class, ShadowBitmapCreator::class ])
class ScreenDetectorTests {

    private companion object {
        private const val PROJECTION_RESULT_CODE = 42
        private val PROJECTION_DATA_INTENT = Intent()

        private const val CLICK_CONDITION_PATH = "/this/is/a/path"
        private val CLICK_CONDITION_SIZE = ProcessingData.SCREEN_SIZE
        private val CLICK_CONDITION_AREA = Rect(
            0,
            0,
            CLICK_CONDITION_SIZE,
            CLICK_CONDITION_SIZE
        )
        private val CLICK_CONDITION = ClickCondition(
            CLICK_CONDITION_AREA,
            CLICK_CONDITION_PATH
        )
        private val CLICK_CONDITION_IMAGE_PIXELS =
            IntArray(CLICK_CONDITION_SIZE * CLICK_CONDITION_SIZE)

        private const val IMAGE_PIXEL_STRIDE = 1
        private val IMAGE_ROW_STRIDE = CLICK_CONDITION_SIZE
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }

    /** Interface to be mocked in order to verify the calls to the caputre completion callback. */
    interface CaptureCallback {
        fun onCaptured(capture: Bitmap)
    }

    /** Interface to be mocked in order to verify the calls to the click detected callback. */
    interface DetectionCallback {
        fun onDetected(click: ClickInfo)
    }

    // ScreenRecorder
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockMediaProjectionManager: MediaProjectionManager
    @Mock private lateinit var mockMediaProjection: MediaProjection
    @Mock private lateinit var mockImageReader: ImageReader
    @Mock private lateinit var mockSurface: Surface
    @Mock private lateinit var mockVirtualDisplay: VirtualDisplay

    // Cache
    @Mock private lateinit var mockCreatedBitmap: Bitmap
    @Mock private lateinit var mockSuppliedBitmap: Bitmap
    @Mock private lateinit var mockImage: Image
    @Mock private lateinit var mockImagePlane: Image.Plane
    @Mock private lateinit var mockImagePlaneBuffer: ByteBuffer
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockBitmapCreator: ShadowBitmapCreator.BitmapCreator

    @Mock private lateinit var mockCaptureBitmap: Bitmap
    @Mock private lateinit var mockCaptureCallback: CaptureCallback
    @Mock private lateinit var mockDetectionCallback: DetectionCallback

    /** The object under tests. */
    private lateinit var screenDetector: ScreenDetector

    /**
     * Goes to start screen record state and provides to registered image available listener.
     *
     * @return the image available listener.
     */
    private fun toStartScreenRecord(): ImageReader.OnImageAvailableListener {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)

        val listenerCaptor = ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener::class.java)
        verify(mockImageReader).setOnImageAvailableListener(listenerCaptor.capture(), anyNotNull())

        return listenerCaptor.value
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        ShadowImageReader.setMockInstance(mockImageReader)
        ShadowBitmapCreator.setMockInstance(mockBitmapCreator)

        setUpScreenRecorder()
        setUpCache()

        mockWhen(mockBitmapCreator.createBitmap(eq(mockCreatedBitmap), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(mockCaptureBitmap)

        screenDetector = ScreenDetector(Point(ProcessingData.SCREEN_SIZE, ProcessingData.SCREEN_SIZE),
            mockBitmapSupplier::getBitmap)
    }

    /** Setup the mocks for the screen recorder. */
    private fun setUpScreenRecorder() {
        // Setup context mocks and display metrics
        mockWhen(mockContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).thenReturn(mockMediaProjectionManager)
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockResources.configuration).thenReturn(ProcessingData.SCREEN_CONFIGURATION)

        // Setup Image reader
        mockWhen(mockImageReader.surface).thenReturn(mockSurface)

        // Setup projection mocks
        mockWhen(mockMediaProjectionManager.getMediaProjection(PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT))
            .thenReturn(mockMediaProjection)
        mockWhen(mockMediaProjection.createVirtualDisplay(
            ScreenRecorder.VIRTUAL_DISPLAY_NAME,
            ProcessingData.SCREEN_SIZE,
            ProcessingData.SCREEN_SIZE,
            ProcessingData.SCREEN_DENSITY_DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mockSurface,
            null,
            null)
        ).thenReturn(mockVirtualDisplay)
    }

    /** Setup the mocks for the cache. */
    private fun setUpCache() {
        // Mock the bitmaps created by [Bitmap.createBitmap]
        mockWhen(mockBitmapCreator.createBitmap(Mockito.anyInt(), Mockito.anyInt(), anyNotNull()))
            .thenReturn(mockCreatedBitmap)

        // Mock the bitmaps supplied by bitmap supplier lambda
        mockWhen(mockBitmapSupplier.getBitmap(CLICK_CONDITION_PATH, CLICK_CONDITION_SIZE, CLICK_CONDITION_SIZE))
            .thenReturn(mockSuppliedBitmap)
        mockWhen(mockSuppliedBitmap.width).thenReturn(CLICK_CONDITION_SIZE)
        mockWhen(mockSuppliedBitmap.height).thenReturn(CLICK_CONDITION_SIZE)

        // Mock the image data
        mockWhen(mockImageReader.acquireLatestImage()).thenReturn(mockImage)
        mockWhen(mockImage.planes).thenReturn(arrayOf(mockImagePlane))
        mockWhen(mockImagePlane.pixelStride).thenReturn(IMAGE_PIXEL_STRIDE)
        mockWhen(mockImagePlane.rowStride).thenReturn(IMAGE_ROW_STRIDE)
        mockWhen(mockImagePlane.buffer).thenReturn(mockImagePlaneBuffer)
    }

    @Test
    fun startScreenRecord() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_twice() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_threading() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)

        val handlerCaptor = ArgumentCaptor.forClass(Handler::class.java)
        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), handlerCaptor.capture())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())

        assertNotNull("Processing handler should not be null", handlerCaptor.value)
        assertNotEquals("Processing looper should not be the main one",
            Looper.getMainLooper(), handlerCaptor.value.looper)
    }

    @Test
    fun isScreenRecording_initialValue() {
        assertFalse(screenDetector.isScreenRecording.getOrAwaitValue())
    }

    @Test
    fun isScreenRecording_recording() {
        toStartScreenRecord()
        assertTrue(screenDetector.isScreenRecording.getOrAwaitValue())
    }

    @Test
    fun capture_notStarted() {
        screenDetector.captureArea(CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        verifyNoInteractions(mockBitmapCreator, mockCaptureCallback)
    }

    @Test
    fun capture_bitmapCreation() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)

        verify(mockBitmapCreator).createBitmap(mockCreatedBitmap, CLICK_CONDITION_AREA.left, CLICK_CONDITION_AREA.top,
            CLICK_CONDITION_AREA.right, CLICK_CONDITION_AREA.bottom)
    }

    @Test
    fun capture_callback() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap)
    }

    @Test
    fun capture_cleanup() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap) // must be called only once
    }
}
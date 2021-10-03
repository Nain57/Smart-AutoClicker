/*
 * Copyright (C) 2021 Nain57
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

import android.content.BroadcastReceiver
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
import android.view.Display
import android.view.Surface
import android.view.WindowManager

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.detection.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.detection.shadows.ShadowImageReader
import com.buzbuz.smartautoclicker.detection.utils.ProcessingData
import com.buzbuz.smartautoclicker.detection.utils.anyNotNull

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import java.nio.ByteBuffer

/** Test the [ConditionDetector] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ ShadowImageReader::class, ShadowBitmapCreator::class ])
class ScreenDetectorTests {

    private companion object {
        private const val PROJECTION_RESULT_CODE = 42
        private val PROJECTION_DATA_INTENT = Intent()

        private const val VALID_CLICK_CONDITION_PATH = "/this/is/a/path"
        private val VALID_CONDITION_AREA = Rect(
            0,
            0,
            ProcessingData.SCREEN_AREA.width() - 1,
            ProcessingData.SCREEN_AREA.height() - 1
        )

        private const val INVALID_CONDITION_PATH = "/this/is/another/path"
        private val INVALID_CONDITION_AREA = Rect(
            1,
            0,
            ProcessingData.SCREEN_AREA.width(),
            ProcessingData.SCREEN_AREA.height()
        )

        private const val IMAGE_PIXEL_STRIDE = 1
        private val IMAGE_ROW_STRIDE = ProcessingData.SCREEN_SIZE
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }

    /** Interface to be mocked in order to verify the calls to the caputre completion callback. */
    interface CaptureCallback {
        fun onCaptured(capture: Bitmap)
    }

    // ScreenRecorder
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockRepository: Repository
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockWindowManager: WindowManager
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockMediaProjectionManager: MediaProjectionManager
    @Mock private lateinit var mockMediaProjection: MediaProjection
    @Mock private lateinit var mockImageReader: ImageReader
    @Mock private lateinit var mockSurface: Surface
    @Mock private lateinit var mockVirtualDisplay: VirtualDisplay

    // Cache
    @Mock private lateinit var mockScreenImage: Image
    @Mock private lateinit var mockScreenImagePlane: Image.Plane
    @Mock private lateinit var mockScreenImagePlaneBuffer: ByteBuffer
    @Mock private lateinit var mockCaptureCallback: CaptureCallback

    // Bitmaps
    @Mock private lateinit var mockBitmapCreator: ShadowBitmapCreator.BitmapCreator
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockCaptureBitmap: Bitmap
    private lateinit var mockScreenBitmap: Bitmap
    private lateinit var mockValidConditionBitmap: Bitmap
    private lateinit var mockInvalidConditionBitmap: Bitmap

    /** The object under tests. */
    private lateinit var screenDetector: ScreenDetector

    /**
     * Goes to start screen record state and provides to registered image available listener.
     * This will execute all handlers runnable in order to ensure state correctness.
     *
     * @return the image available listener.
     */
    private fun toStartScreenRecord(): ImageReader.OnImageAvailableListener {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        idleAllThread()

        val listenerCaptor = ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener::class.java)
        verify(mockImageReader).setOnImageAvailableListener(listenerCaptor.capture(), anyNotNull())

        return listenerCaptor.value
    }

    /**
     * Goes to stop screen record state.
     * This will execute all handlers runnable in order to ensure state correctness.
     */
    private fun toStopScreenRecord() {
        screenDetector.stop()
        idleAllThread()
    }

    /**
     * Execute a capture with the default test data.
     * This will execute all handlers runnable in order to ensure state correctness.
     */
    private fun executeCaptureArea() {
        screenDetector.captureArea(VALID_CONDITION_AREA, mockCaptureCallback::onCaptured)
        idleAllThread()
    }

    /**
     * Goes to detection state.
     * This will execute all handlers runnable in order to ensure state correctness.
     *
     * @param clicks the list of clicks used as tests data.
     */
    private fun toStartDetection(clicks: List<Event>) {
        screenDetector.startDetection(clicks)
        idleAllThread()
    }

    /**
     * Goes to detection stopped state.
     * This will execute all handlers runnable in order to ensure state correctness.
     */
    private fun toStopDetection() {
        screenDetector.stopDetection()
        idleAllThread()
    }

    /** Execute all pending executions on all loopers. */
    private fun idleAllThread() {
        screenDetector.processingThread?.let { shadowOf(it.looper).idle() }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Create a new mock bitmap containing the provided pixels.
     *
     * @param pixels the pixels provided by [Bitmap.getPixels]
     * @param width the width of the bitmap
     * @param height the height of the bitmap
     *
     * @return the new mock bitmap.
     */
    private fun createMockBitmap(pixels: IntArray, width: Int, height: Int): Bitmap {
        val mockBitmap = mock(Bitmap::class.java)

        // Mock the pixels retrieval from the bitmap
        doAnswer {
            pixels.copyInto(it.arguments[0] as IntArray)
        }.`when`(mockBitmap).getPixels(anyNotNull(), anyInt(), anyInt(), eq(0), eq(0), eq(width), eq(height))

        // Mock the size
        mockWhen(mockBitmap.width).thenReturn(width)
        mockWhen(mockBitmap.height).thenReturn(height)

        return mockBitmap
    }

    /**
     * Mock a bitmap for a click condition.
     *
     * @param path the path on the system for the bitmap
     * @param pixels the pixels provided by [Bitmap.getPixels]
     * @param width the width of the bitmap
     * @param height the height of the bitmap
     *
     * @return the new mock bitmap.
     */
    private fun mockConditionBitmap(path: String, pixels: IntArray, width: Int, height: Int): Bitmap {
        val bitmap = createMockBitmap(pixels, width, height)
        mockWhen(mockBitmapSupplier.getBitmap(path, width, height)).thenReturn(bitmap)
        return bitmap
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        ShadowImageReader.setMockInstance(mockImageReader)
        ShadowBitmapCreator.setMockInstance(mockBitmapCreator)

        setUpScreenRecorder()
        setUpCache()

        // Mock bitmap for the valid click condition
        mockValidConditionBitmap = mockConditionBitmap(
            VALID_CLICK_CONDITION_PATH,
            ProcessingData.getScreenPixelCacheForArea(VALID_CONDITION_AREA).first,
            VALID_CONDITION_AREA.width(),
            VALID_CONDITION_AREA.height()
        )

        // Mock Bitmap for the invalid click condition
        mockInvalidConditionBitmap = mockConditionBitmap(
            INVALID_CONDITION_PATH,
            ProcessingData.getOtherPixelCacheForArea(INVALID_CONDITION_AREA).first,
            INVALID_CONDITION_AREA.width(),
            INVALID_CONDITION_AREA.height()
        )

        // Mock for the capture
        mockWhen(mockBitmapCreator.createBitmap(eq(mockScreenBitmap), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(mockCaptureBitmap)

        screenDetector = ScreenDetector(mockContext, mockRepository)
    }

    /** Setup the mocks for the screen recorder. */
    private fun setUpScreenRecorder() {
        // Setup context mocks and display metrics
        mockWhen(mockContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).thenReturn(mockMediaProjectionManager)
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        mockWhen(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        mockWhen(mockResources.configuration).thenReturn(ProcessingData.SCREEN_CONFIGURATION)

        // Mock get display size
        mockWhen(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
        doAnswer { invocation ->
            val argument = invocation.arguments[0] as Point
            argument.x = ProcessingData.DISPLAY_SIZE.x
            argument.y = ProcessingData.DISPLAY_SIZE.y
            null
        }.`when`(mockDisplay).getRealSize(ArgumentMatchers.any())

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
        // Create a mock for the screen bitmap.
        mockScreenBitmap = createMockBitmap(ProcessingData.SCREEN_PIXELS, ProcessingData.SCREEN_SIZE,
            ProcessingData.SCREEN_SIZE)

        // Mock the image data
        mockWhen(mockImageReader.acquireLatestImage()).thenReturn(mockScreenImage)
        mockWhen(mockScreenImage.planes).thenReturn(arrayOf(mockScreenImagePlane))
        mockWhen(mockScreenImagePlane.pixelStride).thenReturn(IMAGE_PIXEL_STRIDE)
        mockWhen(mockScreenImagePlane.rowStride).thenReturn(IMAGE_ROW_STRIDE)
        mockWhen(mockScreenImagePlane.buffer).thenReturn(mockScreenImagePlaneBuffer)

        // Mock the screen bitmap creation
        mockWhen(mockBitmapCreator.createBitmap(ProcessingData.SCREEN_SIZE, ProcessingData.SCREEN_SIZE, Bitmap.Config.ARGB_8888))
            .thenReturn(mockScreenBitmap)
        doAnswer {
            ProcessingData.SCREEN_PIXELS.copyInto(it.arguments[0] as IntArray)
        }.`when`(mockScreenBitmap).getPixels(anyNotNull(), anyInt(), anyInt(), eq(0), eq(0),
            eq(ProcessingData.SCREEN_SIZE), eq(ProcessingData.SCREEN_SIZE))
    }

    @Test
    fun startScreenRecord() {
        toStartScreenRecord()

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_twice() {
        toStartScreenRecord()
        toStartScreenRecord()

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_threading() {
        toStartScreenRecord()

        val handlerCaptor = ArgumentCaptor.forClass(Handler::class.java)
        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), handlerCaptor.capture())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())

        assertNotNull("Processing handler should not be null", handlerCaptor.value)
        assertNotEquals("Processing looper should not be the main one",
            Looper.getMainLooper(), handlerCaptor.value.looper)
    }

    @Test
    fun configChangedReceiver_registration() {
        toStartScreenRecord()

        verify(mockContext).registerReceiver(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun configChangedReceiver_unregistration() {
        toStartScreenRecord()
        toStopScreenRecord()

        val configReceiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(configReceiverCaptor.capture(), ArgumentMatchers.any())
        verify(mockContext).unregisterReceiver(configReceiverCaptor.value)
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun isScreenRecording_initialValue() = runBlocking {
        assertFalse(screenDetector.isScreenRecording.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun isScreenRecording_recording() = runBlocking {
        toStartScreenRecord()
        assertTrue(screenDetector.isScreenRecording.first())
    }

    @Test
    fun capture_notStarted() {
        executeCaptureArea()

        verifyNoInteractions(mockBitmapCreator, mockCaptureCallback)
    }

    @Test
    fun capture_bitmapCreation() {
        val imageAvailableListener = toStartScreenRecord()

        imageAvailableListener.onImageAvailable(mockImageReader)
        executeCaptureArea()

        verify(mockBitmapCreator).createBitmap(mockScreenBitmap, VALID_CONDITION_AREA.left,
            VALID_CONDITION_AREA.top, VALID_CONDITION_AREA.right, VALID_CONDITION_AREA.bottom)
    }

    @Test
    fun capture_callback() {
        val imageAvailableListener = toStartScreenRecord()

        imageAvailableListener.onImageAvailable(mockImageReader)
        executeCaptureArea()

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap)
    }

    @Test
    fun capture_cleanup() {
        val imageAvailableListener = toStartScreenRecord()

        imageAvailableListener.onImageAvailable(mockImageReader)
        executeCaptureArea()
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap) // must be called only once
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun isDetecting_notDetecting() = runBlocking {
        toStartScreenRecord()
        assertFalse(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun isDetecting_detecting() = runBlocking {
        toStartScreenRecord()
        toStartDetection(emptyList())

        assertTrue(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun stopDetecting_notStarted() = runBlocking {
        toStopDetection()
        assertFalse(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun stopDetecting_started() = runBlocking {
        toStartScreenRecord()
        toStartDetection(emptyList())

        toStopDetection()

        assertFalse(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun stopScreenRecord_notStarted() = runBlocking {
        toStopScreenRecord()

        assertFalse(screenDetector.isScreenRecording.first())
        assertFalse(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun stopScreenRecord_recording() = runBlocking {
        toStartScreenRecord()

        toStopScreenRecord()

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).setOnImageAvailableListener(null, null)
            verify(mockImageReader).close()
            verify(mockMediaProjection).unregisterCallback(anyNotNull())
            verify(mockMediaProjection).stop()
        }
        assertFalse(screenDetector.isScreenRecording.first())
        assertFalse(screenDetector.isDetecting.first())
    }

    @Test
    @Ignore("Migrate detector to coroutines")
    fun stopScreenRecord_detecting() = runBlocking {
        toStartScreenRecord()
        toStartDetection(emptyList())

        toStopScreenRecord()

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).setOnImageAvailableListener(null, null)
            verify(mockImageReader).close()
            verify(mockMediaProjection).unregisterCallback(anyNotNull())
            verify(mockMediaProjection).stop()
        }
        assertFalse(screenDetector.isScreenRecording.first())
        assertFalse(screenDetector.isDetecting.first())
    }
}
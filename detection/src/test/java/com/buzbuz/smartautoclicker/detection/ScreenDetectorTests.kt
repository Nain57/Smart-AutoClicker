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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/** Test the [ScenarioProcessor] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ ShadowImageReader::class, ShadowBitmapCreator::class ])
class ScreenDetectorTests {

    private companion object {
        private const val PROJECTION_RESULT_CODE = 42
        private val PROJECTION_DATA_INTENT = Intent()

        private const val CLICK_NAME = "Click"
        private const val CLICK_DELAY = 5000L

        private const val VALID_CLICK_CONDITION_PATH = "/this/is/a/path"
        private const val VALID_CLICK_CONDITION_THRESHOLD = 12
        private val VALID_CLICK_CONDITION_AREA = Rect(
            0,
            0,
            ProcessingData.SCREEN_AREA.width() - 1,
            ProcessingData.SCREEN_AREA.height() - 1
        )
        private val VALID_CLICK_CONDITION = ClickCondition(
            VALID_CLICK_CONDITION_AREA,
            VALID_CLICK_CONDITION_PATH,
            VALID_CLICK_CONDITION_THRESHOLD
        )

        private const val INVALID_CLICK_CONDITION_PATH = "/this/is/another/path"
        private const val INVALID_CLICK_CONDITION_THRESHOLD = 25
        private val INVALID_CLICK_CONDITION_AREA = Rect(
            1,
            0,
            ProcessingData.SCREEN_AREA.width(),
            ProcessingData.SCREEN_AREA.height()
        )
        private val INVALID_CLICK_CONDITION = ClickCondition(
            INVALID_CLICK_CONDITION_AREA,
            INVALID_CLICK_CONDITION_PATH,
            INVALID_CLICK_CONDITION_THRESHOLD
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

    /** Interface to be mocked in order to verify the calls to the click detected callback. */
    interface DetectionCallback {
        fun onDetected(click: ClickInfo)
    }

    // ScreenRecorder
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockWindowManager: WindowManager
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
    @Mock private lateinit var mockDetectionCallback: DetectionCallback

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
     *
     * @return the image available listener.
     */
    private fun toStartScreenRecord(): ImageReader.OnImageAvailableListener {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()

        val listenerCaptor = ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener::class.java)
        verify(mockImageReader).setOnImageAvailableListener(listenerCaptor.capture(), anyNotNull())

        return listenerCaptor.value
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
            ProcessingData.getScreenPixelCacheForArea(VALID_CLICK_CONDITION_AREA).first,
            VALID_CLICK_CONDITION_AREA.width(),
            VALID_CLICK_CONDITION_AREA.height()
        )

        // Mock Bitmap for the invalid click condition
        mockInvalidConditionBitmap = mockConditionBitmap(
            INVALID_CLICK_CONDITION_PATH,
            ProcessingData.getOtherPixelCacheForArea(INVALID_CLICK_CONDITION_AREA).first,
            INVALID_CLICK_CONDITION_AREA.width(),
            INVALID_CLICK_CONDITION_AREA.height()
        )


        // Mock for the capture
        mockWhen(mockBitmapCreator.createBitmap(eq(mockScreenBitmap), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(mockCaptureBitmap)

        screenDetector = ScreenDetector(mockBitmapSupplier::getBitmap)
    }

    /** Setup the mocks for the screen recorder. */
    private fun setUpScreenRecorder() {
        // Setup context mocks and display metrics
        mockWhen(mockContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).thenReturn(mockMediaProjectionManager)
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        mockWhen(mockResources.configuration).thenReturn(ProcessingData.SCREEN_CONFIGURATION)

        // Mock get display size
        val mockDefaultDisplay = mock(Display::class.java)
        mockWhen(mockWindowManager.defaultDisplay).thenReturn(mockDefaultDisplay)
        doAnswer { invocation ->
            val argument = invocation.arguments[0] as Point
            argument.x = ProcessingData.DISPLAY_SIZE.x
            argument.y = ProcessingData.DISPLAY_SIZE.y
            null
        }.`when`(mockDefaultDisplay).getRealSize(ArgumentMatchers.any())

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
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_twice() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()

        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), anyNotNull())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())
    }

    @Test
    fun startScreenRecord_threading() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()

        val handlerCaptor = ArgumentCaptor.forClass(Handler::class.java)
        verify(mockImageReader).setOnImageAvailableListener(anyNotNull(), handlerCaptor.capture())
        verify(mockMediaProjection).registerCallback(anyNotNull(), isNull())

        assertNotNull("Processing handler should not be null", handlerCaptor.value)
        assertNotEquals("Processing looper should not be the main one",
            Looper.getMainLooper(), handlerCaptor.value.looper)
    }

    @Test
    fun configChangedReceiver_registration() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()

        verify(mockContext).registerReceiver(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun configChangedReceiver_unregistration() {
        screenDetector.startScreenRecord(mockContext, PROJECTION_RESULT_CODE, PROJECTION_DATA_INTENT)
        shadowOf(screenDetector.processingThread!!.looper).idle()
        screenDetector.stop(mockContext)

        val configReceiverCaptor = ArgumentCaptor.forClass(BroadcastReceiver::class.java)
        verify(mockContext).registerReceiver(configReceiverCaptor.capture(), ArgumentMatchers.any())
        verify(mockContext).unregisterReceiver(configReceiverCaptor.value)
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
        screenDetector.captureArea(VALID_CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockBitmapCreator, mockCaptureCallback)
    }

    @Test
    fun capture_bitmapCreation() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(VALID_CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)

        verify(mockBitmapCreator).createBitmap(mockScreenBitmap, VALID_CLICK_CONDITION_AREA.left,
            VALID_CLICK_CONDITION_AREA.top, VALID_CLICK_CONDITION_AREA.right, VALID_CLICK_CONDITION_AREA.bottom)
    }

    @Test
    fun capture_callback() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(VALID_CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap)
    }

    @Test
    fun capture_cleanup() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.captureArea(VALID_CLICK_CONDITION_AREA, mockCaptureCallback::onCaptured)
        imageAvailableListener.onImageAvailable(mockImageReader)
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // callback is posted on main thread handler.
        verify(mockCaptureCallback).onCaptured(mockCaptureBitmap) // must be called only once
    }

    @Test
    fun detection_noScreenRecording() {
        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockDetectionCallback)
    }

    @Test
    fun detection_emptyClickList() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockDetectionCallback)
    }

    @Test
    fun detection_clickWithoutConditions() {
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(
            listOf(ProcessingData.newClickInfo(CLICK_NAME)),
            mockDetectionCallback::onDetected
        )
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockDetectionCallback)
    }

    @Test
    fun detection_clickNotDetected() {
        val click = ProcessingData.newClickInfo(CLICK_NAME, ClickInfo.AND, listOf(INVALID_CLICK_CONDITION))
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(
            listOf(click),
            mockDetectionCallback::onDetected
        )
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockDetectionCallback)
    }

    @Test
    fun detection_clickDetected() {
        val click = ProcessingData.newClickInfo(CLICK_NAME, ClickInfo.AND, listOf(VALID_CLICK_CONDITION))
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(
            listOf(click),
            mockDetectionCallback::onDetected
        )
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verify(mockDetectionCallback).onDetected(click)
    }

    @Test
    fun detection_clickDetected_multipleImages() {
        val click = ProcessingData.newClickInfo(CLICK_NAME, ClickInfo.AND, listOf(VALID_CLICK_CONDITION))
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(
            listOf(click),
            mockDetectionCallback::onDetected
        )

        val imageCount = 5
        for (i in 0 until imageCount) {
            imageAvailableListener.onImageAvailable(mockImageReader)
            shadowOf(Looper.getMainLooper()).runToNextTask() // idle to execute possible callbacks
        }

        shadowOf(Looper.getMainLooper()).idle()
        verify(mockDetectionCallback, times(imageCount)).onDetected(click)
    }

    @Test
    fun detection_clickDetected_delayAfter() {
        val click = ProcessingData.newClickInfo(CLICK_NAME, ClickInfo.AND, listOf(VALID_CLICK_CONDITION), CLICK_DELAY)
        val imageAvailableListener = toStartScreenRecord()

        screenDetector.startDetection(
            listOf(click),
            mockDetectionCallback::onDetected
        )

        imageAvailableListener.onImageAvailable(mockImageReader)
        imageAvailableListener.onImageAvailable(mockImageReader) // This one should be skipped due to delay
        shadowOf(Looper.getMainLooper()).idleFor(CLICK_DELAY + 1, TimeUnit.MILLISECONDS)
        imageAvailableListener.onImageAvailable(mockImageReader)
        shadowOf(Looper.getMainLooper()).idle()

        verify(mockDetectionCallback, times(2)).onDetected(click)
    }

    @Test
    fun detection_alreadyStarted() {
        val click = ProcessingData.newClickInfo(CLICK_NAME, ClickInfo.AND, listOf(INVALID_CLICK_CONDITION))
        val imageAvailableListener = toStartScreenRecord()

        // Start with an empty list then a valid list, then check that the valid item is never detected
        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)
        screenDetector.startDetection(
            listOf(click),
            mockDetectionCallback::onDetected
        )
        imageAvailableListener.onImageAvailable(mockImageReader)

        shadowOf(Looper.getMainLooper()).idle() // idle to execute possible callbacks
        verifyNoInteractions(mockDetectionCallback)
    }

    @Test
    fun isDetecting_notDetecting() {
        toStartScreenRecord()
        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun isDetecting_detecting() {
        toStartScreenRecord()
        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)

        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun stopDetecting_notStarted() {
        screenDetector.stopDetection()
        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun stopDetecting_started() {
        toStartScreenRecord()
        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)

        screenDetector.stopDetection()

        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun stopScreenRecord_notStarted() {
        screenDetector.stop(mockContext)

        assertFalse(screenDetector.isScreenRecording.getOrAwaitValue())
        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun stopScreenRecord_recording() {
        toStartScreenRecord()
        screenDetector.stop(mockContext)

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).setOnImageAvailableListener(null, null)
            verify(mockImageReader).close()
            verify(mockMediaProjection).unregisterCallback(anyNotNull())
            verify(mockMediaProjection).stop()
        }
        assertFalse(screenDetector.isScreenRecording.getOrAwaitValue())
        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }

    @Test
    fun stopScreenRecord_detecting() {
        toStartScreenRecord()
        screenDetector.startDetection(emptyList(), mockDetectionCallback::onDetected)
        shadowOf(screenDetector.processingThread!!.looper).idle()
        screenDetector.stop(mockContext)

        inOrder(mockVirtualDisplay, mockImageReader, mockMediaProjection).apply {
            verify(mockVirtualDisplay).release()
            verify(mockImageReader).setOnImageAvailableListener(null, null)
            verify(mockImageReader).close()
            verify(mockMediaProjection).unregisterCallback(anyNotNull())
            verify(mockMediaProjection).stop()
        }
        assertFalse(screenDetector.isScreenRecording.getOrAwaitValue())
        assertFalse(screenDetector.isDetecting.getOrAwaitValue())
    }
}
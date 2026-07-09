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
package com.buzbuz.smartautoclicker.core.display.recorder

import android.graphics.Bitmap
import android.graphics.Point
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.view.Surface

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.shadows.ShadowImageReader

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

import java.nio.ByteBuffer

/** Test the [ImageReaderProxy] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ShadowImageReader::class])
class ImageReaderProxyTests {

    private companion object {
        private const val WIDTH = 1080
        private const val HEIGHT = 1920
        private val SIZE = Point(WIDTH, HEIGHT)

        private const val WIDTH_2 = 720
        private const val HEIGHT_2 = 1280
        private val SIZE_2 = Point(WIDTH_2, HEIGHT_2)
    }

    @Mock private lateinit var mockBitmapRepository: BitmapRepository
    @Mock private lateinit var mockImageReader: ImageReader
    @Mock private lateinit var mockSurface: Surface
    @Mock private lateinit var mockImage: Image
    @Mock private lateinit var mockBitmap: Bitmap

    private lateinit var imageReaderProxy: ImageReaderProxy

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        ShadowImageReader.setMockInstance(mockImageReader)
        mockWhen(mockImageReader.surface).thenReturn(mockSurface)

        imageReaderProxy = ImageReaderProxy(mockBitmapRepository)
    }

    @After
    fun tearDown() {
        ShadowImageReader.reset()
    }

    // ---- resize ----

    @Test
    fun resize_createsNewImageReader() {
        imageReaderProxy.resize(SIZE)

        assertEquals(1, ShadowImageReader.getInstanceCreationCount())
    }

    @Test
    fun resize_surfaceIsFromNewReader() {
        imageReaderProxy.resize(SIZE)

        assertEquals(mockSurface, imageReaderProxy.surface)
    }

    @Test
    fun resize_closesOldReader() {
        imageReaderProxy.resize(SIZE)
        imageReaderProxy.resize(SIZE_2)

        verify(mockImageReader).close()
    }

    @Test
    fun resize_createsSecondReaderAfterFirst() {
        imageReaderProxy.resize(SIZE)
        imageReaderProxy.resize(SIZE_2)

        assertEquals(2, ShadowImageReader.getInstanceCreationCount())
    }

    // ---- close ----

    @Test
    fun close_closesReader() {
        imageReaderProxy.resize(SIZE)
        imageReaderProxy.close()

        verify(mockImageReader).close()
    }

    @Test
    fun close_withoutResize_doesNotCrash() {
        // Should not throw even though no reader was created
        imageReaderProxy.close()

        verify(mockImageReader, never()).close()
    }

    @Test
    fun close_makesGetLastFrameReturnNull() {
        imageReaderProxy.resize(SIZE)
        imageReaderProxy.close()

        assertNull(imageReaderProxy.getLastFrame())
    }

    // ---- getLastFrame ----

    @Test
    fun getLastFrame_withoutResize_returnsNull() {
        assertNull(imageReaderProxy.getLastFrame())
    }

    @Test
    fun getLastFrame_newFrameAvailable_returnsBitmap() {
        setupImageMock(mockImage, WIDTH, HEIGHT)
        mockWhen(mockBitmapRepository.getDisplayRecorderBitmap(WIDTH, HEIGHT)).thenReturn(mockBitmap)
        mockWhen(mockBitmap.width).thenReturn(WIDTH)
        mockWhen(mockBitmap.height).thenReturn(HEIGHT)
        mockWhen(mockImageReader.acquireLatestImage()).thenReturn(mockImage)
        imageReaderProxy.resize(SIZE)

        val result = imageReaderProxy.getLastFrame()

        assertEquals(mockBitmap, result)
    }

    @Test
    fun getLastFrame_noNewFrame_returnsCachedFrame() {
        setupImageMock(mockImage, WIDTH, HEIGHT)
        mockWhen(mockBitmapRepository.getDisplayRecorderBitmap(WIDTH, HEIGHT)).thenReturn(mockBitmap)
        mockWhen(mockBitmap.width).thenReturn(WIDTH)
        mockWhen(mockBitmap.height).thenReturn(HEIGHT)
        // First call returns an image, second returns null
        mockWhen(mockImageReader.acquireLatestImage())
            .thenReturn(mockImage)
            .thenReturn(null)
        imageReaderProxy.resize(SIZE)

        imageReaderProxy.getLastFrame() // primes the cache
        val result = imageReaderProxy.getLastFrame()

        assertEquals(mockBitmap, result)
    }

    @Test
    fun getLastFrame_afterResize_doesNotReturnStaleFrame() {
        setupImageMock(mockImage, WIDTH, HEIGHT)
        mockWhen(mockBitmapRepository.getDisplayRecorderBitmap(WIDTH, HEIGHT)).thenReturn(mockBitmap)
        mockWhen(mockBitmap.width).thenReturn(WIDTH)
        mockWhen(mockBitmap.height).thenReturn(HEIGHT)
        mockWhen(mockImageReader.acquireLatestImage())
            .thenReturn(mockImage) // first resize: frame available
            .thenReturn(null)       // second resize: no new frame yet
        imageReaderProxy.resize(SIZE)
        imageReaderProxy.getLastFrame() // cache a frame from the first reader

        imageReaderProxy.resize(SIZE_2) // resize resets lastFrame

        assertNull(imageReaderProxy.getLastFrame())
    }

    @Test
    fun getLastFrame_runtimeException_throwsScreenCaptureException() {
        mockWhen(mockImageReader.acquireLatestImage()).thenThrow(RuntimeException("gralloc lock failed"))
        imageReaderProxy.resize(SIZE)

        assertThrows(ScreenCaptureException::class.java) {
            imageReaderProxy.getLastFrame()
        }
    }

    @Test
    fun getLastFrame_unsupportedOperationException_returnsNull() {
        mockWhen(mockImageReader.acquireLatestImage()).thenThrow(UnsupportedOperationException("unsupported format"))
        imageReaderProxy.resize(SIZE)

        assertNull(imageReaderProxy.getLastFrame())
    }

    // ---- helpers ----

    /**
     * Sets up a mock [Image] with the minimal plane/buffer structure needed for [ImageReaderProxy.getLastFrame]
     * to complete the pixel-copy path without crashing.
     *
     * We set rowStride == width * pixelStride so the "no padding" branch is taken, which calls
     * [Bitmap.copyPixelsFromBuffer] — the simplest path with the fewest moving parts.
     */
    private fun setupImageMock(image: Image, width: Int, height: Int) {
        mockWhen(image.width).thenReturn(width)
        mockWhen(image.height).thenReturn(height)

        val pixelStride = 4
        val rowStride = width * pixelStride
        val buffer = ByteBuffer.allocate(rowStride * height)

        val plane = mock(Image.Plane::class.java)
        mockWhen(plane.pixelStride).thenReturn(pixelStride)
        mockWhen(plane.rowStride).thenReturn(rowStride)
        mockWhen(plane.buffer).thenReturn(buffer)
        mockWhen(image.planes).thenReturn(arrayOf(plane))
    }
}

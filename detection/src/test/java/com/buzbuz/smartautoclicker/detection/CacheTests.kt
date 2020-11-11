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

import android.graphics.Bitmap
import android.graphics.Point
import android.media.Image
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.detection.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.detection.utils.anyNotNull

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

import java.nio.ByteBuffer

/** Test the [Cache] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], shadows = [ShadowBitmapCreator::class])
class CacheTests {

    private companion object {
        private const val TEST_DATA_DISPLAY_SIZE_WIDTH = 800
        private const val TEST_DATA_DISPLAY_SIZE_HEIGHT = 600
        private val TEST_DATA_DISPLAY_SIZE = Point(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT)

        private const val TEST_DATA_IMAGE_PIXEL_STRIDE = 1
        private const val TEST_DATA_IMAGE_ROW_STRIDE = TEST_DATA_DISPLAY_SIZE_WIDTH
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }

    @Mock private lateinit var mockBitmap: Bitmap
    @Mock private lateinit var mockImage: Image
    @Mock private lateinit var mockImagePlane: Image.Plane
    @Mock private lateinit var mockImagePlaneBuffer: ByteBuffer
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockBitmapCreator: ShadowBitmapCreator.BitmapCreator

    /** The object under test. */
    private lateinit var cache: Cache

    /** Assert if the [cache] values aren't the correct default ones. */
    private fun assertInitialCacheValues() {
        assertNull("Initial current image should be null", cache.currentImage)
        assertNull("Initial screen bitmap should be null", cache.screenBitmap)
        assertNull("Initial screen pixels should be null", cache.screenPixels)
        assertEquals("Initial current diff is invalid", 0L, cache.currentDiff)
        assertEquals("Initial crop index is invalid", 0, cache.cropIndex)
        assertEquals("Initial pixel cache size is invalid", 0, cache.pixelsCache.size())
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        ShadowBitmapCreator.setMockInstance(mockBitmapCreator)
        mockWhen(mockBitmapCreator.createBitmap(anyInt(), anyInt(), anyNotNull())).thenReturn(mockBitmap)

        mockWhen(mockImage.planes).thenReturn(arrayOf(mockImagePlane))
        mockWhen(mockImagePlane.pixelStride).thenReturn(TEST_DATA_IMAGE_PIXEL_STRIDE)
        mockWhen(mockImagePlane.rowStride).thenReturn(TEST_DATA_IMAGE_ROW_STRIDE)
        mockWhen(mockImagePlane.buffer).thenReturn(mockImagePlaneBuffer)

        cache = Cache(TEST_DATA_DISPLAY_SIZE, mockBitmapSupplier::getBitmap)
    }

    @After
    fun tearDown() {
        ShadowBitmapCreator.reset()
    }

    @Test
    fun initialValues() {
        assertInitialCacheValues()
    }

    @Test
    fun refresh_bitmapCacheCreation() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage()

        verify(mockBitmapCreator)
            .createBitmap(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT, Bitmap.Config.ARGB_8888)
        assertEquals("Invalid screen bitmap", mockBitmap, cache.screenBitmap)
        assertNotNull("Screen pixel cache should be initialized", cache.screenPixels)
        assertEquals(
            "Invalid screen pixels size",
            TEST_DATA_DISPLAY_SIZE_WIDTH * TEST_DATA_DISPLAY_SIZE_HEIGHT,
            cache.screenPixels!!.size)
    }

    @Test
    fun refresh_bitmapCaching() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage()

        inOrder(mockBitmap).apply {
            verify(mockBitmap).copyPixelsFromBuffer(mockImagePlaneBuffer)
            verify(mockBitmap).getPixels(cache.screenPixels, 0, TEST_DATA_IMAGE_ROW_STRIDE, 0, 0,
                TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT)
        }
    }

    @Test
    fun release_bitmapCache() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage()
        cache.release()

        assertInitialCacheValues()
    }
}
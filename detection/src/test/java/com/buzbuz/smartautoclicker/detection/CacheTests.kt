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

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.database.domain.Condition

import com.buzbuz.smartautoclicker.detection.shadows.ShadowBitmapCreator
import com.buzbuz.smartautoclicker.detection.utils.anyNotNull

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
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
        private const val TEST_DATA_DISPLAY_SIZE_WIDTH_2 = TEST_DATA_DISPLAY_SIZE_HEIGHT
        private const val TEST_DATA_DISPLAY_SIZE_HEIGHT_2 = TEST_DATA_DISPLAY_SIZE_WIDTH
        private val TEST_DATA_DISPLAY_SIZE_2 = Point(TEST_DATA_DISPLAY_SIZE_WIDTH_2, TEST_DATA_DISPLAY_SIZE_HEIGHT_2)

        private const val TEST_DATA_IMAGE_PIXEL_STRIDE = 1
        private const val TEST_DATA_IMAGE_ROW_STRIDE = TEST_DATA_DISPLAY_SIZE_WIDTH
        private const val TEST_DATA_IMAGE_ROW_STRIDE_2 = TEST_DATA_DISPLAY_SIZE_WIDTH_2

        private const val TEST_DATA_CONDITION_ID = 42L
        private const val TEST_DATA_EVENT_ID = 84L
        private const val TEST_DATA_CONDITION_PATH = "/this/is/a/path"
        private const val TEST_DATA_CONDITION_WIDTH = 100
        private const val TEST_DATA_CONDITION_HEIGHT = 100
        private const val TEST_DATA_CONDITION_THRESHOLD = 12
        private val TEST_DATA_CONDITION_AREA = Rect(
            TEST_DATA_CONDITION_WIDTH,
            TEST_DATA_CONDITION_HEIGHT,
            TEST_DATA_CONDITION_WIDTH * 2,
            TEST_DATA_CONDITION_HEIGHT * 2
        )
        private val TEST_DATA_CONDITION = Condition(
            TEST_DATA_CONDITION_ID,
            TEST_DATA_EVENT_ID,
            TEST_DATA_CONDITION_PATH,
            TEST_DATA_CONDITION_AREA,
            TEST_DATA_CONDITION_THRESHOLD,
        )
        private val TEST_DATA_CLICK_CONDITION_IMAGE_PIXELS =
            IntArray(TEST_DATA_CONDITION_WIDTH * TEST_DATA_CONDITION_HEIGHT)
    }

    /** Interface to be mocked in order to verify the calls to the bitmap supplier. */
    interface BitmapSupplier {
        fun getBitmap(path: String, width: Int, height: Int): Bitmap
    }

    @Mock private lateinit var mockCreatedBitmap: Bitmap
    @Mock private lateinit var mockCreatedBitmap2: Bitmap
    @Mock private lateinit var mockSuppliedBitmap: Bitmap
    @Mock private lateinit var mockImage: Image
    @Mock private lateinit var mockImage2: Image
    @Mock private lateinit var mockImagePlane: Image.Plane
    @Mock private lateinit var mockImagePlane2: Image.Plane
    @Mock private lateinit var mockImagePlaneBuffer: ByteBuffer
    @Mock private lateinit var mockImagePlaneBuffer2: ByteBuffer
    @Mock private lateinit var mockBitmapSupplier: BitmapSupplier
    @Mock private lateinit var mockBitmapCreator: ShadowBitmapCreator.BitmapCreator

    /** The object under test. */
    private lateinit var cache: Cache

    /** Assert if the [cache] values aren't the correct default ones. */
    private fun assertInitialCacheValues() {
        assertNull("Initial current image should be null", cache.currentImage)
        assertNull("Initial screen bitmap should be null", cache.screenBitmap)
        assertNull("Initial screen pixels should be null", cache.screenPixels)
        assertEquals("Initial display size is invalid", Rect(), cache.displaySize)
        assertEquals("Initial current diff is invalid", 0L, cache.currentDiff)
        assertEquals("Initial crop index is invalid", 0, cache.cropIndex)
        assertEquals("Initial pixel cache size is invalid", 0, cache.pixelsCache.size())
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock the bitmaps created by [Bitmap.createBitmap]
        ShadowBitmapCreator.setMockInstance(mockBitmapCreator)
        mockWhen(mockBitmapCreator.createBitmap(
            eq(TEST_DATA_DISPLAY_SIZE_WIDTH),
            eq(TEST_DATA_DISPLAY_SIZE_HEIGHT),
            anyNotNull()
        )).thenReturn(mockCreatedBitmap)
        mockWhen(mockBitmapCreator.createBitmap(
            eq(TEST_DATA_DISPLAY_SIZE_WIDTH_2),
            eq(TEST_DATA_DISPLAY_SIZE_HEIGHT_2),
            anyNotNull()
        )).thenReturn(mockCreatedBitmap2)

        // Mock the bitmaps supplied by bitmap supplier lambda
        mockWhen(mockBitmapSupplier.getBitmap(
            TEST_DATA_CONDITION_PATH,
            TEST_DATA_CONDITION_WIDTH,
            TEST_DATA_CONDITION_HEIGHT
        )).thenReturn(mockSuppliedBitmap)
        mockWhen(mockSuppliedBitmap.width).thenReturn(TEST_DATA_CONDITION_WIDTH)
        mockWhen(mockSuppliedBitmap.height).thenReturn(TEST_DATA_CONDITION_HEIGHT)

        // Mock the image data
        mockWhen(mockImage.planes).thenReturn(arrayOf(mockImagePlane))
        mockWhen(mockImagePlane.pixelStride).thenReturn(TEST_DATA_IMAGE_PIXEL_STRIDE)
        mockWhen(mockImagePlane.rowStride).thenReturn(TEST_DATA_IMAGE_ROW_STRIDE)
        mockWhen(mockImagePlane.buffer).thenReturn(mockImagePlaneBuffer)
        mockWhen(mockImage2.planes).thenReturn(arrayOf(mockImagePlane2))
        mockWhen(mockImagePlane2.pixelStride).thenReturn(TEST_DATA_IMAGE_PIXEL_STRIDE)
        mockWhen(mockImagePlane2.rowStride).thenReturn(TEST_DATA_IMAGE_ROW_STRIDE_2)
        mockWhen(mockImagePlane2.buffer).thenReturn(mockImagePlaneBuffer2)

        cache = Cache(mockBitmapSupplier::getBitmap)
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
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)

        verify(mockBitmapCreator)
            .createBitmap(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT, Bitmap.Config.ARGB_8888)
        assertEquals("Invalid screen bitmap", mockCreatedBitmap, cache.screenBitmap)
        assertNotNull("Screen pixel cache should be initialized", cache.screenPixels)
        assertEquals(
            "Invalid screen pixels size",
            TEST_DATA_DISPLAY_SIZE_WIDTH * TEST_DATA_DISPLAY_SIZE_HEIGHT,
            cache.screenPixels!!.size)
    }

    @Test
    fun refresh_bitmapCacheCreation_twice_sameDisplay() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)

        cache.currentImage = mockImage2
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE_2)

        verify(mockBitmapCreator)
            .createBitmap(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT, Bitmap.Config.ARGB_8888)
        verify(mockBitmapCreator)
            .createBitmap(TEST_DATA_DISPLAY_SIZE_WIDTH_2, TEST_DATA_DISPLAY_SIZE_HEIGHT_2, Bitmap.Config.ARGB_8888)
        assertEquals("Invalid screen bitmap", mockCreatedBitmap2, cache.screenBitmap)
        assertNotNull("Screen pixel cache should be initialized", cache.screenPixels)
        assertEquals(
            "Invalid screen pixels size",
            TEST_DATA_DISPLAY_SIZE_WIDTH_2 * TEST_DATA_DISPLAY_SIZE_HEIGHT_2,
            cache.screenPixels!!.size)
    }

    @Test
    fun refresh_bitmapCacheCreation_twice_newDisplay() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)

        verify(mockBitmapCreator)
            .createBitmap(TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT, Bitmap.Config.ARGB_8888)
        assertEquals("Invalid screen bitmap", mockCreatedBitmap, cache.screenBitmap)
        assertNotNull("Screen pixel cache should be initialized", cache.screenPixels)
        assertEquals(
            "Invalid screen pixels size",
            TEST_DATA_DISPLAY_SIZE_WIDTH * TEST_DATA_DISPLAY_SIZE_HEIGHT,
            cache.screenPixels!!.size)
    }

    @Test
    fun refresh_bitmapCaching() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)

        inOrder(mockCreatedBitmap).apply {
            verify(mockCreatedBitmap).copyPixelsFromBuffer(mockImagePlaneBuffer)
            verify(mockCreatedBitmap).getPixels(cache.screenPixels, 0, TEST_DATA_IMAGE_ROW_STRIDE, 0, 0,
                TEST_DATA_DISPLAY_SIZE_WIDTH, TEST_DATA_DISPLAY_SIZE_HEIGHT)
        }
    }

    @Test
    fun pixelCache_createItem() {
        val conditionPixelsCaptor = ArgumentCaptor.forClass(IntArray::class.java)

        val result = cache.pixelsCache.get(TEST_DATA_CONDITION)

        verify(mockSuppliedBitmap).getPixels(conditionPixelsCaptor.capture(), eq(0),
            eq(TEST_DATA_CONDITION_WIDTH), eq(0), eq(0), eq(TEST_DATA_CONDITION_WIDTH),
            eq(TEST_DATA_CONDITION_HEIGHT))
        assertNotNull("Result should not be null", result)
        assertTrue("Invalid pixel cache condition pixels arrays",
            conditionPixelsCaptor.value.contentEquals(result!!.first))
        assertTrue("Invalid pixel cache current pixels arrays",
            TEST_DATA_CLICK_CONDITION_IMAGE_PIXELS.contentEquals(result.second)
        )
    }

    @Test
    fun pixelCache_createItem_noBitmap() {
        mockWhen(mockBitmapSupplier.getBitmap(
            TEST_DATA_CONDITION_PATH,
            TEST_DATA_CONDITION_WIDTH,
            TEST_DATA_CONDITION_HEIGHT
        )).thenReturn(null)

        val result = cache.pixelsCache.get(TEST_DATA_CONDITION)

        verify(mockSuppliedBitmap, never()).getPixels(anyNotNull(), eq(0), eq(TEST_DATA_CONDITION_WIDTH),
            eq(0), eq(0), eq(TEST_DATA_CONDITION_WIDTH), eq(TEST_DATA_CONDITION_HEIGHT))
        assertNull("Result should be null", result)
    }

    @Test
    fun release_bitmapCache() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)
        cache.release()

        assertInitialCacheValues()
    }

    @Test
    fun release_pixelsCache() {
        cache.pixelsCache.get(TEST_DATA_CONDITION)
        cache.release()

        assertInitialCacheValues()
    }

    @Test
    fun release_bitmapCache_and_pixelsCache() {
        cache.currentImage = mockImage
        cache.refreshProcessedImage(TEST_DATA_DISPLAY_SIZE)
        cache.pixelsCache.get(TEST_DATA_CONDITION)
        cache.release()

        assertInitialCacheValues()
    }
}
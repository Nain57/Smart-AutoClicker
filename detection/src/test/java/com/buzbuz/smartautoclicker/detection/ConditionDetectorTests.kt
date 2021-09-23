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
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.util.LruCache

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.database.domain.AND

import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.database.domain.OR
import com.buzbuz.smartautoclicker.detection.utils.ProcessingData

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

/** Test the [ConditionDetector] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ConditionDetectorTests {

    private companion object {
        private const val TEST_DATA_NAME = "name"
        private const val TEST_DATA_NAME2 = "another name"
        private const val TEST_DATA_NAME3 = "name 3, name harderer"
        private const val TEST_DATA_PATH = "/path"
        private const val TEST_DATA_PATH2 = "/root/folder/directory/item"
        private const val TEST_DATA_PATH3 = "AnotherPathWeirdlyFormatted"
        private const val TEST_DATA_THRESHOLD = 12
        private val TEST_DATA_SCREEN_PART = Rect(
            0,
            0,
            ProcessingData.SCREEN_AREA.width() - 1,
            ProcessingData.SCREEN_AREA.height() - 1
        )
        private val TEST_DATA_SCREEN_PART2 = Rect(
            1,
            0,
            ProcessingData.SCREEN_AREA.width(),
            ProcessingData.SCREEN_AREA.height()
        )
        private val TEST_DATA_SCREEN_PART_OUTSIDE = Rect(
            -4,
            -2,
            ProcessingData.SCREEN_AREA.width() - 1,
            ProcessingData.SCREEN_AREA.height() - 1
        )
    }

    private lateinit var spiedCache: Cache
    @Mock private lateinit var mockPixelCache: LruCache<Condition, Pair<IntArray, IntArray>?>
    @Mock private lateinit var mockCurrentImage: Image
    @Mock private lateinit var mockScreenBitmap: Bitmap

    /** The object under test. */
    private lateinit var conditionDetector: ConditionDetector

    /**
     * Create a new click condition and mocks it in the pixels cache.
     *
     * @param path the path of the condition
     * @param area the area of the condition
     * @param threshold the difference threshold of the condition
     * @param pixelCacheInScreen state of the pixel cache. True for pixels that are currently displayed on screen, False
     *                           for other pixels, null for cache initialization error.
     *
     * @return the click condition.
     */
    private fun mockEventCondition(path: String, threshold: Int, area: Rect, pixelCacheInScreen: Boolean?): Condition {
        val condition = Condition(1L, 1L, path, area, threshold)
        mockWhen(mockPixelCache.get(condition))
            .thenReturn(when {
                pixelCacheInScreen == null -> null
                pixelCacheInScreen -> ProcessingData.getScreenPixelCacheForArea(area)
                else -> ProcessingData.getOtherPixelCacheForArea(area)
            })

        return condition
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Setup cache mocks
        spiedCache = spy(Cache() { _, _, _ -> null })
        doReturn(mockPixelCache).`when`(spiedCache).pixelsCache
        doReturn(mockCurrentImage).`when`(spiedCache).currentImage
        doReturn(mockScreenBitmap).`when`(spiedCache).screenBitmap
        doReturn(ProcessingData.SCREEN_AREA).`when`(spiedCache).displaySize

        // Setup cache for the image displayed on the screen
        doReturn(ProcessingData.SCREEN_PIXELS).`when`(spiedCache).screenPixels
        mockWhen(mockScreenBitmap.width).thenReturn(ProcessingData.SCREEN_SIZE)
        mockWhen(mockScreenBitmap.height).thenReturn(ProcessingData.SCREEN_SIZE)

        conditionDetector = ConditionDetector(spiedCache)
    }

    @Test
    fun empty() {
        assertNull(conditionDetector.detect(emptyList()))
    }

    @Test
    fun noConditions() {
        assertNull(conditionDetector.detect(listOf(
            ProcessingData.newEvent(name = TEST_DATA_NAME),
            ProcessingData.newEvent(name = TEST_DATA_NAME2),
            ProcessingData.newEvent(name = TEST_DATA_NAME3)
        )))
    }

    @Test
    fun oneEvent_oneCondition_detected_allScreen() {
        val validCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)

        val validEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(validCondition))
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_oneCondition_detected_partOfScreen() {
        val conditionArea = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, true)

        val validEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(conditionArea))
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_oneCondition_detected_outsideOfScreen() {
        val conditionArea = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART_OUTSIDE, true)

        val validEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(conditionArea))
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_oneCondition_notDetected() {
        val validCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)

        val validEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(validCondition))
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_oneCondition_error() {
        val validCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, null)

        val validEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(validCondition))
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_AND_multipleConditions_noneDetected() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, false)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, null)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = AND,
            conditions = listOf(errorCondition, otherCondition, onScreenCondition)
        )

        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_AND_multipleConditions_oneDetected() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, null)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = AND,
            conditions = listOf(errorCondition, otherCondition, onScreenCondition)
        )
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_AND_multipleConditions_oneDetected_oneInvalid() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, true)
        val invalidCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART_OUTSIDE, true)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = AND,
            conditions = listOf(invalidCondition, otherCondition, onScreenCondition)
        )
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_AND_multipleConditions_allDetected() {
        val onScreenCondition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val onScreenCondition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, true)
        val onScreenCondition3 = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, true)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = AND,
            conditions = listOf(onScreenCondition1, onScreenCondition2, onScreenCondition3)
        )
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_OR_multipleConditions_noneDetected() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, false)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, null)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = OR,
            conditions = listOf(errorCondition, otherCondition, onScreenCondition)
        )
        assertNull(conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_OR_multipleConditions_oneDetected() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, null)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = OR,
            conditions = listOf(errorCondition, otherCondition, onScreenCondition)
        )
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_OR_multipleConditions_oneDetected_oneInvalid() {
        val onScreenCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val invalidCondition = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART_OUTSIDE, true)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = OR,
            conditions = listOf(invalidCondition, otherCondition, onScreenCondition)
        )
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun oneEvent_OR_multipleConditions_allDetected() {
        val onScreenCondition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val onScreenCondition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, true)
        val onScreenCondition3 = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, true)

        val validEvent = ProcessingData.newEvent(
            name= TEST_DATA_NAME,
            operator = OR,
            conditions = listOf(onScreenCondition1, onScreenCondition2, onScreenCondition3)
        )
        assertEquals(validEvent, conditionDetector.detect(listOf(validEvent)))
    }

    @Test
    fun multipleEvents_noneDetected() {
        val condition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, false)
        val condition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val condition3 = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, false)

        assertNull(conditionDetector.detect(listOf(
            ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(condition1, condition2, condition3)),
            ProcessingData.newEvent(name = TEST_DATA_NAME2, operator = AND, conditions = listOf(condition1, condition2, condition3)),
            ProcessingData.newEvent(name = TEST_DATA_NAME3, operator = AND, conditions = listOf(condition1, condition2, condition3)),
        )))
    }

    @Test
    fun multipleEvents_allError() {
        val condition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, null)
        val condition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, null)
        val condition3 = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, null)

        assertNull(conditionDetector.detect(listOf(
            ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(condition1, condition2, condition3)),
            ProcessingData.newEvent(name = TEST_DATA_NAME2, operator = AND, conditions = listOf(condition1, condition2, condition3)),
            ProcessingData.newEvent(name = TEST_DATA_NAME3, operator = AND, conditions = listOf(condition1, condition2, condition3)),
        )))
    }

    @Test
    fun multipleEvents_onlyLastDetected() {
        val condition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, false)
        val condition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, false)
        val validCondition = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, true)
        val expectedClick = ProcessingData.newEvent(name = TEST_DATA_NAME3, operator = OR, conditions = listOf(condition1, condition2, validCondition))

        assertEquals(expectedClick, conditionDetector.detect(listOf(
            ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND, conditions = listOf(condition1, condition2)),
            ProcessingData.newEvent(name = TEST_DATA_NAME2, operator = AND, conditions = listOf(condition1, condition2)),
            expectedClick
        )))
    }

    @Test
    fun multipleEvents_allDetected() {
        val condition1 = mockEventCondition(TEST_DATA_PATH, TEST_DATA_THRESHOLD, ProcessingData.SCREEN_AREA, true)
        val condition2 = mockEventCondition(TEST_DATA_PATH2, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART, true)
        val condition3 = mockEventCondition(TEST_DATA_PATH3, TEST_DATA_THRESHOLD, TEST_DATA_SCREEN_PART2, true)
        val expectedEvent = ProcessingData.newEvent(name = TEST_DATA_NAME, operator = AND,
            conditions = listOf(condition1, condition2, condition3))

        assertEquals(expectedEvent, conditionDetector.detect(listOf(
            expectedEvent,
            ProcessingData.newEvent(name = TEST_DATA_NAME2, operator = AND, conditions = listOf(condition1, condition2, condition3)),
            ProcessingData.newEvent(name = TEST_DATA_NAME3, operator = AND, conditions = listOf(condition1, condition2, condition3)),
        )))
    }
}
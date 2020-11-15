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
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.util.LruCache

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.database.ClickInfo
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

/** Test the [ScenarioProcessor] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioProcessorTests {

    private companion object {
        private const val TEST_DATA_NAME = "name"
        private const val TEST_DATA_NAME2 = "another name"
        private const val TEST_DATA_NAME3 = "name 3, name harderer"
        private const val TEST_DATA_PATH = "/path"
        private const val TEST_DATA_PATH2 = "/root/folder/directory/item"
        private const val TEST_DATA_PATH3 = "AnotherPathWeirdleFormatted"
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
    }

    private lateinit var spiedCache: Cache
    @Mock private lateinit var mockPixelCache: LruCache<ClickCondition, Pair<IntArray, IntArray>?>
    @Mock private lateinit var mockCurrentImage: Image
    @Mock private lateinit var mockScreenBitmap: Bitmap

    /** The object under test. */
    private lateinit var scenarioProcessor: ScenarioProcessor

    /**
     * Create a new click condition and mocks it in the pixels cache.
     *
     * @param area the area of the condition
     * @param pixelCacheInScreen state of the pixel cache. True for pixels that are currently displayed on screen, False
     *                           for other pixels, null for cache initialization error.
     *
     * @return the click condition.
     */
    private fun mockClickCondition(path: String, area: Rect, pixelCacheInScreen: Boolean?): ClickCondition {
        val condition = ClickCondition(area, path)
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
        spiedCache = spy(Cache(Point(ProcessingData.SCREEN_SIZE, ProcessingData.SCREEN_SIZE)) { _, _, _ -> null })
        doReturn(mockPixelCache).`when`(spiedCache).pixelsCache
        doReturn(mockCurrentImage).`when`(spiedCache).currentImage
        doReturn(mockScreenBitmap).`when`(spiedCache).screenBitmap

        // Setup cache for the image displayed on the screen
        doReturn(ProcessingData.SCREEN_PIXELS).`when`(spiedCache).screenPixels
        mockWhen(mockScreenBitmap.width).thenReturn(ProcessingData.SCREEN_SIZE)
        mockWhen(mockScreenBitmap.height).thenReturn(ProcessingData.SCREEN_SIZE)

        scenarioProcessor = ScenarioProcessor(spiedCache)
    }

    @Test
    fun empty() {
        assertNull(scenarioProcessor.detect(emptyList()))
    }

    @Test
    fun noConditions() {
        assertNull(scenarioProcessor.detect(listOf(
            ProcessingData.newClickInfo(TEST_DATA_NAME),
            ProcessingData.newClickInfo(TEST_DATA_NAME2),
            ProcessingData.newClickInfo(TEST_DATA_NAME3)
        )))
    }

    @Test
    fun oneClick_oneCondition_detected_allScreen() {
        val validCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_PATH, ClickInfo.AND, listOf(validCondition))
        assertEquals(validClick, scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_oneCondition_detected_partOfScreen() {
        val conditionArea = mockClickCondition(TEST_DATA_PATH, TEST_DATA_SCREEN_PART, true)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(conditionArea))
        assertEquals(validClick, scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_oneCondition_notDetected() {
        val validCondition = mockClickCondition(TEST_DATA_PATH, TEST_DATA_SCREEN_PART, false)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(validCondition))
        assertNull(scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_oneCondition_error() {
        val validCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, null)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(validCondition))
        assertNull(scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_AND_multipleConditions_noneDetected() {
        val onScreenCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, false)
        val otherCondition = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART, null)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND,
            listOf(errorCondition, otherCondition, onScreenCondition))
        assertNull(scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_AND_multipleConditions_oneDetected() {
        val onScreenCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART, null)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND,
            listOf(errorCondition, otherCondition, onScreenCondition))
        assertNull(scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_AND_multipleConditions_allDetected() {
        val onScreenCondition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)
        val onScreenCondition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, true)
        val onScreenCondition3 = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, true)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND,
            listOf(onScreenCondition1, onScreenCondition2, onScreenCondition3))
        assertEquals(validClick, scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_OR_multipleConditions_noneDetected() {
        val onScreenCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, false)
        val otherCondition = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART, null)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.OR,
            listOf(errorCondition, otherCondition, onScreenCondition))
        assertNull(scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_OR_multipleConditions_oneDetected() {
        val onScreenCondition = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)
        val otherCondition = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val errorCondition = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, null)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.OR,
            listOf(errorCondition, otherCondition, onScreenCondition))
        assertEquals(validClick, scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun oneClick_OR_multipleConditions_allDetected() {
        val onScreenCondition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)
        val onScreenCondition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, true)
        val onScreenCondition3 = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, true)

        val validClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.OR,
            listOf(onScreenCondition1, onScreenCondition2, onScreenCondition3))
        assertEquals(validClick, scenarioProcessor.detect(listOf(validClick)))
    }

    @Test
    fun multipleClicks_noneDetected() {
        val condition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, false)
        val condition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val condition3 = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, false)

        assertNull(scenarioProcessor.detect(listOf(
            ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(condition1, condition2, condition3)),
            ProcessingData.newClickInfo(TEST_DATA_NAME2, ClickInfo.AND, listOf(condition1, condition2, condition3)),
            ProcessingData.newClickInfo(TEST_DATA_NAME3, ClickInfo.AND, listOf(condition1, condition2, condition3))
        )))
    }

    @Test
    fun multipleClicks_allError() {
        val condition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, null)
        val condition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, null)
        val condition3 = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, null)

        assertNull(scenarioProcessor.detect(listOf(
            ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(condition1, condition2, condition3)),
            ProcessingData.newClickInfo(TEST_DATA_NAME2, ClickInfo.AND, listOf(condition1, condition2, condition3)),
            ProcessingData.newClickInfo(TEST_DATA_NAME3, ClickInfo.AND, listOf(condition1, condition2, condition3))
        )))
    }

    @Test
    fun multipleClicks_onlyLastDetected() {
        val condition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, false)
        val condition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, false)
        val validCondition = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, true)
        val expectedClick = ProcessingData.newClickInfo(TEST_DATA_NAME3, ClickInfo.OR,
            listOf(condition1, condition2, validCondition))

        assertEquals(expectedClick, scenarioProcessor.detect(listOf(
            ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND, listOf(condition1, condition2)),
            ProcessingData.newClickInfo(TEST_DATA_NAME2, ClickInfo.AND, listOf(condition1, condition2)),
            expectedClick
        )))
    }

    @Test
    fun multipleClicks_allDetected() {
        val condition1 = mockClickCondition(TEST_DATA_PATH, ProcessingData.SCREEN_AREA, true)
        val condition2 = mockClickCondition(TEST_DATA_PATH2, TEST_DATA_SCREEN_PART, true)
        val condition3 = mockClickCondition(TEST_DATA_PATH3, TEST_DATA_SCREEN_PART2, true)
        val expectedClick = ProcessingData.newClickInfo(TEST_DATA_NAME, ClickInfo.AND,
            listOf(condition1, condition2, condition3))

        assertEquals(expectedClick, scenarioProcessor.detect(listOf(
            expectedClick,
            ProcessingData.newClickInfo(TEST_DATA_NAME2, ClickInfo.AND, listOf(condition1, condition2, condition3)),
            ProcessingData.newClickInfo(TEST_DATA_NAME3, ClickInfo.AND, listOf(condition1, condition2, condition3))
        )))
    }
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue

import io.mockk.coEvery
import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tests the creation of the displays of the conditions shown behind the gesture waypoints. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureWaypointConditionMapperTests {

    private companion object {
        val EVENT_ID = Identifier(databaseId = 1L)
        val CONDITION_ID = Identifier(databaseId = 10L)
        const val IMAGE_PATH = "imagePath"
    }

    private lateinit var context: Context
    private lateinit var bitmapRepository: BitmapRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        bitmapRepository = mockk()
    }

    @Test
    fun image_conditionBitmapAtCaptureSize() = runTest {
        val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        coEvery { bitmapRepository.getImageConditionBitmap(IMAGE_PATH, 100, 50) } returns bitmap

        val display = listOf(imageCondition(Rect(0, 0, 100, 50))).toWaypointConditions(context, bitmapRepository)

        assertSame(bitmap, display[CONDITION_ID]?.bitmap)
        assertNull(display[CONDITION_ID]?.label)
    }

    @Test
    fun image_missingBitmap_noDisplay() = runTest {
        coEvery { bitmapRepository.getImageConditionBitmap(any(), any(), any()) } returns null

        val display = listOf(imageCondition(Rect(0, 0, 100, 50))).toWaypointConditions(context, bitmapRepository)

        assertFalse(display.containsKey(CONDITION_ID))
    }

    @Test
    fun color_colorIndicatorBitmap() = runTest {
        val condition = ScreenCondition.Color(
            id = CONDITION_ID, eventId = EVENT_ID, name = "color", threshold = 0, shouldBeDetected = true,
            priority = 0, color = 0xFF00FF00.toInt(), detectionArea = Rect(0, 0, 10, 10),
        )

        val display = listOf(condition).toWaypointConditions(context, bitmapRepository)

        assertNotNull(display[CONDITION_ID]?.bitmap)
        assertNull(display[CONDITION_ID]?.label)
    }

    @Test
    fun text_textLabelOnly() = runTest {
        val display = listOf(textCondition("hello")).toWaypointConditions(context, bitmapRepository)

        assertNull(display[CONDITION_ID]?.bitmap)
        assertEquals("hello", display[CONDITION_ID]?.label)
    }

    @Test
    fun text_emptyText_noDisplay() = runTest {
        val display = listOf(textCondition("")).toWaypointConditions(context, bitmapRepository)

        assertFalse(display.containsKey(CONDITION_ID))
    }

    @Test
    fun number_integerValue_operationLabelOnly() = runTest {
        val condition = numberCondition(ComparisonOperation.GREATER_OR_EQUALS, CounterOperationValue.Number(12.0))

        val display = listOf(condition).toWaypointConditions(context, bitmapRepository)

        assertNull(display[CONDITION_ID]?.bitmap)
        assertEquals("≥ 12", display[CONDITION_ID]?.label)
    }

    @Test
    fun number_decimalValue_operationLabel() = runTest {
        val condition = numberCondition(ComparisonOperation.LOWER, CounterOperationValue.Number(2.5))

        val display = listOf(condition).toWaypointConditions(context, bitmapRepository)

        assertEquals("< 2.5", display[CONDITION_ID]?.label)
    }

    @Test
    fun number_counterValue_operationLabelWithCounterName() = runTest {
        val condition = numberCondition(ComparisonOperation.EQUALS, CounterOperationValue.Counter("score"))

        val display = listOf(condition).toWaypointConditions(context, bitmapRepository)

        assertEquals("= score", display[CONDITION_ID]?.label)
    }

    @Test
    fun number_emptyCounterName_operationLabelWithPlaceholder() = runTest {
        val condition = numberCondition(ComparisonOperation.EQUALS, CounterOperationValue.Counter(""))

        val display = listOf(condition).toWaypointConditions(context, bitmapRepository)

        assertEquals("= ?", display[CONDITION_ID]?.label)
    }

    private fun imageCondition(area: Rect) = ScreenCondition.Image(
        id = CONDITION_ID, eventId = EVENT_ID, name = "image", threshold = 0, shouldBeDetected = true, priority = 0,
        path = IMAGE_PATH, area = area, detectionType = EXACT,
    )

    private fun textCondition(text: String) = ScreenCondition.Text(
        id = CONDITION_ID, eventId = EVENT_ID, name = "text", threshold = 0, shouldBeDetected = true, priority = 0,
        text = text, detectionArea = Rect(0, 0, 10, 10), alphabet = OCRAlphabet.LATIN,
    )

    private fun numberCondition(operation: ComparisonOperation, value: CounterOperationValue) = ScreenCondition.Number(
        id = CONDITION_ID, eventId = EVENT_ID, name = "number", threshold = 0, priority = 0,
        detectionArea = Rect(0, 0, 10, 10), comparisonOperation = operation, counterValue = value,
    )
}

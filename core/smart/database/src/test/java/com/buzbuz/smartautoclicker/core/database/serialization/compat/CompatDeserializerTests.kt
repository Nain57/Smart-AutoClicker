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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for bounds-clamping logic in [CompatDeserializer]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CompatDeserializerTests {

    private val deserializer = object : CompatDeserializer() {}

    private fun createJsonImageCondition(detectionType: Int): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(1L),
            "eventId" to JsonPrimitive(1L),
            "name" to JsonPrimitive("test"),
            "priority" to JsonPrimitive(0),
            "type" to JsonPrimitive(ConditionType.ON_IMAGE_DETECTED.name),
            "path" to JsonPrimitive("/test/path"),
            "areaLeft" to JsonPrimitive(0),
            "areaTop" to JsonPrimitive(0),
            "areaRight" to JsonPrimitive(100),
            "areaBottom" to JsonPrimitive(100),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(detectionType),
            "threshold" to JsonPrimitive(4),
        )
    )

    @Test
    fun deserializeConditionImageDetected_detectionType_exactMatch_isPreserved() {
        // Given detectionType = 1 (EXACT)
        val json = createJsonImageCondition(detectionType = 1)

        // When
        val result = deserializer.deserializeConditionImageDetected(json)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.detectionType)
    }

    @Test
    fun deserializeConditionImageDetected_detectionType_wholeScreen_isPreserved() {
        // Given detectionType = 2 (WHOLE_SCREEN)
        val json = createJsonImageCondition(detectionType = 2)

        // When
        val result = deserializer.deserializeConditionImageDetected(json)

        // Then
        assertNotNull(result)
        assertEquals(2, result!!.detectionType)
    }

    @Test
    fun deserializeConditionImageDetected_detectionType_inArea_isPreserved() {
        // Given detectionType = 3 (IN_AREA) — this was incorrectly clamped to 2 when
        // DETECTION_TYPE_UPPER_BOUND was wrongly set to 2 instead of 3.
        val json = createJsonImageCondition(detectionType = 3)

        // When
        val result = deserializer.deserializeConditionImageDetected(json)

        // Then
        assertNotNull(result)
        assertEquals(3, result!!.detectionType)
    }

    @Test
    fun deserializeConditionImageDetected_detectionType_belowLowerBound_isClampedToDefault() {
        // Given detectionType = 0, below DETECTION_TYPE_LOWER_BOUND = 1
        val json = createJsonImageCondition(detectionType = 0)

        // When
        val result = deserializer.deserializeConditionImageDetected(json)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.detectionType)
    }

    @Test
    fun deserializeConditionImageDetected_detectionType_aboveUpperBound_isClamped() {
        // Given detectionType = 4, above DETECTION_TYPE_UPPER_BOUND = 3
        val json = createJsonImageCondition(detectionType = 4)

        // When
        val result = deserializer.deserializeConditionImageDetected(json)

        // Then
        assertNotNull(result)
        assertEquals(3, result!!.detectionType)
    }

    // ===== deserializeConditionNumberDetected – threshold =====

    private fun createJsonNumberCondition(threshold: Int? = null): JsonObject {
        val map = mutableMapOf(
            "id" to JsonPrimitive(1L),
            "eventId" to JsonPrimitive(1L),
            "name" to JsonPrimitive("test"),
            "type" to JsonPrimitive(ConditionType.ON_NUMBER_DETECTED.name),
            "detectionAreaLeft" to JsonPrimitive(0),
            "detectionAreaTop" to JsonPrimitive(0),
            "detectionAreaRight" to JsonPrimitive(100),
            "detectionAreaBottom" to JsonPrimitive(100),
            "numberCounterComparisonOperation" to JsonPrimitive(CounterComparisonOperation.GREATER.name),
            "numberCounterValue" to JsonPrimitive(0.0),
        )
        if (threshold != null) map["threshold"] = JsonPrimitive(threshold)
        return JsonObject(map)
    }

    @Test
    fun deserializeConditionNumberDetected_threshold_validValue_isPreserved() {
        val json = createJsonNumberCondition(threshold = 10)

        val result = deserializer.deserializeConditionNumberDetected(json)

        assertNotNull(result)
        assertEquals(10, result!!.threshold)
    }

    @Test
    fun deserializeConditionNumberDetected_threshold_belowLowerBound_isClamped() {
        // threshold = -1, below CONDITION_THRESHOLD_LOWER_BOUND = 0
        val json = createJsonNumberCondition(threshold = -1)

        val result = deserializer.deserializeConditionNumberDetected(json)

        assertNotNull(result)
        assertEquals(0, result!!.threshold)
    }

    @Test
    fun deserializeConditionNumberDetected_threshold_aboveUpperBound_isClamped() {
        // threshold = 99, above CONDITION_THRESHOLD_UPPER_BOUND = 20
        val json = createJsonNumberCondition(threshold = 99)

        val result = deserializer.deserializeConditionNumberDetected(json)

        assertNotNull(result)
        assertEquals(20, result!!.threshold)
    }

    @Test
    fun deserializeConditionNumberDetected_threshold_missing_usesDefault() {
        val json = createJsonNumberCondition(threshold = null)

        val result = deserializer.deserializeConditionNumberDetected(json)

        assertNotNull(result)
        assertEquals(4, result!!.threshold)
    }
}

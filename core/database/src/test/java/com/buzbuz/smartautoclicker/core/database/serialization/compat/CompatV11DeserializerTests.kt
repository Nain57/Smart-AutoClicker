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

import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.serialization.DeserializerFactory
import com.buzbuz.smartautoclicker.core.database.utils.encodeToJsonObject

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Test the [CompatV11Deserializer] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CompatV11DeserializerTests {

    private companion object {
        private const val VERSION_MINIMUM = 8
        private const val VERSION_MAXIMUM = 10
    }

    private fun createJsonClick(clickOnCondition: Boolean, x: Int? = null, y: Int? = null): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(1L),
            "eventId" to JsonPrimitive(1L),
            "name" to JsonPrimitive("toto"),
            "priority" to JsonPrimitive(1L),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(clickOnCondition),
            "x" to JsonPrimitive(x),
            "y" to JsonPrimitive(y),
            "pressDuration" to JsonPrimitive(1),
        )
    )

    private fun getDefaultImageCondition(shouldBeDetected: Boolean): ConditionEntity = ConditionEntity(
        id = 1L, eventId = 1L, name = "toto", type = ConditionType.ON_IMAGE_DETECTED, path = "/toto",
        areaLeft = 1, areaTop = 2, areaRight = 3, areaBottom = 4, threshold = 10,
        shouldBeDetected = shouldBeDetected, detectionType = 1,
    )

    @Test
    fun deserialization_factory_availability() {
        for (i in VERSION_MINIMUM..VERSION_MAXIMUM) {
            DeserializerFactory.create(i).let { deserializer ->
                assertNotNull(deserializer)
                assertTrue(deserializer is CompatV11Deserializer)
            }
        }
    }

    @Test
    fun deserialization_scenario_detection_quality() {
        // Given
        val scenario = ScenarioEntity(1L, "Toto", 1000)

        // When
        val decodedScenario = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV11Deserializer)
            .deserializeScenario(scenario.encodeToJsonObject())

        // Then
        assertNotNull(decodedScenario)
        assertEquals(scenario.detectionQuality + 600, decodedScenario!!.detectionQuality)
    }

    @Test
    fun deserialization_action_click_user_selected() {
        // Given
        val x = 10
        val y = 20
        val jsonClick = createJsonClick(clickOnCondition = false, x, y)

        // When
        val decodedClick = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV11Deserializer)
            .deserializeAction(jsonClick, emptyList())

        // Then
        assertNotNull(decodedClick)
        assertEquals(ClickPositionType.USER_SELECTED, decodedClick!!.clickPositionType)
        assertEquals(x, decodedClick.x)
        assertEquals(y, decodedClick.y)
        assertNull(decodedClick.clickOnConditionId)
    }

    @Test
    fun deserialization_action_click_on_condition_valid_condition() {
        // Given
        val jsonClick = createJsonClick(clickOnCondition = true)
        val condition = getDefaultImageCondition(true)
        // When
        val decodedClick = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV11Deserializer)
            .deserializeAction(jsonClick, listOf(condition))

        // Then
        assertNotNull(decodedClick)
        assertEquals(ClickPositionType.ON_DETECTED_CONDITION, decodedClick!!.clickPositionType)
        assertEquals(condition.id, decodedClick.clickOnConditionId)
        assertNull(decodedClick.x)
        assertNull(decodedClick.y)
    }

    @Test
    fun deserialization_action_click_on_condition_no_conditions() {
        // Given
        val jsonClick = createJsonClick(clickOnCondition = true)
        // When
        val decodedClick = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV11Deserializer)
            .deserializeAction(jsonClick, emptyList())

        // Then
        assertNotNull(decodedClick)
        assertEquals(ClickPositionType.ON_DETECTED_CONDITION, decodedClick!!.clickPositionType)
        assertNull(decodedClick.clickOnConditionId)
        assertNull(decodedClick.x)
        assertNull(decodedClick.y)
    }

    @Test
    fun deserialization_action_click_on_condition_invalid_conditions() {
        // Given
        val jsonClick = createJsonClick(clickOnCondition = true)
        val condition = getDefaultImageCondition(shouldBeDetected = false)
        // When
        val decodedClick = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV11Deserializer)
            .deserializeAction(jsonClick, listOf(condition))

        // Then
        assertNotNull(decodedClick)
        assertEquals(ClickPositionType.ON_DETECTED_CONDITION, decodedClick!!.clickPositionType)
        assertNull(decodedClick.clickOnConditionId)
        assertNull(decodedClick.x)
        assertNull(decodedClick.y)
    }
}
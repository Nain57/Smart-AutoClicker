/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.backup

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.room.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.database.room.entity.*

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Test the [ScenarioSerializer] class. */
@OptIn(ExperimentalSerializationApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioSerializerTests {

    companion object {
        private val NULL_BOOLEAN_JSON_PRIMITIVE = JsonPrimitive(null as Boolean?)
        private val NULL_NUMBER_JSON_PRIMITIVE = JsonPrimitive(null as Long?)
        private val NULL_STRING_JSON_PRIMITIVE = JsonPrimitive(null as String?)

        private val DEFAULT_SCREEN_SIZE = Point(800,600)

        private val DEFAULT_COMPLETE_SCENARIO = CompleteScenario(
            scenario = ScenarioEntity(1, "Scenario", 600, 1),
            events = listOf(
                CompleteEventEntity(
                    event = EventEntity(1, 1, "Event", 1, 0, null),
                    conditions = listOf(
                        ConditionEntity(1, 1, "Condition", "/toto/tutu", 1, 2, 3, 4, 5, 1, true)
                    ),
                    actions = listOf(
                        CompleteActionEntity(
                            action = ActionEntity(1, 1, 0, "Intent", ActionType.INTENT,
                                isAdvanced = false, isBroadcast = false, intentAction = "org.action", flags = 0,
                                componentName = "org.action/org.action.TOTO",
                            ),
                            intentExtras = listOf(
                                IntentExtraEntity(1, 1, IntentExtraType.BOOLEAN, "ExtraKey", "true")
                            )
                        )
                    )
                )
            ),
            endConditions = listOf(
                EndConditionEntity(1, 1, 1, 20)
            )
        )
    }

    @Test
    fun serialization_invalidVersion() {
        val backup = ScenarioBackup(
            version = 0,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertNull(deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }

    @Test
    fun serialization_sameVersion() {
        val backup = ScenarioBackup(
            version = CLICK_DATABASE_VERSION,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertEquals(backup, deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }

    @Test
    fun serialization_otherValidVersion() {
        val backup = ScenarioBackup(
            version = 19,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertEquals(backup, deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }

    @Test
    fun deserialization_scenario_mandatory_id() {
        val jsonScenario = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("toto"),
            "detectionQuality" to JsonPrimitive(600),
            "endConditionOperator" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonScenario.deserializeScenarioCompat())
        }
    }

    @Test
    fun deserialization_scenario_optional() {
        val jsonScenario = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "detectionQuality" to NULL_NUMBER_JSON_PRIMITIVE,
            "endConditionOperator" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonScenario.deserializeScenarioCompat())
        }
    }

    @Test
    fun deserialization_scenario_optional_detectionQuality() {
        val jsonScenarioLower = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "name" to JsonPrimitive("toto"),
            "detectionQuality" to JsonPrimitive(0),
            "endConditionOperator" to JsonPrimitive(1),
        ))
        val jsonScenarioUpper = JsonObject(mapOf(
            "id" to JsonPrimitive(2),
            "name" to JsonPrimitive("tato"),
            "detectionQuality" to JsonPrimitive(Integer.MAX_VALUE),
            "endConditionOperator" to JsonPrimitive(1),
        ))
        val jsonScenarioNull = JsonObject(mapOf(
            "id" to JsonPrimitive(2),
            "name" to JsonPrimitive("tato"),
            "detectionQuality" to NULL_NUMBER_JSON_PRIMITIVE,
            "endConditionOperator" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertEquals(DETECTION_QUALITY_LOWER_BOUND, jsonScenarioLower.deserializeScenarioCompat()?.detectionQuality)
            assertEquals(DETECTION_QUALITY_UPPER_BOUND, jsonScenarioUpper.deserializeScenarioCompat()?.detectionQuality)
            assertEquals(DETECTION_QUALITY_DEFAULT_VALUE, jsonScenarioNull.deserializeScenarioCompat()?.detectionQuality)
        }
    }

    @Test
    fun deserialization_scenario_optional_endConditionOperator() {
        val jsonScenarioLower = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "name" to JsonPrimitive("toto"),
            "detectionQuality" to JsonPrimitive(600),
            "endConditionOperator" to JsonPrimitive(0),
        ))
        val jsonScenarioUpper = JsonObject(mapOf(
            "id" to JsonPrimitive(2),
            "name" to JsonPrimitive("tato"),
            "detectionQuality" to JsonPrimitive(600),
            "endConditionOperator" to JsonPrimitive(Integer.MAX_VALUE),
        ))
        val jsonScenarioNull = JsonObject(mapOf(
            "id" to JsonPrimitive(2),
            "name" to JsonPrimitive("tato"),
            "detectionQuality" to JsonPrimitive(600),
            "endConditionOperator" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertEquals(OPERATOR_LOWER_BOUND, jsonScenarioLower.deserializeScenarioCompat()?.endConditionOperator)
            assertEquals(OPERATOR_UPPER_BOUND, jsonScenarioUpper.deserializeScenarioCompat()?.endConditionOperator)
            assertEquals(OPERATOR_DEFAULT_VALUE, jsonScenarioNull.deserializeScenarioCompat()?.endConditionOperator)

        }
    }

    @Test
    fun deserialization_endCondition_mandatory_ids() {
        val jsonEndConditionNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "scenarioId" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(2),
            "executions" to JsonPrimitive(1),
        ))
        val jsonEndConditionNullScenarioId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(2),
            "executions" to JsonPrimitive(1),
        ))
        val jsonEndConditionNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(45),
            "scenarioId" to JsonPrimitive(89),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "executions" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                emptyList<EndConditionEntity>(),
                JsonArray(listOf(jsonEndConditionNullId, jsonEndConditionNullScenarioId, jsonEndConditionNullEventId))
                    .deserializeEndConditionsCompat()
            )
        }
    }

    @Test
    fun deserialization_endCondition_optional() {
        val jsonEndCondition = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(3),
            "executions" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertEquals(
                END_CONDITION_EXECUTION_DEFAULT_VALUE,
                JsonArray(listOf(jsonEndCondition)).deserializeEndConditionsCompat().first().executions
            )
        }
    }

    @Test
    fun deserialization_event_mandatory_id() {
        val jsonEventNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "scenarioId" to JsonPrimitive(1),
            "name" to JsonPrimitive("Toto"),
            "conditionOperator" to JsonPrimitive(1),
            "priority" to JsonPrimitive(0),
            "stopAfter" to JsonPrimitive(10),
        ))
        val jsonEventNullScenarioId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("Toto"),
            "conditionOperator" to JsonPrimitive(1),
            "priority" to JsonPrimitive(0),
            "stopAfter" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonEventNullId.deserializeEventCompat())
            assertNull(jsonEventNullScenarioId.deserializeEventCompat())
        }
    }

    @Test
    fun deserialization_event_optional() {
        val jsonEvent = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to JsonPrimitive(2),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "conditionOperator" to NULL_NUMBER_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "stopAfter" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            val event = jsonEvent.deserializeEventCompat()
            assertEquals("", event?.name)
            assertEquals(OPERATOR_DEFAULT_VALUE, event?.conditionOperator)
            assertEquals(0, event?.priority)
            assertNull(event?.stopAfter)
        }
    }

    @Test
    fun deserialization_event_optional_conditionOperator() {
        val jsonEventLower = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to JsonPrimitive(2),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "conditionOperator" to JsonPrimitive(Integer.MIN_VALUE),
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "stopAfter" to NULL_NUMBER_JSON_PRIMITIVE,
        ))
        val jsonEventUpper = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to JsonPrimitive(2),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "conditionOperator" to JsonPrimitive(Integer.MAX_VALUE),
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "stopAfter" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertEquals(OPERATOR_LOWER_BOUND, jsonEventLower.deserializeEventCompat()?.conditionOperator)
            assertEquals(OPERATOR_UPPER_BOUND, jsonEventUpper.deserializeEventCompat()?.conditionOperator)
        }
    }
}
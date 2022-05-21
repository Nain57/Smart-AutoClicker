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
    fun deserialization_event_mandatory_ids() {
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
        val jsonEventDefault = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "scenarioId" to JsonPrimitive(2),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "conditionOperator" to NULL_NUMBER_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "stopAfter" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertEquals(OPERATOR_LOWER_BOUND, jsonEventLower.deserializeEventCompat()?.conditionOperator)
            assertEquals(OPERATOR_UPPER_BOUND, jsonEventUpper.deserializeEventCompat()?.conditionOperator)
            assertEquals(OPERATOR_DEFAULT_VALUE, jsonEventDefault.deserializeEventCompat()?.conditionOperator)
        }
    }

    @Test
    fun deserialization_condition_mandatory_ids() {
        val jsonConditionNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                emptyList<ConditionEntity>(),
                JsonArray(listOf(jsonConditionNullId, jsonConditionNullEventId)).deserializeConditionsCompat()
            )
        }
    }

    @Test
    fun deserialization_condition_mandatory_path() {
        val jsonConditionNullPath = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to NULL_STRING_JSON_PRIMITIVE,
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                emptyList<ConditionEntity>(),
                JsonArray(listOf(jsonConditionNullPath)).deserializeConditionsCompat()
            )
        }
    }

    @Test
    fun deserialization_condition_mandatory_area() {
        val jsonConditionNullLeft = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to NULL_NUMBER_JSON_PRIMITIVE,
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionNullTop = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to NULL_NUMBER_JSON_PRIMITIVE,
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionNullRight = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to NULL_NUMBER_JSON_PRIMITIVE,
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionNullBottom = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("Toto"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                emptyList<ConditionEntity>(),
                JsonArray(listOf(
                    jsonConditionNullLeft,
                    jsonConditionNullTop,
                    jsonConditionNullRight,
                    jsonConditionNullBottom
                )).deserializeEndConditionsCompat()
            )
        }
    }

    @Test
    fun deserialization_condition_optional_name() {
        val jsonConditionNullName = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                "",
                JsonArray(listOf(jsonConditionNullName)).deserializeConditionsCompat().first().name
            )
        }
    }

    @Test
    fun deserialization_condition_optional_shouldBeDetected() {
        val jsonConditionNullShouldBeDetected = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to NULL_BOOLEAN_JSON_PRIMITIVE,
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            assertEquals(
                true,
                JsonArray(listOf(jsonConditionNullShouldBeDetected)).deserializeConditionsCompat()
                    .first().shouldBeDetected
            )
        }
    }

    @Test
    fun deserialization_condition_optional_detection_type() {
        val jsonConditionLower = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(Integer.MIN_VALUE),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionUpper = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(Integer.MAX_VALUE),
            "threshold" to JsonPrimitive(10),
        ))
        val jsonConditionDefault = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to NULL_NUMBER_JSON_PRIMITIVE,
            "threshold" to JsonPrimitive(10),
        ))

        ScenarioSerializer().apply {
            val conditions = JsonArray(listOf(jsonConditionLower, jsonConditionUpper, jsonConditionDefault))
                .deserializeConditionsCompat()

            assertEquals(DETECTION_TYPE_LOWER_BOUND, conditions[0].detectionType)
            assertEquals(DETECTION_TYPE_UPPER_BOUND, conditions[1].detectionType)
            assertEquals(DETECTION_TYPE_DEFAULT_VALUE, conditions[2].detectionType)
        }
    }

    @Test
    fun deserialization_condition_optional_threshold() {
        val jsonConditionLower = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(Integer.MIN_VALUE),
        ))
        val jsonConditionUpper = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(Integer.MAX_VALUE),
        ))
        val jsonConditionDefault = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "path" to JsonPrimitive("/Toto/Tutu"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "name" to JsonPrimitive("Tutu"),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            val conditions = JsonArray(listOf(jsonConditionLower, jsonConditionUpper, jsonConditionDefault))
                .deserializeConditionsCompat()

            assertEquals(CONDITION_THRESHOLD_LOWER_BOUND, conditions[0].threshold)
            assertEquals(CONDITION_THRESHOLD_UPPER_BOUND, conditions[1].threshold)
            assertEquals(CONDITION_THRESHOLD_DEFAULT_VALUE, conditions[2].threshold)
        }
    }

    @Test
    fun deserialization_action_click_mandatory_ids() {
        val jsonClickNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(false),
            "x" to JsonPrimitive(5),
            "y" to JsonPrimitive(5),
            "pressDuration" to JsonPrimitive(1),
        ))
        val jsonClickNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(false),
            "x" to JsonPrimitive(5),
            "y" to JsonPrimitive(5),
            "pressDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonClickNullId.deserializeClickActionCompat())
            assertNull(jsonClickNullEventId.deserializeClickActionCompat())
        }
    }

    @Test
    fun deserialization_action_click_mandatory_coordinates() {
        val clickOnCondition = false
        val jsonClickNullX = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(clickOnCondition),
            "x" to NULL_NUMBER_JSON_PRIMITIVE,
            "y" to JsonPrimitive(5),
            "pressDuration" to JsonPrimitive(1),
        ))
        val jsonClickNullY = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(clickOnCondition),
            "x" to JsonPrimitive(5),
            "y" to NULL_NUMBER_JSON_PRIMITIVE,
            "pressDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonClickNullX.deserializeClickActionCompat())
            assertNull(jsonClickNullY.deserializeClickActionCompat())
        }
    }

    @Test
    fun deserialization_action_click_optional_coordinates() {
        val clickOnCondition = true
        val jsonClickNullX = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(clickOnCondition),
            "x" to NULL_NUMBER_JSON_PRIMITIVE,
            "y" to JsonPrimitive(5),
            "pressDuration" to JsonPrimitive(1),
        ))
        val jsonClickNullY = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(clickOnCondition),
            "x" to JsonPrimitive(5),
            "y" to NULL_NUMBER_JSON_PRIMITIVE,
            "pressDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonClickNullX.deserializeClickActionCompat())
            assertNotNull(jsonClickNullY.deserializeClickActionCompat())
        }
    }

    @Test
    fun deserialization_action_click_optional_others() {
        val jsonClick = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "type" to JsonPrimitive(ActionType.CLICK.name),
            "clickOnCondition" to JsonPrimitive(false),
            "x" to JsonPrimitive(5),
            "y" to JsonPrimitive(5),
            "pressDuration" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonClick.deserializeClickActionCompat())
        }
    }

    @Test
    fun deserialization_action_swipe_mandatory_ids() {
        val jsonSwipeNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to JsonPrimitive(5),
            "toX" to JsonPrimitive(10),
            "toY" to JsonPrimitive(10),
            "swipeDuration" to JsonPrimitive(1),
        ))
        val jsonSwipeNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to JsonPrimitive(5),
            "toX" to JsonPrimitive(10),
            "toY" to JsonPrimitive(10),
            "swipeDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonSwipeNullId.deserializeSwipeActionCompat())
            assertNull(jsonSwipeNullEventId.deserializeSwipeActionCompat())
        }
    }

    @Test
    fun deserialization_action_swipe_mandatory_coordinates() {
        val jsonSwipeNullFromX = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to NULL_NUMBER_JSON_PRIMITIVE,
            "fromY" to JsonPrimitive(5),
            "toX" to JsonPrimitive(10),
            "toY" to JsonPrimitive(10),
            "swipeDuration" to JsonPrimitive(1),
        ))
        val jsonSwipeNullFromY = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to NULL_NUMBER_JSON_PRIMITIVE,
            "toX" to JsonPrimitive(10),
            "toY" to JsonPrimitive(10),
            "swipeDuration" to JsonPrimitive(1),
        ))
        val jsonSwipeNullToX = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to JsonPrimitive(5),
            "toX" to NULL_NUMBER_JSON_PRIMITIVE,
            "toY" to JsonPrimitive(10),
            "swipeDuration" to JsonPrimitive(1),
        ))
        val jsonSwipeNullToY = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to JsonPrimitive(5),
            "toX" to JsonPrimitive(10),
            "toY" to NULL_NUMBER_JSON_PRIMITIVE,
            "swipeDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonSwipeNullFromX.deserializeSwipeActionCompat())
            assertNull(jsonSwipeNullFromY.deserializeSwipeActionCompat())
            assertNull(jsonSwipeNullToX.deserializeSwipeActionCompat())
            assertNull(jsonSwipeNullToY.deserializeSwipeActionCompat())
        }
    }

    @Test
    fun deserialization_action_swipe_optionals() {
        val jsonSwipe = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "fromX" to JsonPrimitive(5),
            "fromY" to JsonPrimitive(5),
            "toX" to JsonPrimitive(10),
            "toY" to JsonPrimitive(10),
            "swipeDuration" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonSwipe.deserializeSwipeActionCompat())
        }
    }

    @Test
    fun deserialization_action_pause_mandatory_ids() {
        val jsonPauseNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.PAUSE.name),
            "pauseDuration" to JsonPrimitive(1),
        ))
        val jsonPauseNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.PAUSE.name),
            "pauseDuration" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonPauseNullId.deserializePauseActionCompat())
            assertNull(jsonPauseNullEventId.deserializePauseActionCompat())
        }
    }

    @Test
    fun deserialization_action_pause_optionals() {
        val jsonPause = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "type" to JsonPrimitive(ActionType.SWIPE.name),
            "pauseDuration" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonPause.deserializePauseActionCompat())
        }
    }

    @Test
    fun deserialization_action_intent_mandatory_ids() {
        val jsonIntentNullId = JsonObject(mapOf(
            "id" to NULL_NUMBER_JSON_PRIMITIVE,
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.INTENT.name),
            "isAdvanced" to JsonPrimitive(false),
            "isBroadcast" to JsonPrimitive(false),
            "intentAction" to JsonPrimitive("com.tutu.ACTION"),
            "componentName" to JsonPrimitive("com.toto/com.toto.tutu"),
            "flags" to JsonPrimitive(1),
        ))
        val jsonIntentNullEventId = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to NULL_NUMBER_JSON_PRIMITIVE,
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.INTENT.name),
            "isAdvanced" to JsonPrimitive(false),
            "isBroadcast" to JsonPrimitive(false),
            "intentAction" to JsonPrimitive("com.tutu.ACTION"),
            "componentName" to JsonPrimitive("com.toto/com.toto.tutu"),
            "flags" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonIntentNullId.deserializeIntentActionCompat())
            assertNull(jsonIntentNullEventId.deserializeIntentActionCompat())
        }
    }

    @Test
    fun deserialization_action_intent_mandatory_intentAction() {
        val jsonIntentNullAction = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to JsonPrimitive("TOTO"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.INTENT.name),
            "isAdvanced" to JsonPrimitive(false),
            "isBroadcast" to JsonPrimitive(false),
            "intentAction" to NULL_STRING_JSON_PRIMITIVE,
            "componentName" to JsonPrimitive("com.toto/com.toto.tutu"),
            "flags" to JsonPrimitive(1),
        ))

        ScenarioSerializer().apply {
            assertNull(jsonIntentNullAction.deserializeIntentActionCompat())
        }
    }

    @Test
    fun deserialization_action_intent_optionals() {
        val jsonIntentNullAction = JsonObject(mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(1),
            "name" to NULL_STRING_JSON_PRIMITIVE,
            "priority" to NULL_NUMBER_JSON_PRIMITIVE,
            "type" to JsonPrimitive(ActionType.INTENT.name),
            "isAdvanced" to NULL_BOOLEAN_JSON_PRIMITIVE,
            "isBroadcast" to NULL_BOOLEAN_JSON_PRIMITIVE,
            "intentAction" to JsonPrimitive("com.tutu.ACTION"),
            "componentName" to NULL_STRING_JSON_PRIMITIVE,
            "flags" to NULL_NUMBER_JSON_PRIMITIVE,
        ))

        ScenarioSerializer().apply {
            assertNotNull(jsonIntentNullAction.deserializeIntentActionCompat())
        }
    }
}
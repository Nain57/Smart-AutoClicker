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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType
import com.buzbuz.smartautoclicker.core.database.serialization.DeserializerFactory
import com.buzbuz.smartautoclicker.core.database.utils.toJsonArray

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Test the [CompatV20Deserializer] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CompatV20DeserializerTests {

    private companion object {
        private const val VERSION_MINIMUM = 13
        private const val VERSION_MAXIMUM = 19
        private const val SCENARIO_ID = 42L
        private const val EVENT_ID = 1L
    }

    private val deserializer = CompatV20Deserializer()

    // ======================= JSON builder helpers =======================

    private fun createJsonCompleteScenario(
        scenarioJson: JsonObject = createJsonScenario(),
        completeEventsJson: List<JsonObject> = emptyList(),
    ): JsonObject = JsonObject(
        mapOf(
            "scenario" to scenarioJson,
            "events" to completeEventsJson.toJsonArray(),
        )
    )

    private fun createJsonScenario(): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(SCENARIO_ID),
            "name" to JsonPrimitive("test"),
            "detectionQuality" to JsonPrimitive(1000),
        )
    )

    private fun createJsonCompleteEvent(
        eventJson: JsonObject = createJsonEvent(),
        conditionsJson: List<JsonObject>,
        actionsJson: List<JsonObject>,
    ): JsonObject = JsonObject(
        mapOf(
            "event" to eventJson,
            "conditions" to conditionsJson.toJsonArray(),
            "actions" to actionsJson.toJsonArray(),
        )
    )

    private fun createJsonEvent(id: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "scenarioId" to JsonPrimitive(SCENARIO_ID),
            "name" to JsonPrimitive("event"),
            "type" to JsonPrimitive(EventType.IMAGE_EVENT.name),
            "conditionOperator" to JsonPrimitive(1),
            "priority" to JsonPrimitive(0),
            "enabledOnStart" to JsonPrimitive(true),
        )
    )

    private fun createJsonCompleteAction(actionJson: JsonObject): JsonObject = JsonObject(
        mapOf(
            "action" to actionJson,
            "intentExtras" to emptyList<JsonObject>().toJsonArray(),
        )
    )

    private fun createJsonActionPause(id: Long = 10L, eventId: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("pause"),
            "priority" to JsonPrimitive(0),
            "type" to JsonPrimitive(ActionType.PAUSE.name),
            "pauseDuration" to JsonPrimitive(50L),
        )
    )

    private fun createJsonActionChangeCounter(
        id: Long = 1L,
        eventId: Long = EVENT_ID,
        counterName: String = "counter",
        operation: ChangeCounterOperationType = ChangeCounterOperationType.ADD,
        operationValueType: CounterOperationValueType = CounterOperationValueType.NUMBER,
        operationValue: Int = 1,
        operationCounterName: String? = null,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive("change_counter"))
            put("priority", JsonPrimitive(0))
            put("type", JsonPrimitive(ActionType.CHANGE_COUNTER.name))
            put("counterName", JsonPrimitive(counterName))
            put("counterOperation", JsonPrimitive(operation.name))
            put("counterOperationValueType", JsonPrimitive(operationValueType.name))
            put("counterOperationValue", JsonPrimitive(operationValue))
            if (operationCounterName != null) put("counterOperationCounterName", JsonPrimitive(operationCounterName))
        }
    )

    private fun createJsonActionNotification(
        id: Long = 2L,
        eventId: Long = EVENT_ID,
        messageType: NotificationMessageType,
        messageText: String? = null,
        messageCounterName: String? = null,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive("notification"))
            put("priority", JsonPrimitive(0))
            put("type", JsonPrimitive(ActionType.NOTIFICATION.name))
            put("notificationImportance", JsonPrimitive(3))
            put("notificationMessageType", JsonPrimitive(messageType.name))
            if (messageText != null) put("notificationMessageText", JsonPrimitive(messageText))
            if (messageCounterName != null) put("notificationMessageCounterName", JsonPrimitive(messageCounterName))
        }
    )

    private fun createJsonConditionImage(id: Long = 1L, eventId: Long = EVENT_ID): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("img_cond"),
            "type" to JsonPrimitive("ON_IMAGE_DETECTED"),
            "priority" to JsonPrimitive(0),
            "path" to JsonPrimitive("/test/image"),
            "areaLeft" to JsonPrimitive(0),
            "areaTop" to JsonPrimitive(0),
            "areaRight" to JsonPrimitive(100),
            "areaBottom" to JsonPrimitive(100),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(4),
        )
    )

    private fun createJsonConditionCounterReached(
        id: Long = 1L,
        eventId: Long = EVENT_ID,
        counterName: String = "condCounter",
        operation: CounterComparisonOperation = CounterComparisonOperation.GREATER,
        counterValue: Int = 0,
        operationCounterName: String? = null,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive("counter_cond"))
            put("type", JsonPrimitive("ON_COUNTER_REACHED"))
            put("priority", JsonPrimitive(0))
            put("counterName", JsonPrimitive(counterName))
            put("counterComparisonOperation", JsonPrimitive(operation.name))
            put("counterValue", JsonPrimitive(counterValue))
            if (operationCounterName != null) put("counterOperationCounterName", JsonPrimitive(operationCounterName))
        }
    )

    private fun createJsonConditionNumberDetected(
        id: Long = 1L,
        eventId: Long = EVENT_ID,
        operationCounterName: String? = null,
    ): JsonObject = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("eventId", JsonPrimitive(eventId))
            put("name", JsonPrimitive("num_cond"))
            put("type", JsonPrimitive("ON_NUMBER_DETECTED"))
            put("priority", JsonPrimitive(0))
            put("detectionAreaLeft", JsonPrimitive(0))
            put("detectionAreaTop", JsonPrimitive(0))
            put("detectionAreaRight", JsonPrimitive(100))
            put("detectionAreaBottom", JsonPrimitive(100))
            put("numberCounterComparisonOperation", JsonPrimitive(CounterComparisonOperation.GREATER.name))
            put("numberCounterValue", JsonPrimitive(0.0))
            if (operationCounterName != null) {
                put("numberCounterOperationValueType", JsonPrimitive(CounterOperationValueType.COUNTER.name))
                put("numberCounterOperationCounterName", JsonPrimitive(operationCounterName))
            }
        }
    )

    // ======================= Factory =======================

    @Test
    fun deserialization_factory_availability() {
        for (i in VERSION_MINIMUM..VERSION_MAXIMUM) {
            DeserializerFactory.create(i).let { d ->
                assertNotNull(d)
                assertTrue(d is CompatV20Deserializer)
            }
        }
    }

    // ======================= Counter condition value =======================

    @Test
    fun deserialize_counter_condition_value_reads_int_as_double() {
        val json = JsonObject(mapOf("counterValue" to JsonPrimitive(7)))

        val result = deserializer.deserializeCounterConditionValue(json)

        assertEquals(7.0, result, 0.0)
    }

    @Test
    fun deserialize_counter_condition_value_missing_returns_zero() {
        val json = JsonObject(emptyMap())

        val result = deserializer.deserializeCounterConditionValue(json)

        assertEquals(0.0, result, 0.0)
    }

    // ======================= Counter action value =======================

    @Test
    fun deserialize_counter_action_value_reads_int_as_double() {
        val json = JsonObject(mapOf("counterOperationValue" to JsonPrimitive(12)))

        val result = deserializer.deserializeCounterActionValue(json)

        assertEquals(12.0, result, 0.0)
    }

    @Test
    fun deserialize_counter_action_value_missing_returns_zero() {
        val json = JsonObject(emptyMap())

        val result = deserializer.deserializeCounterActionValue(json)

        assertEquals(0.0, result, 0.0)
    }

    // ======================= Notification deserialization =======================

    @Test
    fun deserialize_notification_text_type_reads_message_text_directly() {
        val expectedText = "hello from test"
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive(1L),
                "eventId" to JsonPrimitive(EVENT_ID),
                "name" to JsonPrimitive("notif"),
                "priority" to JsonPrimitive(0),
                "notificationImportance" to JsonPrimitive(3),
                "notificationMessageType" to JsonPrimitive(NotificationMessageType.TEXT.name),
                "notificationMessageText" to JsonPrimitive(expectedText),
            )
        )

        val result = deserializer.deserializeActionNotification(json)

        assertNotNull(result)
        assertEquals(expectedText, result!!.notificationMessageText)
    }

    @Test
    fun deserialize_notification_counter_value_type_builds_formatted_text() {
        val counterName = "myCounter"
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive(1L),
                "eventId" to JsonPrimitive(EVENT_ID),
                "name" to JsonPrimitive("notif"),
                "priority" to JsonPrimitive(0),
                "notificationImportance" to JsonPrimitive(3),
                "notificationMessageType" to JsonPrimitive(NotificationMessageType.COUNTER_VALUE.name),
                "notificationMessageCounterName" to JsonPrimitive(counterName),
            )
        )

        val result = deserializer.deserializeActionNotification(json)

        assertNotNull(result)
        assertEquals("$counterName = {$counterName}", result!!.notificationMessageText)
    }

    @Test
    fun deserialize_notification_missing_message_type_returns_null() {
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive(1L),
                "eventId" to JsonPrimitive(EVENT_ID),
                "name" to JsonPrimitive("notif"),
                "priority" to JsonPrimitive(0),
                "notificationImportance" to JsonPrimitive(3),
            )
        )

        val result = deserializer.deserializeActionNotification(json)

        assertNull(result)
    }

    // ======================= Counter migration (migrateToCounterTable) =======================

    @Test
    fun migrate_counter_from_change_counter_action_name() {
        // Given
        val counterName = "actionCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonActionChangeCounter(counterName = counterName)),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == counterName })
    }

    @Test
    fun migrate_counter_from_change_counter_action_operation_counter_name() {
        // Given
        val counterName = "mainCounter"
        val operationCounterName = "refCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(
                            createJsonActionChangeCounter(
                                counterName = counterName,
                                operationValueType = CounterOperationValueType.COUNTER,
                                operationCounterName = operationCounterName,
                            )
                        ),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == counterName })
        assertTrue(result.counters.any { it.name == operationCounterName })
    }

    @Test
    fun migrate_counter_from_notification_action() {
        // Given
        val counterName = "notifCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(
                            createJsonActionNotification(
                                messageType = NotificationMessageType.COUNTER_VALUE,
                                messageCounterName = counterName,
                            )
                        ),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == counterName })
    }

    @Test
    fun migrate_counter_from_counter_condition_name() {
        // Given
        val counterName = "condCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionCounterReached(counterName = counterName)),
                    actionsJson = listOf(createJsonCompleteAction(createJsonActionPause())),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == counterName })
    }

    @Test
    fun migrate_counter_from_counter_condition_operation_counter_name() {
        // Given
        val counterName = "condCounter"
        val refCounterName = "condRefCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(
                        createJsonConditionCounterReached(
                            counterName = counterName,
                            operationCounterName = refCounterName,
                        )
                    ),
                    actionsJson = listOf(createJsonCompleteAction(createJsonActionPause())),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == counterName })
        assertTrue(result.counters.any { it.name == refCounterName })
    }

    @Test
    fun migrate_counter_from_number_condition_operation_counter_name() {
        // Given
        val refCounterName = "numRefCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionNumberDetected(operationCounterName = refCounterName)),
                    actionsJson = listOf(createJsonCompleteAction(createJsonActionPause())),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.any { it.name == refCounterName })
    }

    @Test
    fun migrate_counter_deduplication() {
        // Given - same counter name referenced in action and condition
        val sharedCounterName = "sharedCounter"
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionCounterReached(counterName = sharedCounterName)),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonActionChangeCounter(counterName = sharedCounterName)),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then - the shared counter appears exactly once
        assertEquals(1, result.counters.count { it.name == sharedCounterName })
    }

    @Test
    fun migrate_counter_starting_value_is_zero() {
        // Given
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonActionChangeCounter(counterName = "counter")),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.all { it.startingValue == 0.0 })
    }

    @Test
    fun migrate_counter_scenario_id_matches_event_scenario_id() {
        // Given
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(
                createJsonCompleteEvent(
                    conditionsJson = listOf(createJsonConditionImage()),
                    actionsJson = listOf(
                        createJsonCompleteAction(createJsonActionChangeCounter(counterName = "counter")),
                    ),
                )
            )
        )

        // When
        val result = deserializer.deserializeCompleteScenario(scenarioJson)

        // Then
        assertTrue(result.counters.all { it.scenarioId == SCENARIO_ID })
    }
}

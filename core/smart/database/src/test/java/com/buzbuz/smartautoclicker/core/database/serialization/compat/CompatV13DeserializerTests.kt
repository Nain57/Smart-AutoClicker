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
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
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

/** Test the [CompatV11Deserializer] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CompatV13DeserializerTests {

    private companion object {
        private const val VERSION_MINIMUM = 11
        private const val VERSION_MAXIMUM = 12
    }

    private fun createJsonCompleteScenario(
        scenarioJson: JsonObject = createJsonScenario(),
        completeEventsJson: List<JsonObject> = emptyList(),
        endConditionJson: List<JsonObject> = emptyList(),
    ) : JsonObject = JsonObject(
        mapOf(
            "scenario" to scenarioJson,
            "events" to completeEventsJson.toJsonArray(),
            "endConditions" to endConditionJson.toJsonArray(),
        )
    )

    private fun createJsonScenario(endConditionOperator: Int = 1): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(1L),
            "name" to JsonPrimitive("toto"),
            "randomize" to JsonPrimitive(false),
            "detectionQuality" to JsonPrimitive(1000),
            "endConditionOperator" to JsonPrimitive(endConditionOperator),
        )
    )

    private fun createJsonCompleteEvent(
        eventJson: JsonObject,
        conditionsJson: List<JsonObject> = emptyList(),
        completeActionsJson: List<JsonObject> = emptyList(),
    ): JsonObject = JsonObject(
        mapOf(
            "event" to eventJson,
            "conditions" to conditionsJson.toJsonArray(),
            "actions" to completeActionsJson.toJsonArray(),
        )
    )

    private fun createJsonEvent(id: Long = 1L): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "scenarioId" to JsonPrimitive(1L),
            "name" to JsonPrimitive("tata"),
            "conditionOperator" to JsonPrimitive(1),
            "priority" to JsonPrimitive(1),
            "enabledOnStart" to JsonPrimitive(true),
        )
    )

    private fun createJsonCondition(eventId: Long = 1L): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(1),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("Toto"),
            "path" to JsonPrimitive("/path"),
            "areaLeft" to JsonPrimitive(1),
            "areaTop" to JsonPrimitive(2),
            "areaRight" to JsonPrimitive(10),
            "areaBottom" to JsonPrimitive(20),
            "shouldBeDetected" to JsonPrimitive(true),
            "detectionType" to JsonPrimitive(1),
            "threshold" to JsonPrimitive(10),
        )
    )

    private fun createJsonCompleteAction(
        actionJson: JsonObject,
        intentExtrasJson: List<JsonObject> = emptyList(),
    ): JsonObject = JsonObject(
        mapOf(
            "action" to actionJson,
            "intentExtras" to intentExtrasJson.toJsonArray(),
        )
    )

    private fun createJsonActionToggleEvent(
        id: Long = 1L,
        eventId: Long = 1L,
        name: String = "toto",
        toggleEventId: Long = 1L,
        toggleType: EventToggleType = EventToggleType.TOGGLE,
    ): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive(name),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.TOGGLE_EVENT.name),
            "toggleEventId" to JsonPrimitive(toggleEventId),
            "toggleEventType" to JsonPrimitive(toggleType.name),
        )
    )

    private fun createJsonActionPause(id: Long = 1L, eventId: Long = 1): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id),
            "eventId" to JsonPrimitive(eventId),
            "name" to JsonPrimitive("Toto"),
            "priority" to JsonPrimitive(1),
            "type" to JsonPrimitive(ActionType.PAUSE.name),
            "pauseDuration" to JsonPrimitive(50L),
        )
    )

    private fun createJsonEndCondition(eventId: Long = 1L, executions: Int = 1): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(1L),
            "scenarioId" to JsonPrimitive(1L),
            "eventId" to JsonPrimitive(eventId),
            "executions" to JsonPrimitive(executions),
        )
    )

    @Test
    fun deserialization_factory_availability() {
        for (i in VERSION_MINIMUM..VERSION_MAXIMUM) {
            DeserializerFactory.create(i).let { deserializer ->
                assertNotNull(deserializer)
                assertTrue(deserializer is CompatV13Deserializer)
            }
        }
    }

    @Test
    fun deserialization_event_type_always_image() {
        val eventType = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeEventType(JsonObject(emptyMap()))

        assertEquals(EventType.IMAGE_EVENT, eventType)
    }

    @Test
    fun deserialization_condition_type_always_image_detected() {
        val conditionType = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeConditionType(JsonObject(emptyMap()))

        assertEquals(ConditionType.ON_IMAGE_DETECTED, conditionType)
    }

    @Test
    fun migrate_toggle_event_actions_no_aggregation() {
        // Given
        val toggleName = "titi"
        val toggleEventId = 1L
        val toggleType = EventToggleType.DISABLE
        val toggleEventActionJson = createJsonCompleteAction(
            createJsonActionToggleEvent(name = toggleName, toggleEventId = toggleEventId, toggleType = toggleType),
        )
        val eventJson = createJsonCompleteEvent(
            eventJson = createJsonEvent(),
            completeActionsJson = listOf(toggleEventActionJson),
            conditionsJson = listOf(createJsonCondition()),
        )
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(eventJson)
        )

        // When
        val completeScenario = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeCompleteScenario(scenarioJson)

        // Then
        assertNotNull(completeScenario)
        completeScenario.events.first().actions.let { actions ->

            assertEquals(1, actions.count())
            val action = actions.first().action
            assertEquals(toggleName, action.name)
            assertTrue(action.toggleAll == false)
            assertNull(action.toggleAllType)

            assertEquals(1, actions.first().eventsToggle.count())
            actions.first().eventsToggle.first().let { eventToggleEntity ->
                assertEquals(action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId, eventToggleEntity.toggleEventId)
                assertEquals(toggleType, eventToggleEntity.type)
            }
        }
    }

    @Test
    fun migrate_toggle_event_actions_aggregation() {
        // Given
        val toggleEventId1 = 1L
        val toggleEventId2 = 2L
        val toggleType1 = EventToggleType.DISABLE
        val toggleType2 = EventToggleType.ENABLE
        val toggleEventActionJson1 = createJsonCompleteAction(
            createJsonActionToggleEvent(id = 1L, toggleEventId = toggleEventId1, toggleType = toggleType1),
        )
        val toggleEventActionJson2 = createJsonCompleteAction(
            createJsonActionToggleEvent(id = 2L, toggleEventId = toggleEventId2, toggleType = toggleType2),
        )
        val eventJson = createJsonCompleteEvent(
            eventJson = createJsonEvent(),
            completeActionsJson = listOf(toggleEventActionJson1, toggleEventActionJson2),
            conditionsJson = listOf(createJsonCondition()),
        )
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(eventJson)
        )

        // When
        val completeScenario = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeCompleteScenario(scenarioJson)

        // Then
        assertNotNull(completeScenario)
        completeScenario.events.first().actions.let { actions ->

            assertEquals(1, actions.count())
            val action = actions.first().action
            assertTrue(action.toggleAll == false)
            assertNull(action.toggleAllType)

            assertEquals(2, actions.first().eventsToggle.count())
            actions.first().eventsToggle[0].let { eventToggleEntity ->
                assertEquals(action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId1, eventToggleEntity.toggleEventId)
                assertEquals(toggleType1, eventToggleEntity.type)
            }
            actions.first().eventsToggle[1].let { eventToggleEntity ->
                assertEquals(action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId2, eventToggleEntity.toggleEventId)
                assertEquals(toggleType2, eventToggleEntity.type)
            }
        }
    }

    @Test
    fun migrate_toggle_event_actions_aggregation_multi_event() {
        // Given
        val eventId1 = 1L
        val eventId2 = 2L
        val toggleEventId1 = 1L
        val toggleEventId2 = 2L
        val toggleEventId3 = 3L
        val toggleType1 = EventToggleType.DISABLE
        val toggleType2 = EventToggleType.ENABLE
        val toggleType3 = EventToggleType.TOGGLE
        val toggleEventActionJson1 = createJsonCompleteAction(
            createJsonActionToggleEvent(id = 1L, eventId = eventId1, toggleEventId = toggleEventId1, toggleType = toggleType1),
        )
        val toggleEventActionJson2 = createJsonCompleteAction(
            createJsonActionToggleEvent(id = 2L, eventId = eventId1, toggleEventId = toggleEventId2, toggleType = toggleType2),
        )
        val toggleEventActionJson3 = createJsonCompleteAction(
            createJsonActionToggleEvent(id = 3L, eventId = eventId2, toggleEventId = toggleEventId3, toggleType = toggleType3),
        )
        val eventJson1 = createJsonCompleteEvent(
            eventJson = createJsonEvent(eventId1),
            completeActionsJson = listOf(toggleEventActionJson1, toggleEventActionJson2),
            conditionsJson = listOf(createJsonCondition()),
        )
        val eventJson2 = createJsonCompleteEvent(
            eventJson = createJsonEvent(eventId2),
            completeActionsJson = listOf(toggleEventActionJson3),
            conditionsJson = listOf(createJsonCondition()),
        )
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(eventJson1, eventJson2)
        )

        // When
        val completeScenario = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeCompleteScenario(scenarioJson)

        // Then
        assertNotNull(completeScenario)
        completeScenario.events[0].actions.let { actions ->

            assertEquals(1, actions.count())

            val action1 = actions.first()
            assertTrue(action1.action.toggleAll == false)
            assertNull(action1.action.toggleAllType)

            assertEquals(2, action1.eventsToggle.count())
            action1.eventsToggle[0].let { eventToggleEntity ->
                assertEquals(action1.action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId1, eventToggleEntity.toggleEventId)
                assertEquals(toggleType1, eventToggleEntity.type)
            }
            action1.eventsToggle[1].let { eventToggleEntity ->
                assertEquals(action1.action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId2, eventToggleEntity.toggleEventId)
                assertEquals(toggleType2, eventToggleEntity.type)
            }
        }

        completeScenario.events[1].actions.let { actions ->
            val action2 = actions[0]
            assertTrue(action2.action.toggleAll == false)
            assertNull(action2.action.toggleAllType)

            assertEquals(1, action2.eventsToggle.count())
            action2.eventsToggle[0].let { eventToggleEntity ->
                assertEquals(action2.action.id, eventToggleEntity.actionId)
                assertEquals(toggleEventId3, eventToggleEntity.toggleEventId)
                assertEquals(toggleType3, eventToggleEntity.type)
            }
        }
    }

    @Test
    fun migrate_end_condition() {
        // Given
        val eventId = 1L
        val executions = 10
        val eventJson = createJsonCompleteEvent(
            eventJson = createJsonEvent(id = eventId),
            completeActionsJson = listOf(createJsonCompleteAction(createJsonActionPause(eventId = eventId))),
            conditionsJson = listOf(createJsonCondition(eventId = eventId)),
        )
        val endConditionJson = createJsonEndCondition(eventId = eventId, executions = executions)
        val scenarioJson = createJsonCompleteScenario(
            completeEventsJson = listOf(eventJson),
            endConditionJson = listOf(endConditionJson)
        )

        // When
        val completeScenario = (DeserializerFactory.create(VERSION_MAXIMUM) as CompatV13Deserializer)
            .deserializeCompleteScenario(scenarioJson)

        assertNotNull(completeScenario)
        assertEquals(2, completeScenario.events.count())

        val stopEvent = completeScenario.events.find { it.event.type == EventType.TRIGGER_EVENT }
        assertNotNull(stopEvent)
        assertEquals(1, stopEvent!!.conditions.count())
        assertEquals(1, stopEvent.actions.count())

        val stopCondition = stopEvent.conditions.first()
        assertEquals(ConditionType.ON_COUNTER_REACHED, stopCondition.type)
        assertEquals(executions, stopCondition.counterValue)
        assertEquals(CounterComparisonOperation.GREATER_OR_EQUALS, stopCondition.counterComparisonOperation)

        val stopAction = stopEvent.actions.first()
        assertEquals(ActionType.TOGGLE_EVENT, stopAction.action.type)
        assertTrue(stopAction.action.toggleAll == true)
        assertEquals(EventToggleType.DISABLE, stopAction.action.toggleAllType)

        val monitoredEvent = completeScenario.events.find { it.event.id == eventId }
        assertNotNull(monitoredEvent)
        val counterAction = monitoredEvent!!.actions.find { it.action.type == ActionType.CHANGE_COUNTER }
        assertNotNull(counterAction)
        assertEquals(ChangeCounterOperationType.ADD, counterAction!!.action.counterOperation)
        assertEquals(1, counterAction.action.counterOperationValue)
        assertEquals(stopCondition.counterName, counterAction.action.counterName)
    }
}
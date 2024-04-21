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

import com.buzbuz.smartautoclicker.core.base.extensions.getEnum
import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getJsonArray
import com.buzbuz.smartautoclicker.core.base.extensions.getLong
import com.buzbuz.smartautoclicker.core.base.extensions.getString
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.database.entity.EventType
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Deserializer for all Json object version below 13. */
internal open class CompatV13Deserializer : CompatDeserializer() {

    private companion object {
        /** Operators lower bound on compat deserialization. */
        const val END_CONDITION_OPERATOR_LOWER_BOUND = 1
        /** Operators upper bound on compat deserialization. */
        const val END_CONDITION_OPERATOR_UPPER_BOUND = 2
        /** Operators default value on compat deserialization. */
        const val END_CONDITION_OPERATOR_DEFAULT_VALUE = END_CONDITION_OPERATOR_LOWER_BOUND
        /** End condition executions default value on compat deserialization. */
        const val END_CONDITION_EXECUTION_DEFAULT_VALUE = 1
    }

    private data class LegacyEndCondition(
        val id: Long,
        val scenarioId: Long,
        val eventId: Long,
        val executions: Int,
    )

    private val legacyEndConditions: MutableList<LegacyEndCondition> =
        mutableListOf()
    private var legacyEndConditionOperator: Int = 0

    /** Key is the event id, Value the aggregator action id. */
    private val toggleEventAggregation = mutableMapOf<Long, Long>()
    private val newEventToggles = mutableListOf<EventToggleEntity>()

    private var currentTemporaryId: Long = -1

    override fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario {
        legacyEndConditions.clear()
        legacyEndConditionOperator = 0
        toggleEventAggregation.clear()
        newEventToggles.clear()
        currentTemporaryId = -1

        return super.deserializeCompleteScenario(jsonCompleteScenario)
            .migrateToEventToggle()
            .migrateLegacyEndConditions(jsonCompleteScenario)
    }

    override fun deserializeScenario(jsonScenario: JsonObject): ScenarioEntity? {
        return super.deserializeScenario(jsonScenario)?.also {
            legacyEndConditionOperator = jsonScenario.getInt("endConditionOperator")
                ?.coerceIn(END_CONDITION_OPERATOR_LOWER_BOUND, END_CONDITION_OPERATOR_UPPER_BOUND)
                ?: END_CONDITION_OPERATOR_DEFAULT_VALUE
        }
    }

    /** Before version 13, all events are image events */
    override fun deserializeEventType(jsonEvent: JsonObject): EventType =
        EventType.IMAGE_EVENT

    /** Before version 13, all condition are image detected conditions */
    override fun deserializeConditionType(jsonCondition: JsonObject): ConditionType =
        ConditionType.ON_IMAGE_DETECTED

    override fun deserializeActionToggleEvent(jsonToggleEvent: JsonObject): ActionEntity? {
        val id = jsonToggleEvent.getLong("id", true) ?: return null
        val eventId = jsonToggleEvent.getLong("eventId", true) ?: return null
        val toggleEventId = jsonToggleEvent.getLong("toggleEventId") ?: return null
        val toggleType = jsonToggleEvent.getEnum<EventToggleType>("toggleEventType") ?: return null

        // If that's the first action from the event, use it as aggregator
        // Otherwise, use the one available
        val currentAggregatorId = toggleEventAggregation.getOrPut(eventId) { id }

        // For each toggle action, creates it's counterpart as a EventToggle referencing the aggregator
        newEventToggles.add(
            EventToggleEntity(
                id = getNextId(),
                actionId = currentAggregatorId,
                toggleEventId = toggleEventId,
                type = toggleType,
            )
        )

        // If this is not the aggregator, it is now aggregated and useless
        if (id != currentAggregatorId) return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonToggleEvent.getString("name") ?: "",
            priority = jsonToggleEvent.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.TOGGLE_EVENT,
            toggleAll = false,
            toggleAllType = null,
        )
    }

    private fun CompleteScenario.migrateToEventToggle(): CompleteScenario =
        CompleteScenario(
            scenario = scenario,
            events = events.map { completeEventEntity ->
                val eventAggregatorActionId = toggleEventAggregation[completeEventEntity.event.id]
                    ?: return@map completeEventEntity

                val aggregatorCompleteAction = completeEventEntity.actions.find { completeActionEntity ->
                    eventAggregatorActionId == completeActionEntity.action.id
                } ?: return@map completeEventEntity

                val toggleEvents = newEventToggles.filter { eventToggleEntity ->
                    eventToggleEntity.actionId == eventAggregatorActionId
                }
                if (toggleEvents.isEmpty()) return@map completeEventEntity

                CompleteEventEntity(
                    event = completeEventEntity.event,
                    conditions = completeEventEntity.conditions,
                    actions = completeEventEntity.actions.map { completeActionEntity ->
                        if (completeActionEntity.action.id == eventAggregatorActionId) {
                            CompleteActionEntity(
                                action = aggregatorCompleteAction.action,
                                intentExtras = emptyList(),
                                eventsToggle = toggleEvents,
                            )
                        } else completeActionEntity
                    }
                )
            }
        )

    private fun CompleteScenario.migrateLegacyEndConditions(jsonCompleteScenario: JsonObject): CompleteScenario {
        val endConditions = jsonCompleteScenario.getJsonArray("endConditions")?.let { jsonEndConditions ->
            deserializeLegacyEndConditions(jsonEndConditions)
        }
        if (endConditions.isNullOrEmpty()) return this

        // Create the trigger event replacing the end condition
        val stopTriggerEventConditions = mutableListOf<ConditionEntity>()
        val stopTriggerEventId = getNextId()
        val stopTriggerEvent = EventEntity(
            id = stopTriggerEventId,
            scenarioId = scenario.id,
            name = "Stop scenario",
            type = EventType.TRIGGER_EVENT,
            conditionOperator = legacyEndConditionOperator,
            priority = -1,
            enabledOnStart = true,
        )

        // Create an action in the trigger event that stop the scenario
        val stopScenarioAction = ActionEntity(
            id = getNextId(),
            eventId = stopTriggerEventId,
            name = "Stop scenario",
            priority = 0,
            type = ActionType.TOGGLE_EVENT,
            toggleAll = true,
            toggleAllType = EventToggleType.DISABLE,
        )

        val execCountActions = mutableMapOf<Long, ActionEntity>()
        endConditions.forEach { legacyEndCondition ->
            val endConditionCounterName = "Stop Scenario ${legacyEndCondition.eventId}"

            // Put a new stop condition with a unique trigger name in the trigger event
            stopTriggerEventConditions.add(
                ConditionEntity(
                    id = getNextId(),
                    eventId = stopTriggerEventId,
                    name = endConditionCounterName,
                    type = ConditionType.ON_COUNTER_REACHED,
                    counterName = endConditionCounterName,
                    counterComparisonOperation = CounterComparisonOperation.GREATER_OR_EQUALS,
                    counterValue = legacyEndCondition.executions,
                )
            )

            // Add a new count action to the referenced event by the legacy end condition
            execCountActions[legacyEndCondition.eventId] = ActionEntity(
                id = getNextId(),
                eventId = legacyEndCondition.eventId,
                name = "Execution count",
                priority = 10000,
                type = ActionType.CHANGE_COUNTER,
                counterName = endConditionCounterName,
                counterOperation = ChangeCounterOperationType.ADD,
                counterOperationValue = 1,
            )
        }

        // Insert all new entities in the event list
        val newEventList = buildList {
            add(CompleteEventEntity(
                event = stopTriggerEvent,
                conditions = stopTriggerEventConditions,
                actions = listOf(CompleteActionEntity(stopScenarioAction, emptyList(), emptyList())),
            ))

            addAll(
                events.map { completeEvent ->
                    execCountActions[completeEvent.event.id]?.let { execCountAction ->
                        CompleteEventEntity(
                            event = completeEvent.event,
                            conditions = completeEvent.conditions,
                            actions = completeEvent.actions.toMutableList().apply {
                                add(CompleteActionEntity(execCountAction, emptyList(), emptyList()))
                            }
                        )
                    } ?: completeEvent
                }
            )
        }

        return CompleteScenario(
            scenario = scenario,
            events = newEventList,
        )
    }

    /** @return the deserialized end condition list. */
    private fun deserializeLegacyEndConditions(jsonEndConditions: JsonArray): List<LegacyEndCondition> =
        jsonEndConditions.mapNotNull { jsonEndCondition ->
            val id = jsonEndCondition.jsonObject.getLong("id", true) ?: return@mapNotNull null
            val scenarioId = jsonEndCondition.jsonObject.getLong("scenarioId", true) ?: return@mapNotNull null
            val eventId = jsonEndCondition.jsonObject.getLong("eventId", true) ?: return@mapNotNull null

            LegacyEndCondition(
                id = id,
                scenarioId = scenarioId,
                eventId = eventId,
                executions = jsonEndCondition.jsonObject.getInt("executions")
                    ?: END_CONDITION_EXECUTION_DEFAULT_VALUE,
            )
        }

    private fun getNextId(): Long = --currentTemporaryId


    // ===== The following structures were not possible yet:
    override fun deserializeConditionBroadcastReceived(jsonCondition: JsonObject): ConditionEntity? = null
    override fun deserializeConditionCounterReached(jsonCondition: JsonObject): ConditionEntity? = null
    override fun deserializeConditionTimerReached(jsonCondition: JsonObject): ConditionEntity? = null
    override fun deserializeActionChangeCounter(jsonChangeCounter: JsonObject): ActionEntity? = null
}


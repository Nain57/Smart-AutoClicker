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
package com.buzbuz.smartautoclicker.domain

import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.EventEntity

/**
 * Event of a scenario.
 *
 * @param id the unique identifier for the event. Use 0 for creating a new scenario. Default value is 0.
 * @param scenarioId the identifier of the scenario for this event.
 * @param name the name of the event.
 * @param conditionOperator the operator to apply between the conditions in the [conditions] list.
 * @param priority the execution priority of the event in the scenario.
 * @param actions the list of action to execute when the conditions have been fulfilled
 * @param conditions the list of conditions to fulfill to execute the [actions].
 * @param stopAfter the amount of executions of this click before stopping the scenario.
 */
data class Event(
    var id: Long = 0,
    var scenarioId: Long,
    var name: String,
    @ConditionOperator var conditionOperator: Int,
    var priority: Int,
    val actions: MutableList<Action>? = null,
    val conditions: MutableList<Condition>? = null,
    var stopAfter: Int? = null,
) {

    /** @return the entity equivalent of this event. */
    internal fun toEntity() = EventEntity(id, scenarioId, name, conditionOperator, priority, stopAfter)

    /** @return the complete entity equivalent of this event. Return null if the actions or conditions are null. */
    internal fun toCompleteEntity(): CompleteEventEntity? {
        if (actions == null || conditions == null) {
            return null
        }

        return CompleteEventEntity(
            event = toEntity(),
            actions = actions.map { it.toEntity() },
            conditions = conditions.map { it.toEntity() }
        )
    }

    /** Cleanup all ids contained in this event. Ideal for copying. */
    fun cleanUpIds() {
        id = 0
        actions?.forEach { it.cleanUpIds() }
        conditions?.forEach { it.cleanUpIds() }
    }

    /** @return creates a deep copy of this complete event and of its content. */
    fun deepCopy(): Event {
        val actionsCopy = actions?.let { actionList ->
            actionList.map { it.deepCopy() }
        }

        val conditionsCopy = conditions?.let { conditionList ->
            conditionList.map { it }
        }

        return copy(
            name = "" + name,
            actions = actionsCopy?.toMutableList(),
            conditions = conditionsCopy?.toMutableList(),
        )
    }
}

/** @return the event for this entity. */
internal fun EventEntity.toEvent() = Event(id, scenarioId, name, conditionOperator, priority, stopAfter = stopAfter)

/** @return the complete event for this entity. */
internal fun CompleteEventEntity.toEvent() = Event(
    id = event.id,
    scenarioId = event.scenarioId,
    name= event.name,
    conditionOperator = event.conditionOperator,
    priority = event.priority,
    stopAfter = event.stopAfter,
    actions = actions.sortedBy { it.action.priority }.map { it.toAction() }.toMutableList(),
    conditions = conditions.map { it.toCondition() }.toMutableList(),
)

/** Defines the operators to be applied between the conditions. */
@IntDef(AND, OR)
@Retention(AnnotationRetention.SOURCE)
annotation class ConditionOperator
/** All conditions must be fulfilled. */
const val AND = 1
/** Only one of the conditions must be fulfilled. */
const val OR = 2

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
package com.buzbuz.smartautoclicker.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.buzbuz.smartautoclicker.database.room.entity.*
import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EventEntity

import kotlinx.coroutines.flow.Flow

/** Allows to access and edit the events in the database. */
@Dao
abstract class EventDao {

    /**
     * Get number of events in the database.
     *
     * @return the flow on the number of events.
     */
    @Query("SELECT COUNT(*) FROM event_table")
    abstract fun getEventsCount(): Flow<Int>

    /**
     * Get number of actions in the database.
     *
     * @return the flow on the number of actions.
     */
    @Query("SELECT COUNT(*) FROM action_table")
    abstract fun getActionsCount(): Flow<Int>

    /**
     * Get the list of events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events.
     */
    @Query("SELECT * FROM event_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract fun getEvents(scenarioId: Long): Flow<List<EventEntity>>

    /**
     * Get the list of complete events for a scenario ordered by priority.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of complete events.
     */
    @Transaction
    @Query("SELECT * FROM event_table WHERE scenario_id=:scenarioId ORDER BY priority")
    abstract fun getCompleteEvents(scenarioId: Long): Flow<List<CompleteEventEntity>>

    /**
     * Get the list of event identifier for a given scenario.
     *
     * @param scenarioId the identifier of the scenario to get the events from.
     * @return the flow on the list of events id.
     */
    @Transaction
    @Query("SELECT id FROM event_table WHERE scenario_id=:scenarioId")
    abstract suspend fun getEventsIds(scenarioId: Long): List<Long>

    /**
     * Get the complete event.
     *
     * @param eventId the identifier of the event.
     * @return the flow on the event.
     */
    @Transaction
    @Query("SELECT * FROM event_table WHERE id=:eventId")
    abstract suspend fun getEvent(eventId: Long): CompleteEventEntity

    /**
     * Get all events from all scenarios.
     *
     * @return the list containing all events.
     */
    @Transaction
    @Query("SELECT * FROM event_table ORDER BY name")
    abstract fun getAllEvents(): Flow<List<CompleteEventEntity>>

    /**
     * Get all actions from all events.
     *
     * @return the list containing all actions.
     */
    @Transaction
    @Query("SELECT * FROM action_table ORDER BY name")
    abstract fun getAllActions(): Flow<List<CompleteActionEntity>>

    /**
     * Update the provided events in the database.
     * Priorities will be updated.
     *
     * @param events the list events to update.
     */
    suspend fun updateEventList(events: List<EventEntity>) {
        events.forEachIndexed { index, event ->
            event.priority = index
        }

        updateEvents(events)
    }

    /**
     * Add an event to the database.
     * This will save in the database the event and its actions and conditions. This will set automatically the event id
     * fields in all substructure and ensure action priority correctness.
     *
     * @param event the complete event to be added.
     *
     * @return the new event identifier.
     */
    @Transaction
    open suspend fun addCompleteEvent(event: CompleteEventEntity): Long {
        val eventId = addEvent(event.event)

        event.actions.forEachIndexed { index, action ->
            action.action.id = 0
            action.action.eventId = eventId
            action.action.priority = index
        }
        val actionIds = addActions(event.actions.map { it.action })

        event.actions.forEachIndexed { index, completeAction ->
            completeAction.intentExtras.forEach { intentExtra ->
                intentExtra.actionId = actionIds[index]
            }
            addIntentExtras(completeAction.intentExtras)
        }

        event.conditions.forEach {
            it.id = 0
            it.eventId = eventId
        }
        addConditions(event.conditions)

        return eventId
    }

    /**
     * Update an event in the database.
     *
     * This will update all events values and all of it's conditions and actions. If new actions/conditions have been
     * added, they will be added to the database. If actions/conditions have been removed, they will be removed from the
     * database.
     *
     * @param completeEvent the event to update.
     *
     * @return the list of removed conditions.
     */
    @Transaction
    open suspend fun updateCompleteEvent(completeEvent: CompleteEventEntity): List<ConditionEntity> {
        updateEvent(completeEvent.event)
        updateActions(completeEvent.event.id, completeEvent.actions)
        return updateConditions(completeEvent.event.id, completeEvent.conditions)
    }

    /**
     * Update the actions for an event in the database.
     *
     * This will update all actions values and their associated intent extras, if any. If new actions have
     * been added, they will be added to the database. If actions have been removed, they will be removed from the
     * database.
     *
     * @param eventId the unique identifier for the event associated to the actions.
     * @param completeActions the new list of actions for the provided event.
     */
    private suspend fun updateActions(eventId: Long, completeActions: List<CompleteActionEntity>) {
        // Update actions in db
        ActionsUpdater.let { actionsUpdater ->
            // Update action priorities
            completeActions.forEachIndexed { index, actionEntity ->
                actionEntity.action.priority = index
            }

            // Refresh updater
            actionsUpdater.refreshUpdateValues(
                oldList = getActions(eventId),
                newList = completeActions.map { it.action }
            )

            // Ensure correctness of the event id for add actions
            actionsUpdater.toBeAdded.forEach { action ->
                action.eventId = eventId
            }

            // Add actions and update entity with db ids
            val addedActionsId = addActions(actionsUpdater.toBeAdded)
            addedActionsId.forEachIndexed { index, actionId ->
                actionsUpdater.toBeAdded[index].id = actionId
            }
            updateActions(actionsUpdater.toBeUpdated)
            deleteActions(actionsUpdater.toBeRemoved)

            completeActions.forEach { completeAction ->
                updateIntentExtras(completeAction.action.id, completeAction.intentExtras)
            }
        }
    }

    /**
     * Update the intent extras for an action in the database.
     *
     * This will update all intent extras, if any. If new intent extras have been added/removed, they will be
     * added/removed from the database.
     *
     * @param actionId the unique identifier for the action associated to the intent extras.
     * @param extras the new list of intent extras for the provided action.
     */
    private suspend fun updateIntentExtras(actionId: Long, extras: List<IntentExtraEntity>) {
        // Update intent extras for this action in db
        IntentExtrasUpdater.let { extraUpdater ->
            extraUpdater.refreshUpdateValues(
                oldList = getIntentExtras(actionId),
                newList = extras
            )

            // Insert the action id for add actions
            extraUpdater.toBeAdded.forEach { extra ->
                extra.actionId = actionId
            }
            addIntentExtras(extraUpdater.toBeAdded)
            updateIntentExtras(extraUpdater.toBeUpdated)
            deleteIntentExtras(extraUpdater.toBeRemoved)
        }
    }

    /**
     * Update the conditions for an event in the database.
     *
     * This will update all conditions, if any. If new conditions have been added/removed, they will be added/removed
     * from the database.
     *
     * @param eventId the unique identifier for the event associated to the conditions.
     * @param conditions the new list of conditions for the provided event.
     *
     * @return the list of removed conditions.
     */
    private suspend fun updateConditions(eventId: Long, conditions: List<ConditionEntity>): List<ConditionEntity> {
        ConditionsUpdater.let { conditionsUpdater ->
            conditionsUpdater.refreshUpdateValues(
                oldList = getConditions(eventId),
                newList = conditions
            )
            conditionsUpdater.toBeAdded.forEach { condition ->
                condition.eventId = eventId
            }
            addConditions(conditionsUpdater.toBeAdded)
            updateConditions(conditionsUpdater.toBeUpdated)
            deleteConditions(conditionsUpdater.toBeRemoved)
        }

        return ConditionsUpdater.toBeRemoved
    }

    /**
     * Delete an event from the database.
     * Actions and conditions of this event will be deleted as well due to the CASCADE action on event deletion.
     *
     * @param event the conditions to be added.
     */
    @Delete
    abstract suspend fun deleteEvent(event: EventEntity)

    /**
     * Get the list actions for an event, ordered by priority.
     *
     * @param eventId the identifier of the event to get the actions from.
     */
    @Query("SELECT * FROM action_table WHERE eventId=:eventId ORDER BY priority")
    abstract suspend fun getActions(eventId: Long): List<ActionEntity>

    /**
     * Get the list of conditions for a given event.
     *
     * @param eventId the identifier of the event to get the conditions from.
     * @return the list of conditions for the event.
     */
    @Query("SELECT * FROM condition_table WHERE eventId=:eventId ORDER BY id")
    abstract suspend fun getConditions(eventId: Long): List<ConditionEntity>

    /**
     * Get the list of intent extras for a given action.
     *
     * @param actionId the identifier of the action to get the intent extras from.
     * @return the list of intent extras for the action.
     */
    @Query("SELECT * FROM intent_extra_table WHERE action_id=:actionId ORDER BY id")
    abstract suspend fun getIntentExtras(actionId: Long): List<IntentExtraEntity>

    /**
     * Add an event to the database.
     * @param event the event to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addEvent(event: EventEntity): Long
    /**
     * Add a list of actions to the database.
     * @param actions the actions to be added.
     * @return the list of inserted ids.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addActions(actions: List<ActionEntity>): List<Long>
    /**
     * Add a list of intent extras to the database.
     * @param intentExtras the list of intent extras to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addIntentExtras(intentExtras: List<IntentExtraEntity>)
    /**
     * Add a list of conditions to the database.
     * @param conditions the conditions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun addConditions(conditions: List<ConditionEntity>)

    /**
     * Update an event in the database.
     * @param event the event to be updated.
     */
    @Update
    protected abstract suspend fun updateEvent(event: EventEntity)
    /**
     * Update a list of events in the database.
     * @param events the list of events to be updated.
     */
    @Update
    protected abstract suspend fun updateEvents(events: List<EventEntity>)
    /**
     * Update a list of actions in the database.
     * @param actions the list of actions to be updated.
     */
    @Update
    protected abstract suspend fun updateActions(actions: List<ActionEntity>)
    /**
     * Update a list of intent extras in the database.
     * @param extras the list of intent extras to be updated.
     */
    @Update
    protected abstract suspend fun updateIntentExtras(extras: List<IntentExtraEntity>)
    /**
     * Update a list of conditions in the database.
     * @param conditions the list of conditions to be updated.
     */
    @Update
    protected abstract suspend fun updateConditions(conditions: List<ConditionEntity>)

    /**
     * Delete a list of actions in the database.
     * @param actions the conditions to be removed.
     */
    @Delete
    protected abstract suspend fun deleteActions(actions: List<ActionEntity>)
    /**
     * Delete a list of intent extras in the database.
     * @param extras the intent extras to be removed.
     */
    @Delete
    protected abstract suspend fun deleteIntentExtras(extras: List<IntentExtraEntity>)
    /**
     * Delete a list of conditions in the database.
     * @param conditions the conditions to be removed.
     */
    @Delete
    protected abstract suspend fun deleteConditions(conditions: List<ConditionEntity>)
}
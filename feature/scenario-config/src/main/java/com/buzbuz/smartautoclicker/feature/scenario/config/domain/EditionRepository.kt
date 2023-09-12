/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class EditionRepository private constructor(context: Context) {

    companion object {

        /** Tag for logs */
        private const val TAG = "EditionRepository"
        /** Singleton preventing multiple instances of the EditionRepository at the same time. */
        @Volatile
        private var INSTANCE: EditionRepository? = null

        /**
         * Get the EditionRepository singleton, or instantiates it if it wasn't yet.
         * @param context the Android context.
         * @return the EditionRepository singleton.
         */
        fun getInstance(context: Context): EditionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EditionRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /** The repository providing access to the database. */
    private val repository: Repository = Repository.getRepository(context)
    /** Keep tracks of all changes in the currently edited scenario. */
    private val scenarioEditor: ScenarioEditor = ScenarioEditor()

    /** Provides creators for all elements in an edited scenario. */
    val editedItemsBuilder: EditedItemsBuilder = EditedItemsBuilder(context, scenarioEditor)
    /** Provides the states of all elements in the edited scenario. */
    val editionState: EditionState = EditionState(context, scenarioEditor)
    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = scenarioEditor.editedScenario.map { it == null }

    /** Tells if the user is currently editing a scenario. */
    val isEditingScenario: Flow<Boolean> = scenarioEditor.editedScenario
        .map { it?.id != null }
    /** Tells if the user is currently editing an event. */
    val isEditingEvent: Flow<Boolean> = scenarioEditor.eventsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing a condition. */
    val isEditingCondition: Flow<Boolean> = scenarioEditor.eventsEditor.conditionsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing an action. */
    val isEditingAction: Flow<Boolean> = scenarioEditor.eventsEditor.actionsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing an end condition. */
    val isEditingEndCondition: Flow<Boolean> = scenarioEditor.endConditionsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing an Intent Extra. */
    val isEditingIntentExtra: Flow<Boolean> = scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.editedItem
        .map { it?.id != null }

    // --- SCENARIO - START ---

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long): Boolean {
        val scenario = repository.getScenario(scenarioId) ?: run {
            Log.e(TAG, "Can't start edition, scenario $scenarioId not found")
            return false
        }

        Log.d(TAG, "Start edition of scenario $scenarioId")

        scenarioEditor.startEdition(
            scenario = scenario,
            events = repository.getEvents(scenarioId),
            endConditions = repository.getEndConditions(scenarioId),
        )
        return true
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions(): Boolean {
        Log.d(TAG, "Save editions")

        val updateResult = repository.updateScenario(
            scenario = scenarioEditor.editedScenario.value ?: return false,
            events = scenarioEditor.eventsEditor.editedList.value ?: return false,
            endConditions = scenarioEditor.endConditionsEditor.editedList.value ?: return false,
        )

        // In case of error, do not stop the edition
        if (!updateResult) return false

        scenarioEditor.stopEdition()
        editedItemsBuilder.resetGeneratedIdsCount()

        return true
    }

    /** Update the currently edited scenario. */
    fun updateEditedScenario(scenario: Scenario): Unit = scenarioEditor.updateEditedScenario(scenario)
    /** Update the priority of the events in the scenario. */
    fun updateEventsOrder(newEvents: List<Event>): Unit = scenarioEditor.eventsEditor.updateList(newEvents)


    // --- EVENT - START ---

    fun startEventEdition(event: Event): Unit =
        scenarioEditor.eventsEditor.startItemEdition(event)
    fun updateEditedEvent(event: Event): Unit =
        scenarioEditor.eventsEditor.updateEditedItem(event)
    fun updateActionsOrder(actions: List<Action>): Unit =
        scenarioEditor.eventsEditor.actionsEditor.updateList(actions)
    fun upsertEditedEvent(): Unit =
        scenarioEditor.eventsEditor.upsertEditedItem()
    fun deleteEditedEvent(): Unit =
        scenarioEditor.eventsEditor.deleteEditedItem()


    // --- CONDITION - START ---

    fun startConditionEdition(condition: Condition): Unit =
        scenarioEditor.eventsEditor.conditionsEditor.startItemEdition(condition)
    fun updateEditedCondition(condition: Condition): Unit =
        scenarioEditor.eventsEditor.conditionsEditor.updateEditedItem(condition)
    fun upsertEditedCondition(): Unit =
        scenarioEditor.eventsEditor.conditionsEditor.upsertEditedItem()
    fun deleteEditedCondition(): Unit =
        scenarioEditor.eventsEditor.conditionsEditor.deleteEditedItem()
    fun stopConditionEdition(): Unit =
        scenarioEditor.eventsEditor.conditionsEditor.stopItemEdition()

    // --- CONDITION - END ---


    // --- ACTION - START ---

    fun startActionEdition(action: Action): Unit =
        scenarioEditor.eventsEditor.actionsEditor.startItemEdition(action)
    fun updateEditedAction(action: Action): Unit =
        scenarioEditor.eventsEditor.actionsEditor.updateEditedItem(action)
    fun upsertEditedAction(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.upsertEditedItem()
    fun deleteEditedAction(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.deleteEditedItem()


    // --- INTENT EXTRA - START ---

    fun startIntentExtraEdition(extra: IntentExtra<out Any>): Unit =
        scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.startItemEdition(extra)
    fun updateEditedIntentExtra(extra: IntentExtra<out Any>): Unit =
        scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.updateEditedItem(extra)
    fun upsertEditedIntentExtra(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.upsertEditedItem()
    fun deleteEditedIntentExtra(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.deleteEditedItem()
    fun stopIntentExtraEdition(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.intentExtraEditor.stopItemEdition()

    // --- INTENT EXTRA - END ---

    fun stopActionEdition(): Unit =
        scenarioEditor.eventsEditor.actionsEditor.stopItemEdition()

    // --- ACTION - END ---

    fun stopEventEdition(): Unit =
        scenarioEditor.eventsEditor.stopItemEdition()

    // --- EVENT - END ---

    // --- END CONDITION - START ---

    fun startEndConditionEdition(endCondition: EndCondition): Unit =
        scenarioEditor.endConditionsEditor.startItemEdition(endCondition)
    fun updateEditedEndCondition(endCondition: EndCondition): Unit =
        scenarioEditor.endConditionsEditor.updateEditedItem(endCondition)
    fun upsertEditedEndCondition(): Unit =
        scenarioEditor.endConditionsEditor.upsertEditedItem()
    fun deleteEditedEndCondition(): Unit =
        scenarioEditor.endConditionsEditor.deleteEditedItem()
    fun stopEndConditionEdition(): Unit =
        scenarioEditor.endConditionsEditor.stopItemEdition()

    // --- END CONDITION - END ---

    /** Cancel all changes made during the edition. */
    fun stopEdition() {
        Log.d(TAG, "Stop edition")
        scenarioEditor.stopEdition()
    }

    // --- SCENARIO - END ---
}
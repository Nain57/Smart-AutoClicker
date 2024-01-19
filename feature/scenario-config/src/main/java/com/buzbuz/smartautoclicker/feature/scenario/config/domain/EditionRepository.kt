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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.model.IEditionState

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
    val editionState: IEditionState = EditionState(context, scenarioEditor)

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = scenarioEditor.editedScenario
        .map { it == null }
    /** Tells if the user is currently editing a scenario. */
    val isEditingScenario: Flow<Boolean> = scenarioEditor.editedScenario
        .map { it?.id != null }
    /** Tells if the user is currently editing an event. */
    val isEditingEvent: Flow<Boolean> = scenarioEditor.imageEventsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing a condition. */
    val isEditingCondition: Flow<Boolean> = scenarioEditor.imageEventsEditor.conditionsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing an action. */
    val isEditingAction: Flow<Boolean> = scenarioEditor.imageEventsEditor.actionsEditor.editedItem
        .map { it?.id != null }
    /** Tells if the user is currently editing an Intent Extra. */
    val isEditingIntentExtra: Flow<Boolean> = scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.editedItem
        .map { it?.id != null }

    // --- SCENARIO

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long): Boolean {
        val scenario = repository.getScenario(scenarioId) ?: run {
            Log.e(TAG, "Can't start edition, scenario $scenarioId not found")
            return false
        }

        Log.d(TAG, "Start edition of scenario $scenarioId")

        scenarioEditor.startEdition(
            scenario = scenario,
            imageEvents = repository.getImageEvents(scenarioId),
            triggerEvents = repository.getTriggerEvents(scenarioId),
        )
        return true
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions(): Boolean {
        Log.d(TAG, "Save editions")

        val updateResult = repository.updateScenario(
            scenario = scenarioEditor.editedScenario.value ?: return false,
            events = scenarioEditor.imageEventsEditor.editedList.value ?: return false,
        )

        // In case of error, do not stop the edition
        if (!updateResult) return false

        scenarioEditor.stopEdition()
        editedItemsBuilder.resetGeneratedIdsCount()

        return true
    }

    /** Cancel all changes made during the edition. */
    fun stopEdition() {
        Log.d(TAG, "Stop edition")
        scenarioEditor.stopEdition()
    }

    /** Update the currently edited scenario. */
    fun updateEditedScenario(scenario: Scenario): Unit = scenarioEditor.updateEditedScenario(scenario)
    /** Update the priority of the events in the scenario. */
    fun updateEventsOrder(newEvents: List<ImageEvent>): Unit = scenarioEditor.imageEventsEditor.updateList(newEvents)


    // --- EVENT

    fun startEventEdition(event: Event) =
        scenarioEditor.startEventEdition(event)
    fun updateEditedEvent(event: Event) =
        scenarioEditor.updateEditedEvent(event)
    fun updateActionsOrder(actions: List<Action>) =
        scenarioEditor.updateActionsOrder(actions)
    fun upsertEditedEvent() =
        scenarioEditor.upsertEditedEvent()
    fun deleteEditedEvent() =
        scenarioEditor.deleteEditedEvent()
    fun stopEventEdition(): Unit =
        scenarioEditor.stopEventEdition()


    // --- CONDITION

    fun startConditionEdition(condition: ImageCondition): Unit =
        scenarioEditor.imageEventsEditor.conditionsEditor.startItemEdition(condition)
    fun updateEditedCondition(condition: ImageCondition): Unit =
        scenarioEditor.imageEventsEditor.conditionsEditor.updateEditedItem(condition)
    fun upsertEditedCondition(): Unit =
        scenarioEditor.imageEventsEditor.conditionsEditor.upsertEditedItem()
    fun deleteEditedCondition(): Unit =
        scenarioEditor.imageEventsEditor.conditionsEditor.deleteEditedItem()
    fun stopConditionEdition(): Unit =
        scenarioEditor.imageEventsEditor.conditionsEditor.stopItemEdition()


    // --- ACTION

    fun startActionEdition(action: Action): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.startItemEdition(action)
    fun updateEditedAction(action: Action): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.updateEditedItem(action)
    fun upsertEditedAction(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.upsertEditedItem()
    fun deleteEditedAction(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.deleteEditedItem()
    fun stopActionEdition(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.stopItemEdition()


    // --- INTENT EXTRA

    fun startIntentExtraEdition(extra: IntentExtra<out Any>): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.startItemEdition(extra)
    fun updateEditedIntentExtra(extra: IntentExtra<out Any>): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.updateEditedItem(extra)
    fun upsertEditedIntentExtra(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.upsertEditedItem()
    fun deleteEditedIntentExtra(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.deleteEditedItem()
    fun stopIntentExtraEdition(): Unit =
        scenarioEditor.imageEventsEditor.actionsEditor.intentExtraEditor.stopItemEdition()
}
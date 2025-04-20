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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.buzbuz.smartautoclicker.feature.smart.config.domain

import android.util.Log

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.data.ScenarioEditor
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.IEditionState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class EditionRepository @Inject constructor(
    private val repository: IRepository,
) {

    /** Keep tracks of all changes in the currently edited scenario. */
    private val scenarioEditor: ScenarioEditor = ScenarioEditor()

    /** Provides creators for all elements in an edited scenario. */
    val editedItemsBuilder: EditedItemsBuilder = EditedItemsBuilder(repository, scenarioEditor)
    /** Provides the states of all elements in the edited scenario. */
    val editionState: IEditionState = EditionState(repository, scenarioEditor)

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = scenarioEditor.editedScenario
        .map { it == null }
    /** Tells if the user is currently editing a scenario. */
    val isEditingScenario: Flow<Boolean> = scenarioEditor.editedScenario
        .map { it?.id != null }
    /** Tells if the user is currently editing an event. */
    val isEditingEvent: Flow<Boolean> = scenarioEditor.currentEventEditor
        .flatMapLatest { eventsEditor -> eventsEditor?.editedItem ?: flowOf(null) }
        .map { it?.id != null }
    /** Tells if the user is currently editing a condition. */
    val isEditingCondition: Flow<Boolean> = scenarioEditor.currentEventEditor
        .flatMapLatest { eventsEditor -> eventsEditor?.conditionsEditor?.editedItem ?: flowOf(null) }
        .map { it?.id != null }
    /** Tells if the user is currently editing an action. */
    val isEditingAction: Flow<Boolean> = scenarioEditor.currentEventEditor
        .flatMapLatest { eventsEditor -> eventsEditor?.actionsEditor?.editedItem ?: flowOf(null) }
        .map { it?.id != null }
    /** Tells if the user is currently editing an Intent Extra. */
    val isEditingIntentExtra: Flow<Boolean> = scenarioEditor.currentEventEditor
        .flatMapLatest { eventsEditor -> eventsEditor?.actionsEditor?.intentExtraEditor?.editedItem ?: flowOf(null) }
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
            screenEvents = repository.getScreenEvents(scenarioId),
            triggerEvents = repository.getTriggerEvents(scenarioId),
        )
        return true
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions(): Boolean {
        Log.d(TAG, "Save editions")

        val updateResult = repository.updateScenario(
            scenario = scenarioEditor.editedScenario.value ?: return false,
            events = scenarioEditor.getAllEditedEvents(),
        )

        // In case of error, do not stop the edition
        if (!updateResult) return false

        scenarioEditor.stopEdition()
        repository.cleanupUnusedBitmaps(editedItemsBuilder.newImageConditionsPaths)
        editedItemsBuilder.resetBuilder()

        return true
    }

    /** Cancel all changes made during the edition. */
    suspend fun stopEdition() {
        Log.d(TAG, "Stop edition")
        scenarioEditor.stopEdition()
        repository.cleanupUnusedBitmaps(editedItemsBuilder.newImageConditionsPaths)
        editedItemsBuilder.resetBuilder()
    }

    /** Update the currently edited scenario. */
    fun updateEditedScenario(scenario: Scenario): Unit = scenarioEditor.updateEditedScenario(scenario)
    /** Update the priority of the events in the scenario. */
    fun updateScreenEventsOrder(newEvents: List<ScreenEvent>) {
        scenarioEditor.updateScreenEventsOrder(
            newEvents.mapIndexed { index, event -> event.copy(priority = index) }
        )
    }

    // --- EVENT

    fun startEventEdition(event: Event) =
        scenarioEditor.startEventEdition(event)
    fun updateEditedEvent(event: Event) =
        scenarioEditor.updateEditedEvent(event)
    fun updateActionsOrder(actions: List<Action>) {
        scenarioEditor.updateActionsOrder(
            actions.mapIndexed { index, action -> action.copyBase(priority = index) }
        )
    }
    fun updateScreenConditionsOrder(screenConditions: List<ScreenCondition>) {
        scenarioEditor.updateImageConditionsOrder(
            screenConditions.mapIndexed { index, screenCond ->
                screenCond.copyBase(priority = index)
            }
        )
    }

    fun upsertEditedEvent() =
        scenarioEditor.upsertEditedEvent()
    fun deleteEditedEvent() =
        scenarioEditor.deleteEditedEvent()
    fun stopEventEdition(): Unit =
        scenarioEditor.stopEventEdition()


    // --- CONDITION

    fun startConditionEdition(condition: Condition) =
        scenarioEditor.currentEventEditor.value?.conditionsEditor?.startItemEdition(condition)
    fun updateEditedCondition(condition: Condition) =
        scenarioEditor.currentEventEditor.value?.conditionsEditor?.updateEditedItem(condition)
    fun upsertEditedCondition() =
        scenarioEditor.currentEventEditor.value?.conditionsEditor?.upsertEditedItem()
    fun deleteEditedCondition() =
        scenarioEditor.currentEventEditor.value?.conditionsEditor?.deleteEditedItem()
    fun stopConditionEdition() =
        scenarioEditor.currentEventEditor.value?.conditionsEditor?.stopItemEdition()


    // --- ACTION

    fun startActionEdition(action: Action) =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.startItemEdition(action)
    fun updateEditedAction(action: Action) =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.updateEditedItem(action)
    fun upsertEditedAction() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.upsertEditedItem()
    fun deleteEditedAction() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.deleteEditedItem()
    fun stopActionEdition() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.stopItemEdition()


    // --- INTENT EXTRA

    fun startIntentExtraEdition(extra: IntentExtra<out Any>) =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.startItemEdition(extra)
    fun updateEditedIntentExtra(extra: IntentExtra<out Any>) =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.updateEditedItem(extra)
    fun upsertEditedIntentExtra() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.upsertEditedItem()
    fun deleteEditedIntentExtra() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.deleteEditedItem()
    fun stopIntentExtraEdition() =
        scenarioEditor.currentEventEditor.value?.actionsEditor?.intentExtraEditor?.stopItemEdition()
}


/** Tag for logs */
private const val TAG = "EditionRepository"
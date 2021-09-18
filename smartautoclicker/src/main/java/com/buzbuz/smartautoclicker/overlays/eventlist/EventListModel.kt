/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.eventlist

import android.content.Context
import android.util.Log

import androidx.annotation.IntDef
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.AND
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.database.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.eventconfig.EventConfigDialog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [EventListDialog].
 * To handle all the cases (edit, copy, reorder), several display mode are declared using the [Mode] values. The
 * current mode can be changed through the [uiMode] member.
 *
 * @param context the Android context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventListModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)

    /** Currently selected scenario via [setScenario]. */
    private val scenarioId = MutableStateFlow<Long?>(null)
    /** The currently selected scenario. */
    private val scenario: StateFlow<Scenario?> = scenarioId
        .flatMapLatest { id ->
            id?.let { repository.getScenario(it) } ?: flow { emit(null) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /** List of events for the scenario specified in [scenario]. */
    val events: StateFlow<List<Event>> = scenario
        .filterNotNull()
        .flatMapLatest { scenario ->
            repository.getEventList(scenario.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    /** Backing property for [uiMode]. */
    private val _uiMode = MutableStateFlow(EDITION)
    /** Current Ui mode of the dialog. */
    val uiMode: StateFlow<Int> = _uiMode

    /**
     * Set a scenario for this [EventListModel].
     * This will modify the content of [events].
     *
     * @param scenarioValue the scenario value.
     */
    fun setScenario(scenarioValue: Scenario) {
        scenarioId.value = scenarioValue.id
    }

    /**
     * Set the current Ui mode.
     *
     * @param mode the new mode.
     */
    fun setUiMode(@Mode mode: Int) {
        _uiMode.value = mode
    }

    /**
     * Creates a new event.
     *
     * @param context the Android context.
     * @return the new event.
     */
    fun getNewEvent(context: Context) = Event(
        scenarioId = scenario.value!!.id,
        name = context.getString(R.string.default_event_name),
        conditionOperator = AND,
        priority = events.value.size,
        conditions = mutableListOf(),
        actions = mutableListOf(),
    )

    /**
     * Get the complete event for an event.
     *
     * @param event the event to get the complete version of.
     * @param forCopy true if the generated event is for copy purpose, false if not.
     * @param onCompleted callback called upon completion.
     */
    fun getCompleteEvent(event: Event, forCopy: Boolean, onCompleted: (Event) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val completeEvent = repository.getCompleteEvent(event)
            if (forCopy) {
                completeEvent.priority = events.value.size
                completeEvent.cleanUpIds()
            }

            withContext(Dispatchers.Main) {
                onCompleted.invoke(completeEvent)
            }
        }
    }

    /**
     * Update the priority of the events in the scenario.
     *
     * @param events the events, ordered by their new priorities. They must be in the current scenario and have a
     *               defined id.
     */
    fun updateEventsPriority(events: List<Event>) {
        if (scenario.value == null || events.isEmpty()) {
            Log.e(TAG, "Can't update click priorities, scenario is not matching.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) { repository.updateEventsPriority(events) }
    }

    /**
     * Add or update an event.
     * If the event id is unset, it will be added. If not, updated.
     *
     * @param event the event to add/update.
     */
    fun addOrUpdateEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            if (event.id == 0L) {
                repository.addEvent(event)
            } else {
                repository.updateEvent(event)
            }
        }
    }

    /**
     * Delete an event.
     *
     * @param event the event to delete.
     */
    fun deleteEvent(event: Event) {
        if (scenario.value == null) {
            Log.e(TAG, "Can't delete click with scenario id $event.scenarioId, " +
                    "invalid model scenario ${scenario.value}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) { repository.removeEvent(event) }
    }
}

/** Define the different display mode for the dialog. */
@IntDef(EDITION, COPY, REORDER)
@Retention(AnnotationRetention.SOURCE)
annotation class Mode
/**
 * Shown by default when the dialog is displayed. The action button is shown on each event item
 * and clicking on it will delete the event. Clicking on an item will open the dialog shown by
 * [EventConfigDialog]. The dialog buttons actions are:
 *  - [AlertDialog.BUTTON_POSITIVE]: The dialog is dismissed.
 *  - [AlertDialog.BUTTON_NEGATIVE]: Goes to reorder mode.
 *  - [AlertDialog.BUTTON_NEUTRAL]: Opens the dialog shown by MultiChoiceDialog proposing to create
 *  a new event or copy one. If there is no event on the list, it will directly open the event config dialog.
 */
const val EDITION = 1
/**
 * Shown when clicking on reorder. The action button is hide on each event item. Clicking on an
 * item will open the event config dialog, allowing you to modify the copied click.
 * The dialogs buttons are:
 *  - [AlertDialog.BUTTON_POSITIVE]: The clicks order changes are validated. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEGATIVE]: The clicks order changes are discarded. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEUTRAL]: The button is hide.
 */
const val COPY = 2
/**
 * Shown when clicking on reorder. The action button show the move icon on each event item. Long
 * clicking and moving on an item will drag and drop the item in order to allow clicks reordering. The dialogs
 * buttons are:
 *  - [AlertDialog.BUTTON_POSITIVE]: The clicks order changes are validated. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEGATIVE]: The clicks order changes are discarded. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEUTRAL]: The button is hide.
 */
const val REORDER = 3

private const val TAG = "EventListModel"

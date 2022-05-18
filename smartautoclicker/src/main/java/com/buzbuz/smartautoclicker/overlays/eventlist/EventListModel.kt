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
package com.buzbuz.smartautoclicker.overlays.eventlist

import android.content.Context
import android.util.Log

import androidx.annotation.IntDef
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.eventconfig.EventConfigDialog
import com.buzbuz.smartautoclicker.overlays.utils.newDefaultEvent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    /** Backing property for [scenarioId]. */
    private val _scenarioId = MutableStateFlow<Long?>(null)
    /** Currently selected scenario via [setScenario]. */
    val scenarioId: StateFlow<Long?> = _scenarioId
    /** The currently selected scenario. */
    private val scenario: StateFlow<Scenario?> = scenarioId
        .flatMapLatest { id ->
            id?.let { repository.getScenario(it) } ?: emptyFlow()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /** List of events for the scenario specified in [scenario]. */
    val events: StateFlow<List<Event>?> = scenario
        .filterNotNull()
        .flatMapLatest { scenario ->
            repository.getCompleteEventList(scenario.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /** Backing property for [uiMode]. */
    private val _uiMode = MutableStateFlow(EDITION)
    /** Current Ui mode of the dialog. */
    val uiMode: StateFlow<Int> = _uiMode

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible = _uiMode
        .combine(repository.getEventCount()) { uiMode, eventCount ->
            uiMode == EDITION && eventCount > 0
        }

    /**
     * Set a scenario for this [EventListModel].
     * This will modify the content of [events].
     *
     * @param scenarioValue the scenario value.
     */
    fun setScenario(scenarioValue: Scenario) {
        _scenarioId.value = scenarioValue.id
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
    fun getNewEvent(context: Context) = newDefaultEvent(
        context = context,
        scenarioId = scenario.value!!.id,
        scenarioEventsSize = events.value!!.size,
    )

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
@IntDef(EDITION, REORDER)
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
 * Shown when clicking on reorder. The action button show the move icon on each event item. Long
 * clicking and moving on an item will drag and drop the item in order to allow clicks reordering. The dialogs
 * buttons are:
 *  - [AlertDialog.BUTTON_POSITIVE]: The clicks order changes are validated. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEGATIVE]: The clicks order changes are discarded. Goes to edition mode.
 *  - [AlertDialog.BUTTON_NEUTRAL]: The button is hide.
 */
const val REORDER = 2

private const val TAG = "EventListModel"

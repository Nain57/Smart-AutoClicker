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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.scenario.eventlist

import android.app.Application
import android.content.Context
import android.view.View

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull

class EventListViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository of the application. */
    private val repository: Repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)
    /** Monitors the views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** Currently configured events. */
    val eventsItems = editionRepository.editionState.eventsState
        .mapNotNull { it.value }

    /** Tells if the limitation in event count have been reached. */
    val isEventLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(eventsItems) { isProModePurchased, events ->
            !isProModePurchased && events.size >= ProModeAdvantage.Limitation.EVENT_COUNT_LIMIT.limit
        }
    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> =
        combine(repository.getAllEventsFlow(), editionRepository.editionState.eventsState) { allEvts, scenarioEvts ->
            allEvts.isNotEmpty() || !scenarioEvts.value.isNullOrEmpty()
        }

    /**
     * Creates a new event item.
     * @param context the Android context.
     * @return the new event item.
     */
    fun createNewEvent(context: Context, event: Event? = null): Event = with(editionRepository.editedItemsBuilder) {
        if (event == null) createNewEvent(context)
        else createNewEventFrom(event)
    }

    fun startEventEdition(event: Event) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.upsertEditedEvent()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEvent()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedEvent() = editionRepository.stopEventEdition()

    /** Update the priority of the events in the scenario. */
    fun updateEventsPriority(events: List<Event>) = editionRepository.updateEventsOrder(events)

    fun onEventCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.EVENT_COUNT_LIMIT)
    }

    fun monitorFirstEventView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT)
    }
}

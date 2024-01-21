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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.scenario.triggerevents

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull

class TriggerEventListViewModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)

    /** Currently configured events. */
    val triggerEvents = editionRepository.editionState.editedTriggerEventsState
        .mapNotNull { it.value }

    /** Tells if the limitation in event count have been reached. */
    val isEventLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(triggerEvents) { isProModePurchased, events ->
            !isProModePurchased && events.size >= ProModeAdvantage.Limitation.EVENT_COUNT_LIMIT.limit
        }
    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> = editionRepository.editionState.canCopyTriggerEvents

    /**
     * Creates a new event item.
     * @param context the Android context.
     * @return the new event item.
     */
    fun createNewEvent(context: Context, event: TriggerEvent? = null): TriggerEvent = with(editionRepository.editedItemsBuilder) {
        if (event == null) createNewTriggerEvent(context)
        else createNewTriggerEventFrom(from = event)
    }

    fun startEventEdition(event: TriggerEvent) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.upsertEditedEvent()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEvent()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedEvent() = editionRepository.stopEventEdition()

    fun onEventCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.EVENT_COUNT_LIMIT)
    }
}

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
package com.buzbuz.smartautoclicker.overlays.config.scenario.eventlist

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultEvent
import com.buzbuz.smartautoclicker.overlays.config.scenario.ConfiguredEvent
import com.buzbuz.smartautoclicker.overlays.config.scenario.ConfiguredScenario
import com.buzbuz.smartautoclicker.overlays.config.scenario.INVALID_CONFIGURED_EVENT_ITEM_ID

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class EventListViewModel(application: Application) : AndroidViewModel(application) {

    /** The event currently configured. */
    private lateinit var configuredScenario: MutableStateFlow<ConfiguredScenario?>

    /** List of events for the scenario specified in [configuredScenario]. */
    val eventsItems: Flow<List<ConfiguredEvent>?> by lazy { configuredScenario.map { it?.events } }
    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> by lazy { configuredScenario.map { it?.events?.isNotEmpty() ?: false } }

    /**
     * Set a scenario for this [EventListViewModel].
     * This will modify the content of [eventsItems].
     *
     * @param scenario the scenario flow.
     */
    fun setScenario(scenario: MutableStateFlow<ConfiguredScenario?>) {
        configuredScenario = scenario
    }

    /** Get all events currently configured in this scenario. */
    fun getConfiguredEventList(): List<Event> = configuredScenario.value?.events?.map { it.event } ?: emptyList()

    /**
     * Creates a new event item.
     *
     * @param context the Android context.
     * @param event the event represented by this item. Null for a new event.
     *
     * @return the new event item.
     */
    fun getNewEventItem(context: Context, event: Event? = null): ConfiguredEvent {
        configuredScenario.value?.let { confScenario ->
            return ConfiguredEvent(
                event = event ?: newDefaultEvent(
                    context = context,
                    scenarioId = confScenario.scenario.id,
                    scenarioEventsSize = confScenario.events.size,
                )
            )
        } ?: throw IllegalStateException("No scenario defined !")
    }

    /**
     * Add or update an event.
     * If the event id is unset, it will be added. If not, updated.
     *
     * @param item the item to add/update.
     */
    fun addOrUpdateEvent(item: ConfiguredEvent) {
        val items = (configuredScenario.value?.events ?: emptyList()).toMutableList()

        if (item.itemId == INVALID_CONFIGURED_EVENT_ITEM_ID) {
            items.add(item.copy(itemId = items.size))
        } else {
            val itemIndex = items.indexOfFirst { other -> item.itemId == other.itemId }
            if (itemIndex == -1) return

            items[itemIndex] = item
        }

        updateConfiguredEventItems(items)
    }

    /**
     * Delete an event.
     *
     * @param item the item to delete.
     */
    fun deleteEvent(item: ConfiguredEvent) {
        val items = (configuredScenario.value?.events ?: emptyList()).toMutableList()

        val itemIndex = items.indexOfFirst { other -> item.itemId == other.itemId }
        if (itemIndex == -1) return
        items.removeAt(itemIndex)

        updateConfiguredEventItems(items)
    }

    /**
     * Update the priority of the events in the scenario.
     *
     * @param events the events, ordered by their new priorities. They must be in the current scenario and have a
     *               defined id.
     */
    fun updateEventsPriority(events: List<ConfiguredEvent>) {
        updateConfiguredEventItems(events)
    }

    private fun updateConfiguredEventItems(newItems: List<ConfiguredEvent>) {
        val currentConfiguredScenario = configuredScenario.value ?: return

        configuredScenario.value = currentConfiguredScenario.copy(
            scenario = currentConfiguredScenario.scenario.copy(eventCount = newItems.size),
            events = newItems,
        )
    }
}

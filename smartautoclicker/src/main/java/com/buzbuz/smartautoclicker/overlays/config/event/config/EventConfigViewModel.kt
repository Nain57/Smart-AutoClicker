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
package com.buzbuz.smartautoclicker.overlays.config.event.config

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import com.buzbuz.smartautoclicker.domain.ConditionOperator

import com.buzbuz.smartautoclicker.domain.Event

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take

class EventConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** The event currently configured. */
    private lateinit var configuredEvent: MutableStateFlow<Event?>

    /** The event condition operator currently edited by the user. */
    val conditionOperator: Flow<Int?> by lazy {
        configuredEvent.map { it?.conditionOperator }
    }

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> by lazy {
        configuredEvent
            .filterNotNull()
            .map { it.name }
            .take(1)
    }

    /** Tells if the event name is valid or not. */
    val eventNameError: Flow<Boolean> by lazy {
        configuredEvent.map { it?.name?.isEmpty() ?: true }
    }

    /** Set the event currently configured by the UI. */
    fun setConfiguredEvent(event: MutableStateFlow<Event?>) {
        configuredEvent = event
    }

    /** Set a new name for the configured event. */
    fun setEventName(newName: String) {
        configuredEvent.value?.let { event ->
            configuredEvent.value = event.copy(name = newName)
        }
    }

    /** Toggle the condition operator between AND and OR. */
    fun setConditionOperator(@ConditionOperator operator: Int) {
        configuredEvent.value?.let { event ->
            configuredEvent.value = event.copy(conditionOperator = operator)
        }
    }
}
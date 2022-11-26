/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.event.config

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take

class EventConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** The event currently configured. */
    private lateinit var configuredEvent: MutableStateFlow<Event?>

    private val conditionAndItem = DropdownItem(
        title = R.string.dropdown_item_title_condition_and,
        helperText = R.string.dropdown_helper_text_condition_and,
    )
    private val conditionOrItem = DropdownItem(
        title= R.string.dropdown_item_title_condition_or,
        helperText = R.string.dropdown_helper_text_condition_or,
    )
    val conditionOperatorsItems = listOf(conditionAndItem, conditionOrItem)

    /** The event condition operator currently edited by the user. */
    val conditionOperator: Flow<DropdownItem> by lazy {
        configuredEvent
            .map {
                when (it?.conditionOperator) {
                    AND -> conditionAndItem
                    OR -> conditionOrItem
                    else -> null
                }
            }
            .filterNotNull()
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

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(operatorItem: DropdownItem) {
        configuredEvent.value?.let { event ->
            val operator = when (operatorItem) {
                conditionAndItem -> AND
                conditionOrItem -> OR
                else -> return
            }

            configuredEvent.value = event.copy(conditionOperator = operator)
        }
    }
}
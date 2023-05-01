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
package com.buzbuz.smartautoclicker.overlays.config.event.config

import android.app.Application
import android.content.Context
import androidx.annotation.DrawableRes

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.billing.IBillingRepository
import com.buzbuz.smartautoclicker.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class EventConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)

    /** Currently configured event. */
    private val configuredEvent = editionRepository.editedEvent
        .filterNotNull()

    private val enableEventItem = DropdownItem(
        title = R.string.dropdown_item_title_event_state_enabled,
        helperText = R.string.dropdown_helper_text_event_state_enabled,
    )
    private val disableEventItem = DropdownItem(
        title= R.string.dropdown_item_title_event_state_disabled,
        helperText = R.string.dropdown_helper_text_event_state_disabled,
    )
    val eventStateDropdownState: Flow<EventStateDropdownUiState> = billingRepository.isProModePurchased
        .map { isProModePurchased ->
            EventStateDropdownUiState(
                items = listOf(enableEventItem, disableEventItem),
                enabled = isProModePurchased,
                disabledIcon = R.drawable.ic_pro_small,
            )
        }

    /** The enabled on start state of the configured event. */
    val eventStateItem: Flow<DropdownItem> = configuredEvent
        .map {
            when (it.event.enabledOnStart) {
                true -> enableEventItem
                false -> disableEventItem
            }
        }
        .filterNotNull()

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
    val conditionOperator: Flow<DropdownItem> = configuredEvent
        .map {
            when (it.event.conditionOperator) {
                AND -> conditionAndItem
                OR -> conditionOrItem
                else -> null
            }
        }
        .filterNotNull()

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> = configuredEvent
        .filterNotNull()
        .map { it.event.name }
        .take(1)

    /** Tells if the event name is valid or not. */
    val eventNameError: Flow<Boolean> = configuredEvent
        .map { it.event.name.isEmpty() }

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Set a new name for the configured event. */
    fun setEventName(newName: String) {
        editionRepository.editedEvent.value?.let { conf ->
            viewModelScope.launch {
                editionRepository.updateEditedEvent(conf.event.copy(name = newName))
            }
        }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(operatorItem: DropdownItem) {
        editionRepository.editedEvent.value?.let { conf ->
            val operator = when (operatorItem) {
                conditionAndItem -> AND
                conditionOrItem -> OR
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedEvent(conf.event.copy(conditionOperator = operator))
            }
        }
    }

    /** Toggle the event state between true and false. */
    fun setEventState(state: DropdownItem) {
        editionRepository.editedEvent.value?.let { conf ->
            val value = when (state) {
                enableEventItem -> true
                disableEventItem -> false
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedEvent(conf.event.copy(enabledOnStart = value))
            }
        }
    }

    fun onEventStateClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.EVENT_STATE)
    }
}

data class EventStateDropdownUiState(
    val items: List<DropdownItem>,
    val enabled: Boolean = true,
    @DrawableRes val disabledIcon: Int? = null,
)
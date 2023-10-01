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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.config

import android.app.Application
import android.content.Context
import android.view.View

import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.feature.scenario.config.R

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class EventConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** Currently configured event. */
    private val configuredEvent = editionRepository.editionState.editedEventState
        .mapNotNull { it.value }

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
        .map { event ->
            when (event.enabledOnStart) {
                true -> enableEventItem
                false -> disableEventItem
            }
        }
        .filterNotNull()

    val conditionAndItem = DropdownItem(
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
        .map { event ->
            when (event.conditionOperator) {
                AND -> conditionAndItem
                OR -> conditionOrItem
                else -> null
            }
        }
        .filterNotNull()

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> = configuredEvent
        .filterNotNull()
        .map { it.name }
        .take(1)

    /** Tells if the event name is valid or not. */
    val eventNameError: Flow<Boolean> = configuredEvent
        .map { it.name.isEmpty() }

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Set a new name for the configured event. */
    fun setEventName(newName: String) {
        editionRepository.editionState.getEditedEvent()?.let { event ->
            viewModelScope.launch {
                editionRepository.updateEditedEvent(event.copy(name = newName))
            }
        }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(operatorItem: DropdownItem) {
        editionRepository.editionState.getEditedEvent()?.let { event ->
            val operator = when (operatorItem) {
                conditionAndItem -> AND
                conditionOrItem -> OR
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedEvent(event.copy(conditionOperator = operator))
            }
        }
    }

    /** Toggle the event state between true and false. */
    fun setEventState(state: DropdownItem) {
        editionRepository.editionState.getEditedEvent()?.let { conf ->
            val value = when (state) {
                enableEventItem -> true
                disableEventItem -> false
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedEvent(conf.copy(enabledOnStart = value))
            }
        }
    }

    fun onEventStateClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.EVENT_STATE)
    }

    fun monitorConditionOperatorView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_DROPDOWN_CONDITION_OPERATOR, view)
    }

    fun monitorDropdownItemAndView(view: View) {
        monitoredViewsManager.attach(
            MonitoredViewType.EVENT_DIALOG_DROPDOWN_ITEM_AND,
            view,
            ViewPositioningType.SCREEN,
        )
    }

    fun stopDropdownItemConditionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_DROPDOWN_ITEM_AND)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.EVENT_DIALOG_DROPDOWN_CONDITION_OPERATOR)
            detach(MonitoredViewType.EVENT_DIALOG_DROPDOWN_ITEM_AND)
        }
    }
}

data class EventStateDropdownUiState(
    val items: List<DropdownItem>,
    val enabled: Boolean = true,
    @DrawableRes val disabledIcon: Int? = null,
)
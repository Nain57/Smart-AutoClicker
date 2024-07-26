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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
class EventDialogViewModel @Inject constructor(
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = combine(
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedEventConditionsState,
        editionRepository.editionState.editedEventActionsState,
    ) { editedEvent, conditions, actions, ->
        buildMap {
            put(R.id.page_event, editedEvent.value?.name?.isNotEmpty() ?: false)
            put(R.id.page_conditions, conditions.canBeSaved)
            put(R.id.page_actions, actions.canBeSaved)
        }
    }

    private val configuredEvent = editionRepository.editionState.editedEventState
        .mapNotNull { it.value }

    val eventCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedEventState
        .map { it.canBeSaved }

    /** Tells if the user is currently editing an event. If that's not the case, dialog should be closed. */
    val isEditingEvent: Flow<Boolean> = editionRepository.isEditingEvent
        .distinctUntilChanged()
        .debounce(1000)

    val eventName: Flow<String?> = configuredEvent
        .filterNotNull()
        .map { it.name }
        .take(1)

    val eventNameError: Flow<Boolean> = configuredEvent
        .map { it.name.isEmpty() }

    val configuredEventConditions: Flow<List<Condition>> = editionRepository.editionState.editedEventConditionsState
        .mapNotNull { it.value }

    val triggerConditionsDescription:  Flow<List<EventChildrenItem>> =
        editionRepository.editionState.editedEventTriggerConditionsState.mapNotNull { conditionsListState ->
            conditionsListState.value?.toTriggerConditionsChildrenItem()
        }

    val conditionOperator: Flow<Int> = configuredEvent
        .map { event -> event.conditionOperator }

    val actionsDescriptions: Flow<List<EventChildrenItem>> = editionRepository.editionState.editedEventActionsState.mapNotNull { actionsState ->
        actionsState.value?.toActionsChildrenItem()
    }

    val eventEnabledOnStart: Flow<Boolean> = configuredEvent
        .map { event -> event.enabledOnStart }

    val shouldShowTryCard: Flow<Boolean> = configuredEvent
        .map { it is ImageEvent }

    val canTryEvent: Flow<Boolean> = configuredEvent
        .map { it.isComplete() }


    fun isConfiguringScreenEvent(): Boolean =
        editionRepository.editionState.getEditedEvent<Event>() is ImageEvent

    fun getTryInfo(): Pair<Scenario, ImageEvent>? {
        val scenario = editionRepository.editionState.getScenario() ?: return null
        val event = editionRepository.editionState.getEditedEvent<ImageEvent>() ?: return null

        return scenario to event
    }

    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(repository, condition, onBitmapLoaded)

    fun isEventHaveRelatedActions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByAction()

    fun setEventName(newName: String) {
        updateEditedEvent { oldValue -> oldValue.copyBase(name = newName) }
    }

    fun setConditionOperator(@ConditionOperator operator: Int) {
        updateEditedEvent { oldValue ->
            oldValue.copyBase(conditionOperator = operator)
        }
    }

    fun toggleEventState() {
        updateEditedEvent { oldValue ->
            oldValue.copyBase(enabledOnStart = !oldValue.enabledOnStart)
        }
    }

    fun monitorActionTabView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS, view)
    }
    fun monitorConditionTabView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS, view)
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS)
            detach(MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS)
        }
    }

    private fun updateEditedEvent(closure: (oldValue: Event) -> Event?) {
        editionRepository.editionState.getEditedEvent<Event>()?.let { oldValue ->
            viewModelScope.launch {
                closure(oldValue)?.let { newValue ->
                    editionRepository.updateEditedEvent(newValue)
                }
            }
        }
    }

    private fun List<TriggerCondition>.toTriggerConditionsChildrenItem(): List<EventChildrenItem> = map { condition ->
        EventChildrenItem(
            iconRes = when (condition) {
                is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
                is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
                is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
            },
            isInError = !condition.isComplete(),
        )
    }

    private fun List<Action>.toActionsChildrenItem(): List<EventChildrenItem> = map { action ->
        EventChildrenItem(
            iconRes = when (action) {
                is Action.ChangeCounter -> R.drawable.ic_change_counter
                is Action.Click -> R.drawable.ic_click
                is Action.Intent -> R.drawable.ic_intent
                is Action.Pause -> R.drawable.ic_wait
                is Action.Swipe -> R.drawable.ic_swipe
                is Action.ToggleEvent -> R.drawable.ic_toggle_event
            },
            isInError = !action.isComplete(),
        )
    }
}
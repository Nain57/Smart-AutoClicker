/*
 * Copyright (C) 2025 Kevin Buzeau
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

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
class EventDialogViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val configuredEvent = editionRepository.editionState.editedEventState
        .mapNotNull { it.value }

    private val editedEventHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedEventState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    val screenConditions: Flow<List<UiScreenCondition>> =
        editionRepository.editionState.editedEventScreenConditionsState
            .mapNotNull { screenConditionsState ->
                screenConditionsState.value?.map { screenCondition ->
                    screenCondition.toUiScreenCondition(
                        context = context,
                        inError = !screenCondition.isComplete(),
                    )
                }
            }

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

    val isScreenEvent: Flow<Boolean> = configuredEvent
        .map { it is ScreenEvent }

    val keepDetecting: Flow<Boolean> = configuredEvent
        .filterIsInstance<ScreenEvent>()
        .map { event -> event.keepDetecting }

    val canTryEvent: Flow<Boolean> = configuredEvent
        .filterIsInstance<ScreenEvent>()
        .map { it.isComplete() }


    fun isConfiguringScreenEvent(): Boolean =
        editionRepository.editionState.getEditedEvent<Event>() is ScreenEvent

    fun hasUnsavedModifications(): Boolean =
        editedEventHasChanged.value

    fun getTryInfo(): Pair<Scenario, ScreenEvent>? {
        val scenario = editionRepository.editionState.getScenario() ?: return null
        val event = editionRepository.editionState.getEditedEvent<ScreenEvent>() ?: return null

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

    fun toggleKeepDetectingState() {
        updateEditedEvent { oldValue ->
            if (oldValue is ScreenEvent) oldValue.copy(keepDetecting = !oldValue.keepDetecting)
            else oldValue
        }
    }

    fun isLegacyActionUiEnabled(): Boolean =
        settingsRepository.isLegacyActionUiEnabled()

    fun monitorViews(conditionsField: View, conditionOperatorAndView: View, actionsField: View, saveButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS, conditionsField)
            attach(MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND, conditionOperatorAndView)
            attach(MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS, actionsField)
            attach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE, saveButton)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE)
            detach(MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND)
            detach(MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS)
            detach(MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS)
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
            iconRes = condition.getIconRes(),
            isInError = !condition.isComplete(),
        )
    }

    private fun List<Action>.toActionsChildrenItem(): List<EventChildrenItem> = map { action ->
        EventChildrenItem(
            iconRes = action.getIconRes(),
            isInError = !action.isComplete(),
        )
    }
}
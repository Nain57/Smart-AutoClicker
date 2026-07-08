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

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class EventDialogViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val settingsRepository: SettingsRepository,
    private val tutorialRepository: TutorialRepository,
) : ViewModel() {

    private val userEventCooldownTimeUnit: MutableStateFlow<TimeUnitDropDownItem?> =
        MutableStateFlow(editionRepository.editionState.getEditedEvent<Event>()?.getInitialCooldownTimeUnit())

    private var cachedCooldownMs: Long = editionRepository.editionState.getEditedEvent<ScreenEvent>()?.cooldownMs?.takeIf { it > 0 } ?: 100L

    /** Tells if the user is currently editing an event. If that's not the case, dialog should be closed. */
    val isEditingEvent: Flow<Boolean> = editionRepository.isEditingEvent
        .distinctUntilChanged()
        .debounce(1000.milliseconds)

    val uiState: StateFlow<EventDialogUiState?> = editionRepository.editionState.editedEventState
        .combine(userEventCooldownTimeUnit) { eventsState, cooldownTimeUnit ->
            val event = eventsState.value ?: return@combine null
            event.toUiState(
                context = context,
                hasUnsavedModifications = eventsState.hasChanged,
                canBeSaved = eventsState.canBeSaved,
                cooldownTimeUnit = cooldownTimeUnit,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)

    fun isConfiguringScreenEvent(): Boolean =
        editionRepository.editionState.getEditedEvent<Event>() is ScreenEvent

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun isEventHaveRelatedActions(): Boolean =
        editionRepository.editionState.isEditedEventReferencedByAction()

    fun isLegacyActionUiEnabled(): Boolean =
        settingsRepository.isLegacyActionUiEnabled() && !tutorialRepository.isTutorialStarted()

    fun getTryInfo(): Pair<Scenario, ScreenEvent>? {
        val scenario = editionRepository.editionState.getScenario() ?: return null
        val event = editionRepository.editionState.getEditedEvent<ScreenEvent>() ?: return null

        return scenario to event
    }

    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)

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

    fun toggleCooldownState() {
        updateEditedEvent { oldValue ->
            if (oldValue is ScreenEvent) {
                if (oldValue.cooldownMs == 0L) {
                    val unit = if (cachedCooldownMs == 100L) TimeUnitDropDownItem.Milliseconds else userEventCooldownTimeUnit.value ?: TimeUnitDropDownItem.Milliseconds
                    userEventCooldownTimeUnit.update { unit }
                    oldValue.copy(cooldownMs = cachedCooldownMs)
                } else {
                    oldValue.copy(cooldownMs = 0L)
                }
            } else oldValue
        }
    }

    fun setCooldownTimeUnit(timeUnit: TimeUnitDropDownItem) {
        userEventCooldownTimeUnit.update { timeUnit }
    }

    fun setCooldownValue(value: Long?) {
        if (value == null || value <= 0L) return
        val timeUnit = userEventCooldownTimeUnit.value ?: return

        val newCooldownMs = when (timeUnit) {
            TimeUnitDropDownItem.Minutes -> value * 60000
            TimeUnitDropDownItem.Seconds -> value * 1000
            else -> value
        }
        cachedCooldownMs = newCooldownMs

        updateEditedEvent { oldValue ->
            if (oldValue is ScreenEvent) oldValue.copy(cooldownMs = newCooldownMs)
            else oldValue
        }
    }

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

    private fun Event.toUiState(
        context: Context,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
        cooldownTimeUnit: TimeUnitDropDownItem?,
    ): EventDialogUiState =
        when (this) {
            is ScreenEvent -> toScreenEventUiState(context, hasUnsavedModifications, canBeSaved, cooldownTimeUnit ?: TimeUnitDropDownItem.Seconds)
            is TriggerEvent -> toTriggerEventUiState(hasUnsavedModifications, canBeSaved)
        }

    private fun ScreenEvent.toScreenEventUiState(
        context: Context,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
        cooldownTimeUnit: TimeUnitDropDownItem,
    ) = EventDialogUiState.ScreenEvent(
        canBeSaved = canBeSaved,
        hasUnsavedModifications = hasUnsavedModifications,
        name = name,
        nameError = name.isEmpty(),
        enabledOnStart = enabledOnStart,
        conditionOperator = conditionOperator,
        keepDetecting = keepDetecting,
        canTryEvent = isComplete(),
        cooldownEnabled = cooldownMs > 0L,
        cooldownUnit = cooldownTimeUnit,
        cooldownValue = when (cooldownTimeUnit) {
            TimeUnitDropDownItem.Minutes -> cooldownMs / 60000L
            TimeUnitDropDownItem.Seconds -> cooldownMs / 1000L
            else -> cooldownMs
        }.toString(),
        actionsItems = actions.toActionsChildrenItem(),
        imageConditionsItems = conditions.map { condition ->
            condition.toUiScreenCondition(
                context = context,
                shortThreshold = true,
                inError = !condition.isComplete(),
            )
        },
    )

    private fun TriggerEvent.toTriggerEventUiState(
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
    ) = EventDialogUiState.TriggerEvent(
        canBeSaved = canBeSaved,
        hasUnsavedModifications = hasUnsavedModifications,
        name = name,
        nameError = name.isEmpty(),
        enabledOnStart = enabledOnStart,
        conditionOperator = conditionOperator,
        actionsItems = actions.toActionsChildrenItem(),
        triggerConditionsItems = conditions.toTriggerConditionsChildrenItem(),
    )

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

    private fun Event.getInitialCooldownTimeUnit(): TimeUnitDropDownItem? =
        if (this is ScreenEvent) {
            when {
                cooldownMs > 60000 && cooldownMs % 60000 == 0L -> TimeUnitDropDownItem.Minutes
                cooldownMs > 1000 && cooldownMs % 1000 == 0L -> TimeUnitDropDownItem.Seconds
                else -> TimeUnitDropDownItem.Milliseconds
            }
        } else null

}
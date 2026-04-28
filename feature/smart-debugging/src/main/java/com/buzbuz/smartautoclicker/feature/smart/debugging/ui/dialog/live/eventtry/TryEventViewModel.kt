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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.eventtry

import android.content.Context
import androidx.annotation.DrawableRes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugLiveDetectionResultUseCase
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.TryEventActionsItem
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.EventResultUiState
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.ImageConditionResultUiState
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.mapping.toConditionUiState

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map
import kotlin.collections.toMutableList
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
class TryElementViewModel @Inject constructor(
    @ApplicationContext context: Context,
    detectionResultUseCase: GetDebugLiveDetectionResultUseCase,
    private val smartProcessingRepository: SmartProcessingRepository,
) : ViewModel() {

    private val isPlaying: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { state -> state == DetectionState.DETECTING }
        .distinctUntilChanged()

    /** The info on the last positive detection. */
    val displayResults: Flow<EventResultUiState?> = detectionResultUseCase
        .invoke(minDisplayDuration = POSITIVE_VALUE_DISPLAY_TIMEOUT_MS, filterNotFulfilled = false)
        .combine(isPlaying) { result, isPlaying ->
            if (isPlaying) result?.toUiState(context) else null
        }

    fun startTry(context: Context, scenario: Scenario, imageEvent: ImageEvent) {
        viewModelScope.launch {
            delay(500)
            smartProcessingRepository.tryEvent(context, scenario, imageEvent)
        }
    }

    fun stopTry() {
        viewModelScope.launch {
            smartProcessingRepository.stopDetection()
        }
    }
}

private fun DebugLiveEventOccurrence.toUiState(context: Context): EventResultUiState =
    EventResultUiState(
        eventIcon = event.getDebugIcon(),
        eventFulfilled = fulfilled,
        eventName = event.name,
        eventConditionOperator = event.getConditionOperatorText(context),
        eventDuration = context.getDurationText(processingDurationMs),
        actions = event.actions.toActionItems(),
        detectionResults = conditionsResults.toUiState(),
    )

private fun List<DebugLiveEventConditionResult>.toUiState(): List<ImageConditionResultUiState> =
    mapNotNull { result ->
        if (result !is DebugLiveEventConditionResult.Image) return@mapNotNull null
        else result.toConditionUiState()
    }

private fun List<Action>.toActionItems(): List<TryEventActionsItem> =
    if (size <= 5) map { action -> TryEventActionsItem(action.getDebugIcon()) }
    else subList(0, 4)
        .map { action -> TryEventActionsItem(action.getDebugIcon()) }
        .toMutableList()
        .apply { add(TryEventActionsItem(R.drawable.ic_more)) }

@DrawableRes
private fun Event.getDebugIcon(): Int =
    when (this) {
        is ImageEvent -> R.drawable.ic_condition
        is TriggerEvent -> R.drawable.ic_trigger_event
    }

@DrawableRes
private fun Action.getDebugIcon(): Int =
    when (this) {
        is ChangeCounter -> R.drawable.ic_change_counter
        is Click -> R.drawable.ic_click
        is Intent -> R.drawable.ic_intent
        is Notification -> R.drawable.ic_action_notification
        is Pause -> R.drawable.ic_wait
        is SetText -> R.drawable.ic_action_set_text
        is Swipe -> R.drawable.ic_swipe
        is SystemAction -> R.drawable.ic_action_system
        is ToggleEvent -> R.drawable.ic_toggle_event
    }

private fun Event.getConditionOperatorText(context: Context): String =
    when (conditionOperator) {
        AND -> context.getString(R.string.condition_operator_and)
        OR -> context.getString(R.string.condition_operator_or)
        else -> ""
    }

private fun Context.getDurationText(durationMs: Long): String =
    "$durationMs${getString(R.string.dropdown_label_time_unit_ms)}"

/** Delay before removing the last positive result display in debug. */
private val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500.milliseconds
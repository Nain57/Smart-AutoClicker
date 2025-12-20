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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.IRepository
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
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.findWithId
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.formatDebugTimelineTimestamp

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
class DebugReportTimelineViewModel @Inject constructor(
    @ApplicationContext context: Context,
    debuggingRepository: DebuggingRepository,
    smartRepository: IRepository,
) : ViewModel() {

    /** The scenario referenced by the last debug report. Null if not found or no reports. */
    private val scenario: Flow<Scenario?> = debuggingRepository.getLastReportOverview().map { overview ->
        overview?.scenarioId?.let { id -> smartRepository.getScenario(id) }
    }

    /** The image events of the scenario referenced by the last debug report. Null if not found or no reports. */
    private val imageEvents: Flow<List<ImageEvent>?> = scenario.flatMapLatest { scenario ->
        scenario?.id?.databaseId?.let { dbId -> smartRepository.getImageEventsFlow(dbId) } ?: flowOf(null)
    }

    /** The trigger events of the scenario referenced by the last debug report. Null if not found or no reports. */
    private val triggerEvents: Flow<List<TriggerEvent>?> = scenario.flatMapLatest { scenario ->
        scenario?.id?.databaseId?.let { dbId -> smartRepository.getTriggerEventsFlow(dbId) } ?: flowOf(null)
    }

    /** The occurrences of events while the scenario was running. Null if not found or no reports. */
    private val eventsOccurrences: Flow<List<DebugReportEventOccurrence>?> =
        debuggingRepository.getLastReportEventsOccurrences()


    val uiState: StateFlow<DebugReportTimelineUiState> =
        combine(eventsOccurrences, imageEvents, triggerEvents) { occurrences, imgEvts, trigEvts ->
            occurrences.toUiState(context, imgEvts, trigEvts)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugReportTimelineUiState.Loading,
        )


    private fun List<DebugReportEventOccurrence>?.toUiState(
        context: Context,
        imgEvents: List<ImageEvent>?,
        trigEvents: List<TriggerEvent>?,
    ): DebugReportTimelineUiState {
        if (this == null || imgEvents == null || trigEvents == null) return DebugReportTimelineUiState.NotAvailable

        val items = toUiStateItems(context, imgEvents, trigEvents)

        return if (items.isEmpty()) DebugReportTimelineUiState.Empty
        else DebugReportTimelineUiState.Available(items)
    }

    private fun List<DebugReportEventOccurrence>.toUiStateItems(
        context: Context,
        imgEvents: List<ImageEvent>,
        trigEvents: List<TriggerEvent>,
    ): List<DebugReportTimelineEventOccurrenceItem> =
        mapIndexedNotNull { index, occurrence ->
            val event = when (occurrence) {
                is DebugReportEventOccurrence.ImageEvent -> imgEvents.findWithId(occurrence.eventId)
                is DebugReportEventOccurrence.TriggerEvent -> trigEvents.findWithId(occurrence.eventId)
            } ?: return@mapIndexedNotNull null

            DebugReportTimelineEventOccurrenceItem(
                id = index,
                scenarioId = event.scenarioId.databaseId,
                eventName = event.name,
                timeText = occurrence.relativeTimestampMs.formatDebugTimelineTimestamp(),
                occurrenceText = occurrence.getOccurrenceText(context),
                conditionsText = occurrence.conditionsResults.getConditionFulfilledText(context, event.conditions),
                actions = event.actions.toUiStateItems(),
                occurrence = occurrence,
            )
        }

    private fun List<Action>.toUiStateItems(): List<DebugReportTimelineEventActionItem> =
        mapIndexed { index, action ->
            DebugReportTimelineEventActionItem(
                id = index,
                iconRes = action.getDebugIconRes(),
            )
        }

    private fun <T : DebugReportEventOccurrence> T.getOccurrenceText(context: Context) : String =
        when (this) {
            is DebugReportEventOccurrence.ImageEvent ->
                context.getString(R.string.item_event_occurrence_frame_number, frameNumber)
            is DebugReportEventOccurrence.TriggerEvent ->
                context.getString(R.string.item_event_occurrence_trigger)
        }

    private fun <T : DebugReportConditionResult> List<T>.getConditionFulfilledText(
        context: Context,
        conditions: List<Condition>,
    ): String =
        when (size) {
            0 -> ""
            1 -> context.getString(
                R.string.item_event_occurrence_one_condition_fulfilled,
                conditions.findWithId(first().conditionId)?.name ?: "",
            )
            else -> context.getString(R.string.item_event_occurrence_several_condition_processed, size)
        }

    @DrawableRes
    private fun Action.getDebugIconRes(): Int =
        when (this) {
            is Click -> R.drawable.ic_click
            is Swipe -> R.drawable.ic_swipe
            is Pause -> R.drawable.ic_wait
            is Intent -> R.drawable.ic_intent
            is ToggleEvent ->  R.drawable.ic_toggle_event
            is ChangeCounter -> R.drawable.ic_change_counter
            is Notification -> R.drawable.ic_action_notification
            is SetText -> R.drawable.ic_action_set_text
            is SystemAction -> R.drawable.ic_action_system
        }
}

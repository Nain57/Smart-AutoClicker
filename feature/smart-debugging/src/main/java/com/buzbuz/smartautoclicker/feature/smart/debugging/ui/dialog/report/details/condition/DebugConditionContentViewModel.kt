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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.condition

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.EQUALS
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.GREATER
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.GREATER_OR_EQUALS
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.LOWER
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached.ComparisonOperation.LOWER_OR_EQUALS
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.findWithId
import com.buzbuz.smartautoclicker.feature.smart.debugging.utils.formatDebugDuration

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


@OptIn(ExperimentalCoroutinesApi::class)
class DebugConditionContentViewModel @Inject constructor(
    @ApplicationContext context: Context,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val bitmapRepository: BitmapRepository,
) : ViewModel() {


    private val eventOccurrence: MutableStateFlow<Pair<Event?, DebugReportEventOccurrence?>> =
        MutableStateFlow(Pair(null, null))

    val uiState: StateFlow<DebugConditionContentUiState> = eventOccurrence
        .mapNotNull { (event, occurrence) ->
            if (event == null || occurrence == null) return@mapNotNull null
            else DebugConditionContentUiState.Available(
                items = buildList {
                    add(event.toHeaderItem(context))
                    addAll(occurrence.conditionsResults.toItems(context, event))
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DebugConditionContentUiState.Loading,
        )

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        viewModelScope.launch(ioDispatcher) {
            try {
                val bitmap = bitmapRepository.getConditionBitmap(condition)
                withContext(mainDispatcher) { onBitmapLoaded(bitmap) }
            } catch (_: CancellationException) {
                withContext(mainDispatcher) { onBitmapLoaded(null) }
            }
        }


    fun setOccurrence(scenarioId: Long, occurrence: DebugReportEventOccurrence) {
        viewModelScope.launch {
            val event = when (occurrence) {
                is DebugReportEventOccurrence.ImageEvent -> smartRepository.getImageEvents(scenarioId)
                is DebugReportEventOccurrence.TriggerEvent -> smartRepository.getTriggerEvents(scenarioId)
            }.findWithId(occurrence.eventId) ?: return@launch

            eventOccurrence.update { event to occurrence }
        }
    }


    private fun Event.toHeaderItem(context: Context): EventOccurrenceItem.Header =
        EventOccurrenceItem.Header(
            conditionOperatorValueText = conditionOperator.getOperatorDisplayName(context),
        )

    @Suppress("UNCHECKED_CAST")
    private fun List<DebugReportConditionResult>.toItems(
        context: Context,
        event: Event,
    ): List<EventOccurrenceItem> =
        when (event) {
            is ImageEvent -> (this@toItems as? List<DebugReportConditionResult.ImageCondition>)?.toImageItems(context, event)
            is TriggerEvent -> (this@toItems as? List<DebugReportConditionResult.TriggerCondition>)?.toTriggerItems(context, event)
        } ?: emptyList()

    private fun List<DebugReportConditionResult.ImageCondition>.toImageItems(
        context: Context,
        event: ImageEvent,
    ): List<EventOccurrenceItem> =
        event.conditions
            .sortedBy { condition -> condition.priority }
            .mapNotNull { condition ->
                this@toImageItems.find { result -> condition.id.databaseId == result.conditionId }?.let { result ->
                    EventOccurrenceItem.Image(
                        id = result.conditionId,
                        condition = condition,
                        conditionName = condition.name,
                        durationText = result.detectionDurationMs.formatDebugDuration(),
                        shouldDetectedValue = condition.shouldBeDetected,
                        isFulfilledValue = result.isFulFilled,
                        confidenceValid = result.confidenceRate.toDisplayConfidence() >= condition.threshold.toMinimumConfidence(),
                        confidenceText = context.getString(
                            R.string.item_event_occurrence_details_image_confidence,
                            result.confidenceRate.toDisplayConfidence(), condition.threshold.toMinimumConfidence(),
                        ),
                    )
                }
            }

    private fun List<DebugReportConditionResult.TriggerCondition>.toTriggerItems(
        context: Context,
        event: TriggerEvent,
    ): List<EventOccurrenceItem> =
        event.conditions
            .mapNotNull { condition ->
                this@toTriggerItems.find { result -> condition.id.databaseId == result.conditionId }?.let { result ->
                    EventOccurrenceItem.Trigger(
                        id = result.conditionId,
                        conditionName = condition.name,
                        iconRes = condition.getIcon(),
                        description = condition.getDescription(context),
                    )
                }
            }

    private fun TriggerCondition.getDescription(context: Context): String =
        when (this) {
            is TriggerCondition.OnBroadcastReceived -> context.getString(
                R.string.item_event_occurrence_details_trigger_desc_broadcast,
                intentAction.getIntentActionDisplayName(),
            )
            is TriggerCondition.OnCounterCountReached -> context.getString(
                R.string.item_event_occurrence_details_trigger_desc_counter,
                counterName,
                comparisonOperation.getComparisonOperationDisplayName(context),
                counterValue.value.toString(),
            )
            is TriggerCondition.OnTimerReached -> context.getString(
                R.string.item_event_occurrence_details_trigger_desc_timer,
                formatDuration(durationMs),
            )
        }

    @DrawableRes
    private fun TriggerCondition.getIcon(): Int =
        when (this) {
            is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
            is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
            is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
        }

    private fun Int.getOperatorDisplayName(context: Context) : String =
        when (this) {
            AND -> context.getString(R.string.condition_operator_and)
            OR -> context.getString(R.string.condition_operator_or)
            else -> ""
        }

    private fun String.getIntentActionDisplayName(): String {
        val lastDotIndex = lastIndexOf('.')

        return if (lastDotIndex != -1 && lastDotIndex != lastIndex) substring(lastDotIndex + 1)
        else this
    }

    private fun ComparisonOperation.getComparisonOperationDisplayName(context: Context): String =
        when (this) {
            GREATER -> context.getString(R.string.comparison_operator_greater)
            GREATER_OR_EQUALS -> context.getString(R.string.comparison_operator_greater_or_equals)
            EQUALS -> context.getString(R.string.comparison_operator_equals)
            LOWER_OR_EQUALS -> context.getString(R.string.comparison_operator_lower_or_equals)
            LOWER -> context.getString(R.string.comparison_operator_lower)
        }

    private fun Double.toDisplayConfidence(): Double =
        (this * 100).coerceIn(0.00, 100.00)

    private fun Int.toMinimumConfidence(): Double =
        100.00 - this
}

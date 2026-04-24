/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.debugging

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugLiveDetectionResultUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.debugging.R

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class LiveDebuggingViewModel @Inject constructor(
    @ApplicationContext context: Context,
    debuggingRepository: DebuggingRepository,
    debugDetectionResultUseCase: GetDebugLiveDetectionResultUseCase,
) : ViewModel() {

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = debuggingRepository.isLiveDebugging

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LiveDebuggingUiState?> = debugDetectionResultUseCase
        .invoke(displayDuration = POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
        .combine(isDebugging) { results, isDebugging -> if (isDebugging) results else null }
        .map { result -> result?.toLastPositiveDebugInfo(context) }

}

private fun DebugLiveEventOccurrence.toLastPositiveDebugInfo(context: Context): LiveDebuggingUiState =
    LiveDebuggingUiState(
        eventIcon = event.getDebugIcon(),
        eventName = event.name,
        eventFulfilledCount = fulfilledCount.toString(),
        eventDuration = context.getDurationText(processingDurationMs),
        actions = event.actions.map { action -> LiveDebuggingActionsItem(action.getIconRes()) },
    )

@DrawableRes
private fun Event.getDebugIcon(): Int =
    when (this) {
        is ImageEvent -> R.drawable.ic_condition
        is TriggerEvent -> R.drawable.ic_trigger_event
    }

private fun Context.getDurationText(durationMs: Long): String =
    "$durationMs${getString(R.string.dropdown_label_time_unit_ms)}"

/** Delay before removing the last positive result display in debug. */
private val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500.milliseconds
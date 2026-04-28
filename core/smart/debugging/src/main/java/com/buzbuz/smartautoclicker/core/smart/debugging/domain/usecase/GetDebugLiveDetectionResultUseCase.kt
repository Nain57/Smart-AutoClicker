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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase

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
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Use case providing result for live image event occurrence display.
 *
 * Each [DebugLiveEventOccurrence] are provided for a maximum of displayDuration, and then reset to null. If a new
 * image event occurs during this reset timer, the new value will be immediately emitted and the reset timer restarted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetDebugLiveDetectionResultUseCase @Inject constructor(
    private val debuggingRepository: DebuggingRepository,
) {

    private companion object {
        private val DEFAULT_RESULT_DISPLAY_DURATION = 3.seconds
    }

    operator fun invoke(
        minDisplayDuration: Duration = DEFAULT_RESULT_DISPLAY_DURATION,
        filterNotFulfilled: Boolean = true,
    ): Flow<DebugLiveEventOccurrence?> =
        debuggingRepository.lastImageEventProcessed
            .filter { results -> !filterNotFulfilled || (results?.fulfilled == true) }
            .transformLatest { results ->
                if (results == null) {
                    emit(null)
                    return@transformLatest
                }

                emit(results)
                delay(results.getDisplayDurationMs(minDisplayDuration))
                emit(null)
            }

    private fun DebugLiveEventOccurrence.getDisplayDurationMs(minDisplayDuration: Duration): Long =
        (processingDurationMs + event.actions.getDurationMs())
            .coerceAtLeast(minDisplayDuration.inWholeMilliseconds)


    private fun List<Action>.getDurationMs(): Long =
        fold(initial = 0) { acc, action ->
            acc + when (action) {
                is Click -> action.pressDuration ?: 0
                is Swipe -> action.swipeDuration ?: 0
                is Pause -> action.pauseDuration ?: 0
                is ChangeCounter,
                is Intent,
                is Notification,
                is SetText,
                is SystemAction,
                is ToggleEvent -> 0
            }
        }
}
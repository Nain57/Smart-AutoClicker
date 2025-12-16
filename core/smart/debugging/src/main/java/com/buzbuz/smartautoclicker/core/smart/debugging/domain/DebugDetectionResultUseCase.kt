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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveImageEventOccurrence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Use case providing result for live image event occurrence display.
 *
 * Each [DebugLiveImageEventOccurrence] are provided for a maximum of displayDuration, and then reset to null. If a new
 * image event occurs during this reset timer, the new value will be immediately emitted and the reset timer restarted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugDetectionResultUseCase @Inject constructor(
    private val debuggingRepository: DebuggingRepository,
) {

    private companion object {
        private val DEFAULT_RESULT_DISPLAY_DURATION = 3.seconds
    }

    operator fun invoke(displayDuration: Duration = DEFAULT_RESULT_DISPLAY_DURATION): Flow<DebugLiveImageEventOccurrence?> =
        debuggingRepository.lastImageEventFulfilled
            .transformLatest { results ->
                if (results == null) {
                    emit(null)
                    return@transformLatest
                }

                emit(results)
                delay(3.seconds)
                emit(null)
            }
}
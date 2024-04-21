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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.debugging.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.formatConfidenceRate

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** ViewModel for the debug features. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugModel @Inject constructor(
    @ApplicationContext context: Context,
    debuggingRepository: DebuggingRepository
) : ViewModel() {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getDebugConfigPreferences()

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = debuggingRepository.isDebugging.map { debugging ->
        debugging && sharedPreferences.getIsDebugViewEnabled(context)
    }

    /** The coordinates of the last positive detection. */
    val debugLastPositiveCoordinates: Flow<Rect> = debuggingRepository.lastResult
        .map { debugInfo ->
            if (debugInfo != null && debugInfo.isDetected) debugInfo.conditionArea
            else Rect()
        }

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LastPositiveDebugInfo> = debuggingRepository.lastPositiveInfo
        .flatMapLatest { debugInfo ->
            flow {
                if (debugInfo == null) {
                    emit(LastPositiveDebugInfo())
                    return@flow
                }

                emit(
                    LastPositiveDebugInfo(
                        debugInfo.event.name,
                        debugInfo.condition.name,
                        debugInfo.confidenceRate.formatConfidenceRate(),
                    )
                )

                delay(POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
                emit(LastPositiveDebugInfo())
            }
        }
}

/**
 * Info on the last positive detection.
 * @param eventName name of the event
 * @param conditionName the name of the condition detected.
 * @param confidenceRateText the text to display for the confidence rate
 */
data class LastPositiveDebugInfo(
    val eventName: String = "",
    val conditionName: String = "",
    val confidenceRateText: String = "",
)

/** Delay before removing the last positive result display in debug. */
private const val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500L
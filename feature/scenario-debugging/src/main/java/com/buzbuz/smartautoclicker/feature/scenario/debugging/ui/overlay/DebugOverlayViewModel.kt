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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.overlay

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.feature.scenario.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.scenario.debugging.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.debugging.getIsDebugViewEnabled
import com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report.formatConfidenceRate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/** ViewModel for the debug features. */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugModel(application: Application) : AndroidViewModel(application) {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getDebugConfigPreferences()
    /** The debugging engine. */
    private var repository: DebuggingRepository = DebuggingRepository.getDebuggingRepository(application)

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = repository.isDebugging.map { debugging ->
        debugging && sharedPreferences.getIsDebugViewEnabled(application)
    }

    /** The coordinates of the last positive detection. */
    val debugLastPositiveCoordinates: Flow<Rect> = repository.lastResult
        .map { debugInfo ->
            if (debugInfo != null && debugInfo.isDetected) debugInfo.conditionArea
            else Rect()
        }

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LastPositiveDebugInfo> = repository.lastPositiveInfo
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
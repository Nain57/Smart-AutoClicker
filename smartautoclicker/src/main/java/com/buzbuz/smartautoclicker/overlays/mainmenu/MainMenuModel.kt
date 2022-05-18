/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.mainmenu

import android.content.Context
import android.graphics.Rect

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.engine.DetectorEngine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample

/**
 * View model for the [MainMenu].
 * @param context the Android context.
 */
class MainMenuModel(context: Context) : OverlayViewModel(context) {

    /** The detector engine. */
    private var detectorEngine: DetectorEngine = DetectorEngine.getDetectorEngine(context)
    /** The repository for the scenarios. */
    private var repository: Repository? = Repository.getRepository(context)
    /** The current of the detection. */
    val detectionState: Flow<Boolean> = detectorEngine.isDetecting
    /** The current list of event in the detector engine. */
    val eventList: Flow<List<Event>?> = detectorEngine.scenarioEvents
    /** Tells if the current detection is running in debug mode. */
    val isDebugging = detectorEngine.isDebugging

    /** The confidence rate on the last detection, positive or negative. */
    @OptIn(FlowPreview::class)
    val debugLastConfidenceRate: Flow<String?> = detectorEngine.debugEngine.lastResult
        .sample(CONFIDENCE_RATE_SAMPLING_TIME_MS)
        .map { lastDebugInfo ->
            lastDebugInfo.detectionResult.confidenceRate.formatConfidenceRate()
        }

    /** The info on the last positive detection. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val debugLastPositive: Flow<LastPositiveDebugInfo> = detectorEngine.debugEngine.lastPositiveInfo
        .flatMapLatest { debugInfo ->
            flow {
                emit(LastPositiveDebugInfo(
                    debugInfo.event.name,
                    debugInfo.condition.name,
                    debugInfo.detectionResult.confidenceRate.formatConfidenceRate(),
                ))

                delay(POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
                emit(LastPositiveDebugInfo())
            }
        }

    /** The coordinates of the last positive detection. */
    val debugLastPositiveCoordinates: Flow<Rect> = detectorEngine.debugEngine.lastResult
        .map { debugInfo ->
            if (debugInfo.detectionResult.isDetected) debugInfo.conditionArea
            else Rect()
        }

    /** Start/Stop the detection. */
    fun toggleDetection(debugMode: Boolean = false) {
        detectorEngine.apply {
            if (isDetecting.value) {
                stopDetection()
            } else {
                startDetection(debugMode)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.cleanCache()
        repository = null
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

/** Format this value as a displayable confidence rate. */
private fun Double.formatConfidenceRate(): String = "${String.format("%.2f", this * 100)} % "

/** Delay before removing the last positive result display in debug. */
private const val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500L
/** Sampling on the current confidence rate for the display. */
private const val CONFIDENCE_RATE_SAMPLING_TIME_MS = 450L
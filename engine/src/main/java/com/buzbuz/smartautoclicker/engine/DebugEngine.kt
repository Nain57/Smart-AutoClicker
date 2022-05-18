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
package com.buzbuz.smartautoclicker.engine

import android.graphics.Rect

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.detection.DetectionResult

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.MutableSharedFlow

/** Engine for the debugging of a scenario processing. */
class DebugEngine {

    /** The DebugInfo for the current image. */
    private val currentInfo = MutableSharedFlow<DebugInfo>()

    /** The DebugInfo for the current image. */
    val lastResult = currentInfo
    /** The DebugInfo for the last positive detection. */
    val lastPositiveInfo = currentInfo
        .filter { it.detectionResult.isDetected }

    /**
     * Publish a new detection result debug info.
     * @param event the event checked.
     * @param condition the condition checked.
     * @param result the result of the detection.
     */
    internal suspend fun onNewDetectionResult(event: Event, condition: Condition, result: DetectionResult) {
        val halfWidth = condition.area.width() / 2
        val halfHeight = condition.area.height() / 2

        val coordinates =
            if (result.position.x == 0 && result.position.y == 0) Rect()
            else Rect(
                result.position.x - halfWidth,
                result.position.y - halfHeight,
                result.position.x + halfWidth,
                result.position.y + halfHeight
            )

        currentInfo.emit(DebugInfo(event, condition, result, coordinates))
    }

    /** Clear the values in the engine. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        currentInfo.resetReplayCache()
    }
}

/** Debug information for the scenario processing/ */
data class DebugInfo(
    val event: Event,
    val condition: Condition,
    val detectionResult: DetectionResult,
    val conditionArea: Rect,
)
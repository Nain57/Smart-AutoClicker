/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.domain.trying

import android.util.Log
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.domain.ConditionResult
import com.buzbuz.smartautoclicker.core.processing.domain.ScreenConditionResult
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener

internal class ImageConditionProcessingTryListener(
    private val triedItem: ImageConditionTry,
    private val clientListener: (ScreenConditionResult) -> Unit,
) : ScenarioProcessingListener {

    override suspend fun onScreenEventProcessingStarted(event: ScreenEvent) {
        Log.d(TAG, "onImageEventProcessingStarted: $event")
    }

    override suspend fun onScreenConditionProcessingStarted(condition: ScreenCondition) {
        Log.d(TAG, "onImageConditionProcessingStarted: $condition")
    }

    override suspend fun onScreenConditionProcessingCompleted(result: ConditionResult) {
        if (result !is ScreenConditionResult) return
        if (result.condition.id != triedItem.condition.id) return

        Log.d(TAG, "Image Processing completed: $result")
        clientListener(result)
    }
}

private const val TAG = "ProcessingTryListener"
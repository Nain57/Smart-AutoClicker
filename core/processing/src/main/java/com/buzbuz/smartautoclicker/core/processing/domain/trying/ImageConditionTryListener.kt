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
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.processing.domain.ConditionResult
import com.buzbuz.smartautoclicker.core.processing.domain.ImageConditionResult
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener

internal class ImageConditionProcessingTryListener(
    private val triedItem: ImageConditionTry,
    private val clientListener: (ImageConditionResult) -> Unit,
) : ScenarioProcessingListener {

    override suspend fun onImageEventProcessingStarted(event: ImageEvent) {
        Log.d(TAG, "onImageEventProcessingStarted: $event")
    }

    override suspend fun onImageConditionProcessingStarted(condition: ImageCondition) {
        Log.d(TAG, "onImageConditionProcessingStarted: $condition")
    }

    override suspend fun onImageConditionProcessingCompleted(result: ConditionResult) {
        if (result !is ImageConditionResult) return
        if (result.condition.id != triedItem.condition.id) return

        Log.d(TAG, "Image Processing completed: $result")
        clientListener(result)
    }
}

private const val TAG = "ProcessingTryListener"
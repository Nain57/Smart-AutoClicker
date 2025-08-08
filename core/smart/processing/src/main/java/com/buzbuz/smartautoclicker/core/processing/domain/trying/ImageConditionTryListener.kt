
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
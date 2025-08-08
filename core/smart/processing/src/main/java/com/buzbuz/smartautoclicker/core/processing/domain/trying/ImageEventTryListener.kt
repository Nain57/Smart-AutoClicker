
package com.buzbuz.smartautoclicker.core.processing.domain.trying

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.processing.domain.IConditionsResult
import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener

internal class ImageEventProcessingTryListener(
    private val triedItem: ImageEventTry,
    private val clientListener: (IConditionsResult) -> Unit,
) : ScenarioProcessingListener {

    override suspend fun onImageEventProcessingCompleted(event: ImageEvent, results: IConditionsResult) {
        if (event.id != triedItem.event.id) return
        clientListener(results)
    }
}
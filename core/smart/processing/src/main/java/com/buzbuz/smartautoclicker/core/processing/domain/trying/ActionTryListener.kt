
package com.buzbuz.smartautoclicker.core.processing.domain.trying

import com.buzbuz.smartautoclicker.core.processing.domain.ScenarioProcessingListener

internal class ActionTryListener(
    private val clientListener: () -> Unit,
) : ScenarioProcessingListener {

    override suspend fun onSessionEnded() {
        clientListener()
    }
}

private const val TAG = "ProcessingTryListener"
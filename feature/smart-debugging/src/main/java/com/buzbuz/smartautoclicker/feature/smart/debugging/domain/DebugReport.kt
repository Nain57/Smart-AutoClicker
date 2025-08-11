
package com.buzbuz.smartautoclicker.feature.smart.debugging.domain

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

data class DebugReport(
    val scenario: Scenario,
    val sessionInfo: ProcessingDebugInfo,
    val imageProcessedInfo: ProcessingDebugInfo,
    val eventsTriggeredCount: Long,
    val eventsProcessedInfo: List<Pair<ImageEvent, ProcessingDebugInfo>>,
    val conditionsDetectedCount: Long,
    val conditionsProcessedInfo: Map<Long, Pair<ImageCondition, ConditionProcessingDebugInfo>>,
)

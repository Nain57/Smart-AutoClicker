
package com.buzbuz.smartautoclicker.core.processing.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

interface ScenarioProcessingListener {

    suspend fun onSessionStarted(
        context: Context,
        scenario: Scenario,
        imageEvents: List<ImageEvent>,
        triggerEvents: List<TriggerEvent>,
    ) = Unit

    suspend fun onTriggerEventProcessingStarted(event: TriggerEvent) = Unit
    suspend fun onTriggerEventProcessingCompleted(event: TriggerEvent, results: List<ConditionResult>) = Unit

    suspend fun onImageEventsProcessingStarted() = Unit

    suspend fun onImageEventProcessingStarted(event: ImageEvent) = Unit

    suspend fun onImageConditionProcessingStarted(condition: ImageCondition) = Unit
    suspend fun onImageConditionProcessingCompleted(result: ConditionResult) = Unit
    suspend fun onImageConditionProcessingCancelled() = Unit

    suspend fun onImageEventProcessingCompleted(event: ImageEvent, results: IConditionsResult) = Unit
    suspend fun onImageEventProcessingCancelled() = Unit

    suspend fun onImageEventsProcessingCompleted() = Unit

    suspend fun onSessionEnded() = Unit
}
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
package com.buzbuz.smartautoclicker.core.processing.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario


interface ScenarioProcessingListener {

    suspend fun onSessionStarted(
        context: Context,
        scenario: Scenario,
        screenEvents: List<ScreenEvent>,
        triggerEvents: List<TriggerEvent>,
    ) = Unit

    suspend fun onTriggerEventProcessingStarted(event: TriggerEvent) = Unit
    suspend fun onTriggerEventProcessingCompleted(event: TriggerEvent, results: List<ConditionResult>) = Unit

    suspend fun onScreenEventsProcessingStarted() = Unit

    suspend fun onScreenEventProcessingStarted(event: ScreenEvent) = Unit

    suspend fun onScreenConditionProcessingStarted(condition: ScreenCondition) = Unit
    suspend fun onScreenConditionProcessingCompleted(result: ConditionResult) = Unit
    suspend fun onScreenConditionProcessingCancelled() = Unit

    suspend fun onScreenEventProcessingCompleted(event: ScreenEvent, results: IConditionsResult) = Unit
    suspend fun onScreenEventProcessingCancelled() = Unit

    suspend fun onScreenEventsProcessingCompleted() = Unit

    suspend fun onSessionEnded() = Unit
}
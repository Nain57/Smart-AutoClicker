/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.counter

import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCounterReferencesUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(): Flow<Map<String, Set<CounterReference>>> =
        editionRepository.editionState.allEditedEvents
            .map { events -> events.findCounterReferences() }

    private fun List<Event>.findCounterReferences(): Map<String, Set<CounterReference>> =
        buildMap {
            this@findCounterReferences.forEach { event ->
                event.conditions.getConditionsCounterReferences().forEach { (counterName, references) ->
                    addReferences(counterName, references)
                }
                event.actions.getActionsCounterReferences().forEach { (counterName, references) ->
                    addReferences(counterName, references)
                }
            }
        }

    private fun List<Condition>.getConditionsCounterReferences(): Map<String, Set<CounterReference>> =
        buildMap {
            this@getConditionsCounterReferences.forEach { condition ->
                when (condition) {
                    is ScreenCondition.Number -> {
                        val counterValue = condition.counterValue
                        if (counterValue is CounterOperationValue.Counter) {
                            addReference(counterValue.value, condition)
                        }
                    }

                    is TriggerCondition.OnCounterCountReached -> {
                        addReference(condition.counterName, condition)

                        val operationValue = condition.counterValue
                        if (operationValue is CounterOperationValue.Counter) {
                            addReference(operationValue.value, condition)
                        }
                    }

                    is ScreenCondition.Color,
                    is ScreenCondition.Image,
                    is ScreenCondition.Text,
                    is TriggerCondition.OnBroadcastReceived,
                    is TriggerCondition.OnTimerReached -> Unit
                }
            }
        }

    private fun List<Action>.getActionsCounterReferences(): Map<String, Set<CounterReference>> =
        buildMap {
            this@getActionsCounterReferences.forEach { action ->
                when (action) {
                    is ChangeCounter -> {
                        addReference(action.counterName, action)

                        val operationValue = action.operationValue
                        if (operationValue is CounterOperationValue.Counter) {
                            addReference(operationValue.value, action)
                        }
                    }

                    is Notification -> {
                        if (action.messageType == Notification.MessageType.COUNTER_VALUE) {
                            addReference(action.messageCounterName, action)
                        }
                    }

                    is SetText -> {
                        action.text.findCounterReferences().forEach { counterName ->
                            addReference(counterName, action)
                        }
                    }

                    is Click,
                    is Intent,
                    is Pause,
                    is SystemAction,
                    is Swipe,
                    is ToggleEvent -> Unit
                }
            }
        }

    private fun MutableMap<String, Set<CounterReference>>.addReference(counterName: String, condition: Condition) {
        put(
            counterName,
            getOrDefault(counterName, emptySet()) + CounterReference.ConditionElement(condition)
        )
    }

    private fun MutableMap<String, Set<CounterReference>>.addReference(counterName: String, action: Action) {
        put(
            counterName,
            getOrDefault(counterName, emptySet()) + CounterReference.ActionElement(action)
        )
    }

    private fun MutableMap<String, Set<CounterReference>>.addReferences(counterName: String, references: Set<CounterReference>) {
        put(
            counterName,
            getOrDefault(counterName, emptySet()) + references
        )
    }
}

/** Element within a Scenario associated to a counter. */
sealed class CounterReference {
    data class ActionElement(val action: Action): CounterReference()
    data class ConditionElement(val condition: Condition): CounterReference()
}
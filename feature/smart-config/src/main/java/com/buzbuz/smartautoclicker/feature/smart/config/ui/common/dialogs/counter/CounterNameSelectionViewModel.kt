/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.counter

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class CounterNameSelectionViewModel @Inject constructor(
    editionRepository: EditionRepository,
) : ViewModel() {

    val counterNames: Flow<Set<String>> = editionRepository.editionState.allEditedEvents
        .combine(editionRepository.editionState.editedEventActionsState) { allEditedEvents, currentActions ->
            buildSet {
                allEditedEvents.forEach { event ->
                    event.conditions.forEach { condition ->
                        if (condition is TriggerCondition.OnCounterCountReached) {
                            add(condition.counterName)
                            if (condition.counterValue is CounterOperationValue.Counter) {
                                add(condition.counterValue.value.toString())
                            }
                        }
                    }
                    event.actions.forEach { action ->
                        action.getContainedCounterNames()?.let(::addAll)
                    }
                }

                currentActions.value?.forEach { action ->
                    action.getContainedCounterNames()?.let(::addAll)
                }
            }
        }


    private fun Action.getContainedCounterNames(): Set<String>? =
        when (this) {
            is ChangeCounter -> {
                buildSet {
                    add(counterName)
                    if (operationValue is CounterOperationValue.Counter) {
                        add(operationValue.value.toString())
                    }
                }
            }
            is Notification ->
                if (messageType == Notification.MessageType.COUNTER_VALUE) setOf(messageCounterName)
                else null
            else -> null
        }
}

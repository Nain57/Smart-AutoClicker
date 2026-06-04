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
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import javax.inject.Inject

class ReplaceCounterUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(from: Counter, to: Counter) {
        editionRepository.editionState.getAllEditedEvents().forEach { event ->
            editionRepository.startEventEdition(event)
            event.conditions.forEach { condition -> condition.replaceCounter(from, to) }
            event.actions.forEach { action -> action.replaceCounter(from, to) }
            editionRepository.upsertEditedEvent()
        }
    }

    private fun Condition.replaceCounter(from: Counter, to: Counter) {
        when (this) {
            is ScreenCondition.Number -> {
                if (counterValue is CounterOperationValue.Counter && counterValue.value == from.counterName) {
                    editionRepository.startConditionEdition(this)
                    editionRepository.updateEditedCondition(
                        this.copy(counterValue = CounterOperationValue.Counter(to.counterName))
                    )
                    editionRepository.upsertEditedCondition()
                }
            }

            is TriggerCondition.OnCounterCountReached -> {
                var newCondition: Condition = this
                if (counterName == from.counterName) {
                    newCondition = newCondition.copy(counterName = to.counterName)
                }
                if (counterValue is CounterOperationValue.Counter && counterValue.value == from.counterName) {
                    newCondition = newCondition.copy(counterValue = CounterOperationValue.Counter(to.counterName))
                }

                if (newCondition != this) {
                    editionRepository.startConditionEdition(this)
                    editionRepository.updateEditedCondition(newCondition)
                    editionRepository.upsertEditedCondition()
                }
            }

            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnTimerReached -> Unit
        }
    }

    private fun Action.replaceCounter(from: Counter, to: Counter) {
        when (this) {
            is ChangeCounter -> {
                var newAction: Action = this
                if (counterName == from.counterName) {
                    newAction = newAction.copy(counterName = to.counterName)
                }
                if (operationValue is CounterOperationValue.Counter && operationValue.value == from.counterName) {
                    newAction = newAction.copy(operationValue = CounterOperationValue.Counter(to.counterName))
                }

                if (newAction != this) {
                    editionRepository.startActionEdition(this)
                    editionRepository.updateEditedAction(newAction)
                    editionRepository.upsertEditedAction()
                }
            }

            is Notification -> {
                if (messageType == Notification.MessageType.COUNTER_VALUE && messageCounterName == from.counterName) {
                    editionRepository.startActionEdition(this)
                    editionRepository.updateEditedAction(this.copy(messageCounterName = to.counterName))
                    editionRepository.upsertEditedAction()
                }
            }

            is SetText -> {
                if (text.findCounterReferences().contains(from.counterName)) {
                    editionRepository.startActionEdition(this)
                    editionRepository.updateEditedAction(
                        this.copy(text = text.replace("{${from.counterName}}", "{${to.counterName}}"))
                    )
                    editionRepository.upsertEditedAction()
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
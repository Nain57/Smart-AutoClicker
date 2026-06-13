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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import javax.inject.Inject


/** For a given Condition, get all possible missing references to another item in the current scenario. */
class GetConditionMissingReferencesUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(
        condition: Condition,
        eventsToCopy: List<Event> = emptyList(),
    ): ItemWithMissingReferences.ConditionItem {
        val missingReferences = when (condition) {
            is ScreenCondition.Number -> condition.getMissingReferences()
            is TriggerCondition.OnCounterCountReached -> condition.getMissingReferences()

            // Nothing is referenced in those conditions
            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnTimerReached -> emptyList()
        }

        return ItemWithMissingReferences.ConditionItem(
            item = condition,
            missingReferences = missingReferences,
        )
    }

    private fun ScreenCondition.Number.getMissingReferences(): List<MissingCopyReference> =
        buildList {
            if (counterValue is CounterOperationValue.Counter) {
                val counterName = (counterValue as CounterOperationValue.Counter).value
                if (editionRepository.editionState.getCounter(counterName) == null) {
                    add(MissingCopyReference.CounterReference(counterName))
                }
            }
        }

    private fun TriggerCondition.OnCounterCountReached.getMissingReferences(): List<MissingCopyReference> =
        buildList {
            if (editionRepository.editionState.getCounter(counterName) == null) {
                add(MissingCopyReference.CounterReference(counterName))
            }

            if (counterValue is CounterOperationValue.Counter) {
                val valueCounterName = (counterValue as CounterOperationValue.Counter).value
                if (editionRepository.editionState.getCounter(valueCounterName) == null) {
                    add(MissingCopyReference.CounterReference(valueCounterName))
                }
            }
        }
}

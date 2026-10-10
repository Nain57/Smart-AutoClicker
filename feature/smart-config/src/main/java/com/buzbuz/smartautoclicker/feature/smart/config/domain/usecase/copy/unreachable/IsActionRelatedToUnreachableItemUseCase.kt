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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.unreachable

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureCoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GesturePosition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import javax.inject.Inject

class IsActionRelatedToUnreachableItemUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(action: Action, eventsToCopy: List<Event> = emptyList()): Boolean {
        // We want to check for conflict on the resulting list, allowing cross-referenced items within the same copy
        val copyResultEvents: Map<Identifier, Event> = buildMap {
            putAll(editionRepository.editionState.getAllEditedEvents().map { event -> event.id to event })
            putAll(eventsToCopy.map { event -> event.id to event })
        }

        return when (action) {
            is ChangeCounter -> action.isRelatedToUnreachableItem()
            is Click -> action.isRelatedToUnreachableItem(copyResultEvents)
            is Gesture -> action.isRelatedToUnreachableItem(copyResultEvents)
            is Notification -> action.isRelatedToUnreachableItem()
            is SetText -> action.isRelatedToUnreachableItem()
            is ToggleEvent -> action.isRelatedToUnreachableItem(copyResultEvents)

            // Nothing is referenced in those actions
            is Intent,
            is Pause,
            is SystemAction -> false
        }
    }

    private fun ChangeCounter.isRelatedToUnreachableItem(): Boolean {
        if (editionRepository.counterIsUnreachable(counterName)) return true

        return when (val ctnValue = operationValue) {
            is CounterOperationValue.Counter -> editionRepository.counterIsUnreachable(ctnValue.value)
            is CounterOperationValue.Number -> false
        }
    }

    private fun Click.isRelatedToUnreachableItem(copyResultEvents: Map<Identifier, Event>): Boolean {
        (position as? GesturePosition.Condition)?.conditionId?.let { conditionId ->
            return copyResultEvents[eventId]?.conditions
                ?.find { condition -> condition.id == conditionId } == null
        }

        return false
    }

    private fun Notification.isRelatedToUnreachableItem(): Boolean {
        messageText.findCounterReferences().forEach { counterName ->
            if (editionRepository.counterIsUnreachable(counterName)) return true
        }

        return false
    }

    private fun SetText.isRelatedToUnreachableItem(): Boolean {
        text.findCounterReferences().forEach { counterName ->
            if (editionRepository.counterIsUnreachable(counterName)) return true
        }

        return false
    }

    private fun Gesture.isRelatedToUnreachableItem(copyResultEvents: Map<Identifier, Event>): Boolean {
        for (finger in fingers) {
            for (motion in finger.motions) {
                val positions: List<GesturePosition> = when (motion) {
                    is GestureFingerMotion.Press -> listOf(motion.position, motion.offset)
                    is GestureFingerMotion.Move -> listOf(motion.position, motion.offset)
                    is GestureFingerMotion.Wait,
                    is GestureFingerMotion.Release -> emptyList()
                }

                for (position in positions) {
                    when (position) {
                        is GesturePosition.Condition -> {
                            position.conditionId?.let { conditionId ->
                                if (copyResultEvents[eventId]?.conditions?.any { it.id == conditionId } != true) return true
                            }
                        }
                        is GesturePosition.Coordinates -> {
                            for (coordValue in listOf(position.x, position.y)) {
                                if (coordValue is GestureCoordinatesValue.Counter) {
                                    if (editionRepository.counterIsUnreachable(coordValue.counterName)) return true
                                }
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private fun ToggleEvent.isRelatedToUnreachableItem(copyResultEvents: Map<Identifier, Event>): Boolean {
        if (toggleAll) return false

        eventToggles.forEach { toggle ->
            if (!copyResultEvents.contains(toggle.targetEventId)) return true
        }

        return false
    }

    private fun EditionRepository.counterIsUnreachable(counterName: String): Boolean =
        editionState.getCounter(counterName) == null
}
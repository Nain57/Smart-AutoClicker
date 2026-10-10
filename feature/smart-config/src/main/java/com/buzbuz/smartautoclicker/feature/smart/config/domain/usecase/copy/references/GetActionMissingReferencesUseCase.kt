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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.domain.IRepository
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
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference

import javax.inject.Inject


/**
 * For a given Action, get all possible missing references to another item in the current scenario.
 */
class GetActionMissingReferencesUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
    private val smartRepository: IRepository,
) {

    suspend operator fun invoke(
        action: Action,
        eventsToCopy: List<Event> = emptyList(),
    ): ItemWithMissingReferences.ActionItem {

        // We want to check for conflict on the resulting list, allowing cross-referenced items within the same copy
        val copyResultEvents: Map<Identifier, Event> = buildMap {
            putAll(editionRepository.editionState.getAllEditedEvents().map { event -> event.id to event })
            putAll(eventsToCopy.map { event -> event.id to event })
        }

        val missingReferences = when (action) {
            is ChangeCounter -> action.getMissingReferences()
            is Click -> action.getMissingReferences(copyResultEvents)
            is Gesture -> action.getMissingReferences(copyResultEvents)
            is Notification -> action.getMissingReferences()
            is SetText -> action.getMissingReferences()
            is ToggleEvent -> action.getMissingReferences(copyResultEvents)

            // Nothing is referenced in those actions
            is Intent,
            is Pause,
            is SystemAction -> emptyList()
        }

        return ItemWithMissingReferences.ActionItem(
            item = action,
            missingReferences = missingReferences,
        )
    }

    private fun ChangeCounter.getMissingReferences(): List<MissingCopyReference> =
        buildList {
            if (editionRepository.editionState.getCounter(counterName) == null) {
                add(MissingCopyReference.CounterReference(counterName))
            }

            if (operationValue is CounterOperationValue.Counter) {
                val valueCounterName = (operationValue as CounterOperationValue.Counter).value
                if (editionRepository.editionState.getCounter(valueCounterName) == null) {
                    add(MissingCopyReference.CounterReference(valueCounterName))
                }
            }
        }

    private suspend fun Click.getMissingReferences(copyResultEvents: Map<Identifier, Event>): List<MissingCopyReference> =
        listOfNotNull(position, offset).flatMap { clickPosition ->
            clickPosition.getMissingReference(copyResultEvents, eventId)
        }

    private suspend fun Gesture.getMissingReferences(copyResultEvents: Map<Identifier, Event>): List<MissingCopyReference> {
        val result = mutableListOf<MissingCopyReference>()
        fingers.forEach { finger ->
            finger.motions.forEach { motion ->
                val positions: List<Position> = when (motion) {
                    is GestureFingerMotion.Press -> listOf(motion.position, motion.offset)
                    is GestureFingerMotion.Move -> listOf(motion.position, motion.offset)
                    is GestureFingerMotion.Wait,
                    is GestureFingerMotion.Release -> emptyList()
                }

                positions.forEach { position ->
                    result.addAll(position.getMissingReference(copyResultEvents, eventId))
                }
            }
        }

        return result
    }

    private suspend fun Position.getMissingReference(
        copyResultEvents: Map<Identifier, Event>,
        eventId: Identifier,
    ): List<MissingCopyReference> {
        when (this) {
            is Position.Condition -> {
                val condId = conditionId ?: return emptyList()
                val isFound = copyResultEvents[eventId]?.conditions?.any { it.id == condId } == true
                if (isFound) return emptyList()

                // The referenced condition can belong to another edited event not yet saved in database
                val name = copyResultEvents.values
                    .firstNotNullOfOrNull { event -> event.conditions.find { it.id == condId }?.name }
                    ?: smartRepository.getConditionName(condId)
                    ?: "Unknown"

                return listOf(MissingCopyReference.ScreenConditionReference(name = name, conditionId = condId))
            }

            is Position.Coordinates -> {
                val result = mutableListOf<MissingCopyReference>()
                listOf(x, y).forEach { coordinatesValue ->
                    if (coordinatesValue is CoordinatesValue.Counter) {
                        if (editionRepository.editionState.getCounter(coordinatesValue.counterName) == null) {
                            result.add(MissingCopyReference.CounterReference(coordinatesValue.counterName))
                        }
                    }
                }

                return result
            }
        }
    }

    private fun Notification.getMissingReferences(): List<MissingCopyReference> =
        messageText.findCounterReferences()
            .filter { counterName -> editionRepository.editionState.getCounter(counterName) == null }
            .map { counterName -> MissingCopyReference.CounterReference(counterName) }

    private fun SetText.getMissingReferences(): List<MissingCopyReference> =
        text.findCounterReferences()
            .filter { counterName -> editionRepository.editionState.getCounter(counterName) == null }
            .map { counterName -> MissingCopyReference.CounterReference(counterName) }

    private suspend fun ToggleEvent.getMissingReferences(copyResultEvents: Map<Identifier, Event>): List<MissingCopyReference> {
        if (toggleAll) return emptyList()

        // We don't want one error per toggle, as if one is valid, all them of will most likely be
        // Just take the first one found, UI forces an update in batches anyway
        val invalidToggle = eventToggles.find { toggle ->
            toggle.targetEventId?.let { targetEventId ->
                !copyResultEvents.contains(targetEventId)
            } ?: false
        } ?: return emptyList()

        val name = invalidToggle.targetEventId
            ?.let { eventId -> smartRepository.getEventName(eventId) }
            ?: "Unknown"

        return listOf(MissingCopyReference.EventToggleReference(name))
    }
}

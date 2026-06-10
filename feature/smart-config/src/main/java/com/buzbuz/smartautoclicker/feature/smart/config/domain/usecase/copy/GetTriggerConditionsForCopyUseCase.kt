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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ConditionsForCopy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.collections.plus


class GetTriggerConditionsForCopyUseCase @Inject constructor(
    @Dispatcher(IO) dispatcherIo: CoroutineDispatcher,
    editionRepository: EditionRepository,
    smartRepository: IRepository,
) {

    private val editedScenarioEventsId: Flow<List<Identifier?>> = editionRepository.editionState.allEditedEventsFlow
        .map { events -> events.map { event -> event.id } }

    private val editedEventId: Flow<Identifier?> = editionRepository.editionState.editedEventState
        .map { eventState -> eventState.value?.id }

    private val editedEventConditions: Flow<List<TriggerCondition>> = editionRepository.editionState.editedEventState
        .map { eventState ->
            val event = eventState.value as? TriggerEvent ?: return@map emptyList()
            event.conditions
        }

    private val editedScenarioConditions: Flow<List<TriggerCondition>> = editionRepository.editionState.editedTriggerEventsState
        .map { eventsState ->
            eventsState.value?.fold(initial = emptyList(), operation = { acc, event -> acc + event.conditions })
                ?: emptyList()
        }

    private val allConditions: Flow<List<TriggerCondition>> = smartRepository.allConditions
        .map { conditions -> conditions.filterIsInstance<TriggerCondition>() }
        .flowOn(dispatcherIo)


    operator fun invoke(): Flow<ConditionsForCopy<TriggerCondition>> = combine(
        editedScenarioEventsId,
        editedEventId,
        editedEventConditions,
        editedScenarioConditions,
        allConditions,
    ) { scenarioEventsId, eventId, eventConditions, scenarioConditions, allConditions ->
        ConditionsForCopy(
            thisEvent = eventConditions
                .filterConditionsForCopy(),
            thisScenario = scenarioConditions
                .filter { condition -> condition.eventId != eventId }
                .filterConditionsForCopy(),
            otherScenario = allConditions
                .filter { condition -> !scenarioEventsId.contains(condition.eventId) }
                .filterConditionsForCopy(),
        )
    }

    private fun List<TriggerCondition>.filterConditionsForCopy(): List<TriggerCondition> =
        filter { condition ->
            when {
                // Remove invalid
                !condition.isComplete() -> false
                // Ok for copy
                else -> true
            }
        }
}
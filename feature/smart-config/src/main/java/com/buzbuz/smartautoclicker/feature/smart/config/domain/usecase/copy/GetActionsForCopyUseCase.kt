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
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ActionsForCopy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class GetActionsForCopyUseCase @Inject constructor(
    @Dispatcher(IO) dispatcherIo: CoroutineDispatcher,
    editionRepository: EditionRepository,
    smartRepository: IRepository,
) {

    private val editedScenarioEventsId: Flow<List<Identifier?>> = editionRepository.editionState.allEditedEventsFlow
        .map { events -> events.map { event -> event.id } }

    private val editedEventId: Flow<Identifier?> = editionRepository.editionState.editedEventState
        .map { eventState -> eventState.value?.id }

    private val editedEventActions: Flow<List<Action>> = editionRepository.editionState.editedEventState
        .map { eventState -> eventState.value?.actions ?: emptyList() }

    private val editedScenarioActions: Flow<List<Action>> = editionRepository.editionState.allEditedEventsFlow
        .map { events -> events.fold(initial = emptyList(), operation = { acc, current -> acc + current.actions }) }

    private val allActions: Flow<List<Action>> = smartRepository.allActions
        .flowOn(dispatcherIo)


    operator fun invoke(): Flow<ActionsForCopy> = combine(
        editedScenarioEventsId,
        editedEventId,
        editedEventActions,
        editedScenarioActions,
        allActions,
    ) { scenarioEventsId, eventId, eventActions, scenarioActions, allActions ->
        ActionsForCopy(
            thisEvent = eventActions
                .filterActionsForCopy(),
            thisScenario = scenarioActions
                .filter { action -> action.eventId != eventId }
                .filterActionsForCopy(),
            otherScenario = allActions
                .filter { action -> !scenarioEventsId.contains(action.eventId) }
                .filterActionsForCopy(),
        )
    }

    private fun List<Action>.filterActionsForCopy(): List<Action> =
        filter { action ->
            when {
                // Remove invalid
                !action.isComplete() -> false
                // Ok for copy
                else -> true
            }
        }
}
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
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IsScreenEventCopyAvailableUseCase @Inject constructor(
    @Dispatcher(IO) dispatcherIo: CoroutineDispatcher,
    editionRepository: EditionRepository,
    smartRepository: IRepository,
) {

    private val haveEditedEvents: Flow<Boolean> = editionRepository.editionState.editedScreenEventsState
        .map { editedEventsState -> editedEventsState.value?.isNotEmpty() ?: false }

    private val haveDatabaseEvents: Flow<Boolean> = smartRepository.screenEventsCount
        .flowOn(dispatcherIo)
        .map { eventCount ->  eventCount > 0 }

    operator fun invoke(): Flow<Boolean> =
        combine(haveEditedEvents, haveDatabaseEvents) { haveEdited, haveDatabase ->
            haveEdited || haveDatabase
        }

}
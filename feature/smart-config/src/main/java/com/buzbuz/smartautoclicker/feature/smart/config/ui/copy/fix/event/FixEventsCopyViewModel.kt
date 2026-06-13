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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.unreachable.IsEventRelatedToUnreachableItemUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiTriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsCopyUiState

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.map

class FixEventsCopyViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val isEventRelatedToUnreachableItemUseCase: IsEventRelatedToUnreachableItemUseCase,
) : ViewModel() {

    private val eventsToCopy: MutableStateFlow<List<Event>?> = MutableStateFlow(null)

    val uiState: StateFlow<FixEventsCopyUiState?> = eventsToCopy
        .map { events -> events.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setEventsToCopy(events: List<Event>) {
        eventsToCopy.update { events }
    }

    fun updateEvent(newEvent: Event) {
        eventsToCopy.update { old ->
            val eventIndex = old?.indexOfFirst { event -> event.id == newEvent.id } ?: return@update old
            if (eventIndex !in old.indices) return@update old

            old.toMutableList().apply {
                removeAt(eventIndex)
                add(eventIndex, newEvent)
            }
        }
    }

    fun getFixedEventsToCopy(): List<Event> =
        eventsToCopy.value ?: emptyList()

    fun getResultingEventList(): List<Event> =
        (eventsToCopy.value ?: emptyList()) + editionRepository.editionState.getAllEditedEvents()

    private fun List<Event>?.toUiState(): FixEventsCopyUiState {
        if (this == null) {
            return FixEventsCopyUiState(
                items = emptyList(),
                canBeCopied = false,
            )
        }

        var isCopyValid = true
        val items = buildList {
            add(FixCopyUiItem.Header(R.string.item_header_event_copy_fix))
            addAll(
                this@toUiState.map { event ->
                    val isEventValid = event.isComplete() && !isEventRelatedToUnreachableItemUseCase(event)
                    isCopyValid = isCopyValid && isEventValid
                    FixCopyUiItem.Item.EventItem(
                        uiEvent = event.toUiEvent(),
                        isValidForCopy = isEventValid
                    )
                }
            )
        }

        return FixEventsCopyUiState(
            items = items,
            canBeCopied = isCopyValid,
        )
    }

    private fun Event.toUiEvent(): UiEvent =
        when (this) {
            is ScreenEvent -> toUiImageEvent(false)
            is TriggerEvent -> toUiTriggerEvent(false)
        }
}
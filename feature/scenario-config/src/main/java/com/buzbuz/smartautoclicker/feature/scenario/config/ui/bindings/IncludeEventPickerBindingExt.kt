/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings

import android.view.View

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.IncludeEventPickerBinding

/** Update the ui state of the selected event. */
fun IncludeEventPickerBinding.updateState(
    viewState: EventPickerViewState,
    onPickerClicked: (List<Event>) -> Unit,
) {
    when (viewState) {
        EventPickerViewState.NoEvents -> {
            eventNone.visibility = View.VISIBLE
            eventEmpty.visibility = View.GONE
            includeSelectedEvent.root.visibility = View.GONE
        }

        is EventPickerViewState.NoSelection -> {
            eventNone.visibility = View.GONE
            eventEmpty.visibility = View.VISIBLE
            eventEmpty.setOnClickListener { onPickerClicked(viewState.availableEvents) }
            includeSelectedEvent.root.visibility = View.GONE
        }

        is EventPickerViewState.Selected -> {
            eventNone.visibility = View.GONE
            eventEmpty.visibility = View.GONE
            includeSelectedEvent.root.visibility = View.VISIBLE
            includeSelectedEvent.bind(viewState.event, false) { onPickerClicked(viewState.availableEvents) }
        }
    }
}

/** Possible states for a [IncludeEventPickerBinding]. */
sealed class EventPickerViewState {
    object NoEvents : EventPickerViewState()
    data class NoSelection(val availableEvents: List<Event>): EventPickerViewState()
    data class Selected(val event: Event, val availableEvents: List<Event>): EventPickerViewState()
}
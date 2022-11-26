/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.event.copy

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.bindings.updateState
import com.buzbuz.smartautoclicker.overlays.base.dialog.CopyDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 *
 */
class EventCopyDialog(
    context: Context,
    private val scenarioId: Long,
    private val events: List<Event>,
    private val onEventSelected: (Event) -> Unit,
) : CopyDialog(context) {

    /** View model for this content. */
    private val viewModel: EventCopyModel by lazy { ViewModelProvider(this).get(EventCopyModel::class.java) }
    /** Adapter displaying the list of events. */
    private lateinit var eventCopyAdapter: EventCopyAdapter

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setItemsFromContainer(events)

        eventCopyAdapter = EventCopyAdapter { selectedEvent ->
            viewModel.let {
                onEventSelected(it.getCopyEvent(scenarioId, selectedEvent))
                destroy()
            }
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = eventCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventList.collect(::updateEventList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateEventList(newItems: List<EventCopyModel.EventCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        eventCopyAdapter.submitList(newItems)
    }
}
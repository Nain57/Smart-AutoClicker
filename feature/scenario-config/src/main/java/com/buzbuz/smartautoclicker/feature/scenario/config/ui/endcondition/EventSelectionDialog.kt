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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.endcondition

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.bind

import com.google.android.material.bottomsheet.BottomSheetDialog


class EventSelectionDialog(
    private val eventList: List<Event>,
    private val onEventClicked: (Event) -> Unit,
): OverlayDialog(R.style.ScenarioConfigTheme) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Adapter for the list of events. */
    private val eventsAdapter = EventsAdapter(::onEventSelected)

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_event_selection)
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { debounceUserInteraction { back() } }
            }
        }

        viewBinding.layoutLoadableList.apply {
            setEmptyText(R.string.message_empty_event_list)
            list.apply {
                adapter = eventsAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.layoutLoadableList.updateState(eventList)
        eventsAdapter.submitList(eventList)
    }

    /**
     * Called when an event is selected in the list.
     * Notify the provided listener and dismiss the dialog.
     *
     * @param event the selected event.
     */
    private fun onEventSelected(event: Event) {
        debounceUserInteraction {
            onEventClicked(event)
            back()
        }
    }
}

/**
 * Adapter for the list of events.
 * @param onEventSelected listener on user click on an event.
 */
private class EventsAdapter(
    private val onEventSelected: (Event) -> Unit,
) : ListAdapter<Event, EventViewHolder>(EventsDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder =
        EventViewHolder(
            ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEventSelected,
        )

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) =
        holder.onBind(getItem(position))
}

/** DiffUtil Callback comparing two EndConditionListItem when updating the [EventsAdapter] list. */
private object EventsDiffUtilCallback: DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
}

/**
 * ViewHolder for an Event.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onEventSelected called when the user select an event.
 */
private class EventViewHolder(
    private val viewBinding: ItemEventBinding,
    private val onEventSelected: (Event) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(event: Event) {
        viewBinding.bind(event, false) { onEventSelected(event) }
    }
}
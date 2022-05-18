/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.scenariosettings.endcondition

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.databinding.DialogEndConditionEventSelectBinding
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.scenariosettings.EndConditionAdapter
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog
import com.buzbuz.smartautoclicker.overlays.utils.bindEvent

/**
 * Display the list of selectable events for a end condition.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param eventList the list of selectable events for the end condition.
 * @param onEventClicked called when the user select an event.
 */
class EndConditionEventSelectionDialog(
    context: Context,
    private val eventList: List<Event>,
    private val onEventClicked: (Event) -> Unit,
): LoadableListDialog(context) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEndConditionEventSelectBinding
    /** Adapter for the list of events. */
    private val eventsAdapter = EndConditionEventsAdapter(::onEventSelected)

    override val emptyTextId: Int = R.string.dialog_event_list_no_events

    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEndConditionEventSelectBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_end_condition_event_select_title)
            .setView(viewBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        updateLayoutState(eventList)
        listBinding.list.apply{
            adapter = eventsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        eventsAdapter.submitList(eventList)
    }

    /**
     * Called when an event is selected in the list.
     * Notify the provided listener and dismiss the dialog.
     *
     * @param event the selected event.
     */
    private fun onEventSelected(event: Event) {
        onEventClicked(event)
        dismiss()
    }
}

/**
 * Adapter for the list of events.
 * @param onEventSelected listener on user click on an event.
 */
private class EndConditionEventsAdapter(
    private val onEventSelected: (Event) -> Unit,
) : ListAdapter<Event, EndConditionEventViewHolder>(EndConditionEventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EndConditionEventViewHolder =
        EndConditionEventViewHolder(
            ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEventSelected,
        )

    override fun onBindViewHolder(holder: EndConditionEventViewHolder, position: Int) =
        holder.onBind(getItem(position))
}

/** DiffUtil Callback comparing two EndConditionListItem when updating the [EndConditionAdapter] list. */
private object EndConditionEventDiffUtilCallback: DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id != 0L && oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
}

/**
 * ViewHolder for an Event.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onEventSelected called when the user select an event.
 */
private class EndConditionEventViewHolder(
    private val viewBinding: ItemEventBinding,
    private val onEventSelected: (Event) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {
    fun onBind(event: Event) {
        viewBinding.apply {
            bindEvent(event = event, itemClickedListener = onEventSelected)
            btnAction.isClickable = false
        }
    }
}
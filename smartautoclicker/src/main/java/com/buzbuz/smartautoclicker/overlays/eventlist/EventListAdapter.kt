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
package com.buzbuz.smartautoclicker.overlays.eventlist

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.utils.bindEvent

import java.util.Collections

/**
 * Adapter displaying a list of clicks.
 *
 * This adapter supports different display mode through it's [mode] member:
 *  - [EDITION]: clicking on an item calls [itemClickedListener], and the delete button is shown.
 *  - [REORDER]: user can't clicks on an item, items can be reordered through drag and drop, the delete button is
 *               replaced by the drag and drop icon.
 *
 * @param itemClickedListener listener called when the user clicks on an click item when in [EDITION].
 * @param deleteClickedListener listener called when the user clicks on the delete button on a click item.
 */
class EventListAdapter(
    private val itemClickedListener: (Event) -> Unit,
    private val deleteClickedListener: ((Event) -> Unit)
) : RecyclerView.Adapter<EventViewHolder>() {

    /** The list of events displayed by this adapter. */
    var events: List<Event>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    /**
     * Original position of the events when entering the [REORDER] mode.
     * If [cancelReorder] is called, this list will be used to restore the original positions before user changes.
     */
    var backupEvents: List<Event>? = null
    /** Set the Ui mode for the adapter. This will trigger a refresh of the list. */
    @Mode
    var mode: Int = EDITION
        set(value) {
            field = value
            backupEvents = if (value == REORDER) events?.toMutableList() else null
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = events?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder =
        EventViewHolder(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bindEvent(events!![position], mode, itemClickedListener, deleteClickedListener)
    }

    /**
     * Swap the position of two events in the list.
     * If the ui is not in [REORDER] mode, this method will have no effect.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveEvents(from: Int, to: Int) {
        if (mode != REORDER) {
            return
        }

        events?.let {
            Collections.swap(it, from, to)
            notifyItemMoved(from, to)
        }
    }

    /** Cancel all reordering changes made by the user.  */
    fun cancelReorder() {
        events = backupEvents?.toMutableList()
    }
}

/**
 * View holder displaying a click in the [EventListAdapter].
 * @param holderViewBinding the view binding for this item.
 */
class EventViewHolder(private val holderViewBinding: ItemEventBinding)
    : RecyclerView.ViewHolder(holderViewBinding.root) {

    /**
     * Bind this view holder to an event.
     *
     * @param event the item providing the binding data.
     * @param mode the current ui mode.
     * @param itemClickedListener listener called when an event is clicked.
     * @param deleteClickedListener listener called when the delete button is clicked.
     */
    fun bindEvent(
        event: Event,
        @Mode mode: Int,
        itemClickedListener: (Event) -> Unit,
        deleteClickedListener: (Event) -> Unit,
    ) {
        holderViewBinding.bindEvent(event, mode, itemClickedListener, deleteClickedListener)
    }
}

/**
 * ItemTouchHelper attached to the adapter when in [REORDER] mode in order for the user to change the order of
 * the clicks.
 */
class EventReorderTouchHelper
    : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        (recyclerView.adapter as EventListAdapter).moveEvents(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing do to
    }
}
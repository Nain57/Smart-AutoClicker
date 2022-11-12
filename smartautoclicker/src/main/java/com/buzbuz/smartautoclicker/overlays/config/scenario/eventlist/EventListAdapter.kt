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
package com.buzbuz.smartautoclicker.overlays.config.scenario.eventlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.bind
import com.buzbuz.smartautoclicker.overlays.config.scenario.ConfiguredEvent

import java.util.Collections

/**
 * Adapter displaying a list of events.
 *
 * @param itemClickedListener listener called when the user clicks on an item.
 * @param itemReorderListener listener called when the user finish moving an item.
 */
class EventListAdapter(
    private val itemClickedListener: (ConfiguredEvent) -> Unit,
    private val itemReorderListener: (List<ConfiguredEvent>) -> Unit,
) : ListAdapter<ConfiguredEvent, EventViewHolder>(EventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder =
        EventViewHolder(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bindEvent(getItem(position), itemClickedListener)
    }

    /**
     * Swap the position of two events in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveEvents(from: Int, to: Int) {
        val newList = currentList.toMutableList()
        Collections.swap(newList, from, to)
        submitList(newList)
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        itemReorderListener(currentList)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [EventListAdapter] list. */
object EventDiffUtilCallback: DiffUtil.ItemCallback<ConfiguredEvent>() {
    override fun areItemsTheSame(oldItem: ConfiguredEvent, newItem: ConfiguredEvent): Boolean =
        oldItem.itemId == newItem.itemId

    override fun areContentsTheSame(oldItem: ConfiguredEvent, newItem: ConfiguredEvent): Boolean =
        oldItem == newItem
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
     * @param item the item providing the binding data.
     * @param itemClickedListener listener called when an event is clicked.
     */
    fun bindEvent(item: ConfiguredEvent, itemClickedListener: (ConfiguredEvent) -> Unit) {
        holderViewBinding.bind(item.event, true) { itemClickedListener(item) }
    }
}

/** ItemTouchHelper attached to the adapter in order for the user to change the order of the events. */
class EventReorderTouchHelper
    : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true

        (recyclerView.adapter as EventListAdapter).moveEvents(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing do to
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as EventListAdapter).notifyMoveFinished()
            isDragging = false
        }
    }
}
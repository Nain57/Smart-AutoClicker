/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.copy.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.databinding.ItemCopyHeaderBinding
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.copy.events.EventCopyModel.EventCopyItem
import com.buzbuz.smartautoclicker.overlays.utils.getIconRes

/**
 * Adapter displaying all events in a list.
 * @param onEventSelected Called when the user presses an event.
 */
class EventCopyAdapter(
    private val onEventSelected: (Event) -> Unit
) : ListAdapter<EventCopyItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when(getItem(position)) {
            is EventCopyItem.HeaderItem -> R.layout.item_copy_header
            is EventCopyItem.EventItem -> R.layout.item_event
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_copy_header -> HeaderViewHolder(
                ItemCopyHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_event -> EventViewHolder(
                ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as EventCopyItem.HeaderItem)
            is EventViewHolder -> holder.onBind(getItem(position) as EventCopyItem.EventItem, onEventSelected)
        }
    }
}

/** DiffUtil Callback comparing two items when updating the [EventCopyAdapter] list. */
private object DiffUtilCallback: DiffUtil.ItemCallback<EventCopyItem>(){
    override fun areItemsTheSame(oldItem: EventCopyItem, newItem: EventCopyItem): Boolean =
        when {
            oldItem is EventCopyItem.HeaderItem && newItem is EventCopyItem.HeaderItem -> true
            oldItem is EventCopyItem.EventItem && newItem is EventCopyItem.EventItem ->
                oldItem.event!!.id == newItem.event!!.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: EventCopyItem, newItem: EventCopyItem): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [EventCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class HeaderViewHolder(
    private val viewBinding: ItemCopyHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: EventCopyItem.HeaderItem) {
        viewBinding.textHeader.setText(header.title)
    }
}


/**
 * View holder displaying an event in the [EventCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
class EventViewHolder(private val viewBinding: ItemEventBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a event item.
     *
     * @param item the item to be represented by this view holder.
     * @param eventClickedListener listener notified upon user click on this item.
     */
    fun onBind(item: EventCopyItem.EventItem, eventClickedListener: (Event) -> Unit) {
        item.event?.let { event ->
            viewBinding.apply {
                name.text = event.name

                actionsLayout.removeAllViews()
                event.actions?.forEach { action ->
                    View.inflate(itemView.context, R.layout.view_action_icon, actionsLayout)
                    (actionsLayout.getChildAt(actionsLayout.childCount - 1) as ImageView)
                        .setImageResource(action.getIconRes())
                }
            }

            itemView.setOnClickListener { eventClickedListener(event) }
        }
    }
}
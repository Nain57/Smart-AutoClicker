/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.imageevents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent

import java.util.Collections

/**
 * Adapter displaying a list of events.
 *
 * @param itemClickedListener listener called when the user clicks on an item.
 * @param itemReorderListener listener called when the user finish moving an item.
 * @param itemViewBound listener called when a view is bound to an Event item.
 */
class ImageEventListAdapter(
    private val itemClickedListener: (ScreenEvent) -> Unit,
    private val itemReorderListener: (List<UiImageEvent>) -> Unit,
    private val itemViewBound: ((Int, View?) -> Unit),
) : ListAdapter<UiImageEvent, ImageEventViewHolder>(ImageEventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageEventViewHolder =
        ImageEventViewHolder(ItemImageEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ImageEventViewHolder, position: Int) {
        holder.bindEvent(getItem(position), itemClickedListener)
        itemViewBound(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ImageEventViewHolder) {
        itemViewBound(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
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

/** DiffUtil Callback comparing two ActionItem when updating the [ImageEventListAdapter] list. */
object ImageEventDiffUtilCallback: DiffUtil.ItemCallback<UiImageEvent>() {
    override fun areItemsTheSame(oldItem: UiImageEvent, newItem: UiImageEvent): Boolean =
        oldItem.event.id == newItem.event.id

    override fun areContentsTheSame(oldItem: UiImageEvent, newItem: UiImageEvent): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying a click in the [ImageEventListAdapter].
 * @param holderViewBinding the view binding for this item.
 */
class ImageEventViewHolder(private val holderViewBinding: ItemImageEventBinding)
    : RecyclerView.ViewHolder(holderViewBinding.root) {

    /**
     * Bind this view holder to an event.
     *
     * @param item the item providing the binding data.
     * @param itemClickedListener listener called when an event is clicked.
     */
    fun bindEvent(item: UiImageEvent, itemClickedListener: (ScreenEvent) -> Unit) {
        holderViewBinding.bind(item, true, itemClickedListener)
    }
}

/** ItemTouchHelper attached to the adapter in order for the user to change the order of the events. */
class ImageEventReorderTouchHelper
    : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true

        (recyclerView.adapter as ImageEventListAdapter).moveEvents(
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
            (recyclerView.adapter as ImageEventListAdapter).notifyMoveFinished()
            isDragging = false
        }
    }
}
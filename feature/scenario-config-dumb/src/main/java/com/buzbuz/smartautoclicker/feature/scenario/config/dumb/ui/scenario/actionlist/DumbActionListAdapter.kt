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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.ItemDumbActionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.bindings.onBind

import java.util.Collections

class DumbActionListAdapter(
    private val actionClickedListener: (DumbActionDetails) -> Unit,
    private val actionReorderListener: (List<DumbActionDetails>) -> Unit,
) : ListAdapter<DumbActionDetails, DumbActionViewHolder>(DumbActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DumbActionViewHolder =
        DumbActionViewHolder(ItemDumbActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DumbActionViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    /**
     * Swap the position of two events in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveActions(from: Int, to: Int) {
        val newList = currentList.toMutableList()
        Collections.swap(newList, from, to)
        submitList(newList)
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        actionReorderListener(currentList)
    }
}

/**
 * View holder displaying an action in the [DumbActionListAdapter].
 * @param viewBinding the view binding for this item.
 */
class DumbActionViewHolder(private val viewBinding: ItemDumbActionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param details the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(details: DumbActionDetails, actionClickedListener: (DumbActionDetails) -> Unit) {
        viewBinding.onBind(details, actionClickedListener)
    }
}

/** ItemTouchHelper attached to the adapter */
class DumbActionReorderTouchHelper : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true
        (recyclerView.adapter as DumbActionListAdapter).moveActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as DumbActionListAdapter).notifyMoveFinished()
            isDragging = false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Nothing do to */ }
}
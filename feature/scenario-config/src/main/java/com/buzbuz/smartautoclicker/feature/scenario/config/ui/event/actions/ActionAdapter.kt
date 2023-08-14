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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.actions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemActionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.bind
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.conditions.ConditionViewHolder

import java.util.Collections

/**
 * Displays the actions in a list.
 * Also provide a item displayed in the last position to add a new action.
 *
 * @param actionClickedListener  the listener called when the user clicks on a action.
 * @param actionReorderListener listener called when the list have been reordered.
 * @param itemViewBound listener called when a view is bound to an Action item.
 */
class ActionAdapter(
    private val actionClickedListener: (Action) -> Unit,
    private val actionReorderListener: (List<Pair<Action, ActionDetails>>) -> Unit,
    private val itemViewBound: ((Int, View?) -> Unit),
) : ListAdapter<Pair<Action, ActionDetails>, ActionViewHolder>(ActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder =
        ActionViewHolder(ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
        itemViewBound(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ActionViewHolder) {
        itemViewBound(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
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

/** DiffUtil Callback comparing two ActionItem when updating the [ActionAdapter] list. */
object ActionDiffUtilCallback: DiffUtil.ItemCallback<Pair<Action, ActionDetails>>() {

    override fun areItemsTheSame(
        oldItem: Pair<Action, ActionDetails>,
        newItem: Pair<Action, ActionDetails>,
    ): Boolean = oldItem.first.id == newItem.first.id

    override fun areContentsTheSame(
        oldItem: Pair<Action, ActionDetails>,
        newItem: Pair<Action, ActionDetails>,
    ): Boolean = oldItem == newItem
}

/**
 * View holder displaying an action in the [ActionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ActionViewHolder(private val viewBinding: ItemActionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param action the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(action: Pair<Action, ActionDetails>, actionClickedListener: (Action) -> Unit) {
        viewBinding.bind(action.second, true, actionClickedListener)
    }
}

/** ItemTouchHelper attached to the adapter */
class ActionReorderTouchHelper : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true
        (recyclerView.adapter as ActionAdapter).moveActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as ActionAdapter).notifyMoveFinished()
            isDragging = false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Nothing do to */ }
}
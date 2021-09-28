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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.databinding.ItemActionCardBinding

import java.util.Collections

import kotlin.collections.ArrayList

/**
 * Displays the actions in a list.
 * Also provide a item displayed in the last position to add a new action.
 *
 * @param addActionClickedListener the listener called when the user clicks on the add item. True if this is the first
 *                                 item, false if not.
 * @param actionClickedListener  the listener called when the user clicks on a action.
 */
class ActionsAdapter(
    private val addActionClickedListener: (Boolean) -> Unit,
    private val actionClickedListener: (Int, Action) -> Unit,
    private val actionReorderListener: (List<Action>) -> Unit,
) : ListAdapter<Action, ActionViewHolder>(ActionDiffUtilCallback) {

    /** The list of actions to be shown by this adapter.*/
    var actions: ArrayList<Action>? = null
        set(value) {
            field = value
            submitList(value)
        }

    override fun getItemCount(): Int = actions?.size?.plus(1) ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder =
        ActionViewHolder(ItemActionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        // The last item is the add item, allowing the user to add a new action.
        if (position == itemCount - 1) {
            holder.onBindAddAction(addActionClickedListener)
        } else {
            holder.onBindAction(actions!![position], actionClickedListener)
        }
    }


    /**
     * Swap the position of two actions in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun swapActions(from: Int, to: Int) {
        actions?.let {
            Collections.swap(it, from, to)
            notifyItemMoved(from, to)
            actionReorderListener(it)
        }
    }
}

/** */
object ActionDiffUtilCallback: DiffUtil.ItemCallback<Action>(){
    override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean =
        oldItem.getIdentifier() == newItem.getIdentifier()

    override fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean = oldItem == newItem
}

/**
 * View holder displaying an action in the [ActionsAdapter].
 * @param viewBinding the view binding for this item.
 */
class ActionViewHolder(private val viewBinding: ItemActionCardBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a 'Add action' item.
     *
     * @param addActionClickedListener listener notified upon user click on this item.
     */
    fun onBindAddAction(addActionClickedListener: (Boolean) -> Unit) {
        viewBinding.apply {
            actionName.visibility = View.GONE
            actionIcon.apply {
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(R.drawable.ic_add)
            }
        }

        itemView.setOnClickListener { addActionClickedListener.invoke(bindingAdapterPosition == 0) }
    }

    /**
     * Bind this view holder as a action item.
     *
     * @param action the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBindAction(action: Action, actionClickedListener: (Int, Action) -> Unit) {
        viewBinding.apply {
            actionName.visibility = View.VISIBLE
            actionIcon.scaleType = ImageView.ScaleType.FIT_CENTER

            when (action) {
                is Action.Click -> {
                    actionIcon.setImageResource(R.drawable.ic_click)
                    actionName.text = action.name
                }
                is Action.Swipe -> {
                    actionIcon.setImageResource(R.drawable.ic_swipe)
                    actionName.text = action.name
                }
                is Action.Pause -> {
                    actionIcon.setImageResource(R.drawable.ic_wait)
                    actionName.text = action.name
                }
            }
        }

        itemView.setOnClickListener { actionClickedListener.invoke(bindingAdapterPosition, action) }
    }
}

/** ItemTouchHelper attached to the adapter */
class ActionReorderTouchHelper
    : ItemTouchHelper.SimpleCallback(0, 0) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        (recyclerView.adapter as ActionsAdapter).swapActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing do to
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (target.bindingAdapterPosition == recyclerView.adapter!!.itemCount - 1) return false
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = if (viewHolder.bindingAdapterPosition == recyclerView.adapter!!.itemCount - 1) 0
                        else ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, 0)
    }
}
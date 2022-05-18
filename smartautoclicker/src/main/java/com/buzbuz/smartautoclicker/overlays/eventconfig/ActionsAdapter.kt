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
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.databinding.ItemActionCardBinding
import com.buzbuz.smartautoclicker.databinding.ItemNewCopyCardBinding
import com.buzbuz.smartautoclicker.overlays.utils.getIconRes

import java.util.Collections

/**
 * Displays the actions in a list.
 * Also provide a item displayed in the last position to add a new action.
 *
 * @param addActionClickedListener the listener called when the user clicks on the add item. True if this is the first
 *                                 item, false if not.
 * @param actionClickedListener  the listener called when the user clicks on a action.
 */
class ActionsAdapter(
    private val addActionClickedListener: () -> Unit,
    private val copyActionClickedListener: () -> Unit,
    private val actionClickedListener: (Int, Action) -> Unit,
    private val actionReorderListener: (List<ActionListItem>) -> Unit,
) : ListAdapter<ActionListItem, RecyclerView.ViewHolder>(ActionDiffUtilCallback) {

    /** The list of actions to be shown by this adapter.*/
    private var actions: MutableList<ActionListItem>? = null

    override fun submitList(list: MutableList<ActionListItem>?) {
        actions = list
        super.submitList(list)
    }

    override fun getItem(position: Int): ActionListItem = actions!![position]

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ActionListItem.AddActionItem -> R.layout.item_new_copy_card
            is ActionListItem.ActionItem -> R.layout.item_action_card
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_new_copy_card ->
                AddActionViewHolder(
                    ItemNewCopyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    addActionClickedListener,
                    copyActionClickedListener,
                )
            R.layout.item_action_card ->
                ActionViewHolder(ItemActionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ActionViewHolder -> holder
                .onBind((getItem(position) as ActionListItem.ActionItem).action, actionClickedListener)
            is AddActionViewHolder -> holder.onBind((getItem(position) as ActionListItem.AddActionItem))
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
        }
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        actionReorderListener(currentList)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [ActionsAdapter] list. */
object ActionDiffUtilCallback: DiffUtil.ItemCallback<ActionListItem>() {
    override fun areItemsTheSame(oldItem: ActionListItem, newItem: ActionListItem): Boolean = when {
        oldItem is ActionListItem.AddActionItem && newItem is ActionListItem.AddActionItem -> true
        oldItem is ActionListItem.ActionItem && newItem is ActionListItem.ActionItem ->
            oldItem.action.id == newItem.action.id
        else -> false
    }

    override fun areContentsTheSame(oldItem: ActionListItem, newItem: ActionListItem): Boolean = oldItem == newItem
}

/** View holder for the add action item. */
class AddActionViewHolder(
    private val viewBinding: ItemNewCopyCardBinding,
    addActionClickedListener: () -> Unit,
    copyActionClickedListener: () -> Unit
) : RecyclerView.ViewHolder(viewBinding.root) {

    init {
        viewBinding.newItem.setOnClickListener { addActionClickedListener() }
        viewBinding.copyItem.setOnClickListener { copyActionClickedListener() }
    }

    fun onBind(action: ActionListItem.AddActionItem) {
        viewBinding.copyItem.visibility =
            if (action.shouldDisplayCopy) View.VISIBLE
            else View.GONE
        viewBinding.separator.visibility =
            if (action.shouldDisplayCopy) View.VISIBLE
            else View.GONE
    }
}

/**
 * View holder displaying an action in the [ActionsAdapter].
 * @param viewBinding the view binding for this item.
 */
class ActionViewHolder(private val viewBinding: ItemActionCardBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param action the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBind(action: Action, actionClickedListener: (Int, Action) -> Unit) {
        viewBinding.apply {
            actionName.visibility = View.VISIBLE
            actionName.text = action.name
            actionIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            actionIcon.setImageResource(action.getIconRes())
        }

        itemView.setOnClickListener { actionClickedListener.invoke(bindingAdapterPosition, action) }
    }
}

/** ItemTouchHelper attached to the adapter */
class ActionReorderTouchHelper : ItemTouchHelper.SimpleCallback(0, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true
        (recyclerView.adapter as ActionsAdapter).swapActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as ActionsAdapter).notifyMoveFinished()
            isDragging = false
        }
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (target is AddActionViewHolder) return false
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = if (viewHolder.bindingAdapterPosition == recyclerView.adapter!!.itemCount - 1) 0
                        else ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Nothing do to */ }
}
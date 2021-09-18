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
package com.buzbuz.smartautoclicker.overlays.eventlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.extensions.setLeftCompoundDrawable

import java.util.Collections


/**
 * Adapter displaying a list of clicks.
 *
 * This adapter supports different display mode through it's [mode] member:
 *  - [EDITION]: clicking on an item calls [itemClickedListener], and the delete button is shown.
 *  - [COPY]: clicking on an item calls [itemClickedListener], and the delete button is hidden.
 *  - [REORDER]: user can't clicks on an item, items can be reordered through drag and drop, the delete button is
 *               replaced by the drag and drop icon.
 *
 * @param itemClickedListener listener called when the user clicks on an click item when in [EDITION] or [COPY]
 *                            mode.
 * @param deleteClickedListener listener called when the user clicks on the delete button on a click item.
 */
class EventListAdapter(
    private val itemClickedListener: (Event) -> Unit,
    private val deleteClickedListener: ((Event) -> Unit)
) : RecyclerView.Adapter<EventViewHolder>() {

    /** The list of clicks displayed by this adapter. */
    var clicks: List<Event>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    /**
     * Original position of the clicks when entering the [REORDER] mode.
     * If [cancelReorder] is called, this list will be used to restore the original positions before user changes.
     */
    var backupClicks: List<Event>? = null
    /** Set the Ui mode for the adapter. This will trigger a refresh of the list. */
    @Mode
    var mode: Int? = null
        set(value) {
            field = value
            backupClicks = if (value == REORDER) clicks?.toMutableList() else null
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = clicks?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder =
        EventViewHolder(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val click = clicks!![position]
        val drawable = R.drawable.ic_click //TODO: Update items summary

        holder.holderViewBinding.apply {
            name.text = click.name
            name.setLeftCompoundDrawable(drawable)

            when (mode) {
                EDITION -> {
                    root.setOnClickListener { itemClickedListener.invoke(click) }
                    btnAction.apply {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_cancel)
                        setOnClickListener { deleteClickedListener.invoke(click) }
                    }
                }
                COPY -> {
                    root.setOnClickListener { itemClickedListener.invoke(click) }
                    btnAction.visibility = View.GONE
                }
                REORDER -> {
                    root.setOnClickListener(null)
                    btnAction.apply {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.ic_drag)
                        setOnClickListener(null)
                    }
                }
            }
        }
    }

    /**
     * Swap the position of two clicks in the list.
     * If the ui is not in [REORDER] mode, this method will have no effect.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveClicks(from: Int, to: Int) {
        if (mode != REORDER) {
            return
        }

        clicks?.let {
            Collections.swap(it, from, to)
            notifyItemMoved(from, to)
        }
    }

    /** Cancel all reordering changes made by the user.  */
    fun cancelReorder() {
        clicks = backupClicks?.toMutableList()
    }
}

/**
 * View holder displaying a click in the [EventListAdapter].
 * @param holderViewBinding the view binding for this item.
 */
class EventViewHolder(val holderViewBinding: ItemEventBinding)
    : RecyclerView.ViewHolder(holderViewBinding.root)

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
        (recyclerView.adapter as EventListAdapter).moveClicks(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing do to
    }
}
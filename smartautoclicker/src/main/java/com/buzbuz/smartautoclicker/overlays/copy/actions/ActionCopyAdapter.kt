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
package com.buzbuz.smartautoclicker.overlays.copy.actions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.databinding.ItemActionBinding

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Adapter displaying all actions in a list.
 * @param onActionSelected Called when the user presses an action.
 */
class ActionCopyAdapter(private val onActionSelected: (Action) -> Unit): RecyclerView.Adapter<ActionViewHolder>() {

    /** The list of actions to be shown by this adapter.*/
    var actions: ArrayList<Action>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = actions?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder =
        ActionViewHolder(ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.onBindAction(actions!![position], onActionSelected)
    }
}

/**
 * View holder displaying an action in the [ActionCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
class ActionViewHolder(private val viewBinding: ItemActionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a action item.
     *
     * @param action the action to be represented by this item.
     * @param actionClickedListener listener notified upon user click on this item.
     */
    fun onBindAction(action: Action, actionClickedListener: (Action) -> Unit) {
        viewBinding.apply {
            actionName.visibility = View.VISIBLE

            when (action) {
                is Action.Click -> {
                    actionTypeIcon.setImageResource(R.drawable.ic_click)
                    actionName.text = action.name
                    actionDetails.text = itemView.context.getString(R.string.dialog_action_copy_click_details,
                        formatDuration(action.pressDuration!!), action.x, action.y)
                }
                is Action.Swipe -> {
                    actionTypeIcon.setImageResource(R.drawable.ic_swipe)
                    actionName.text = action.name
                    actionDetails.text = itemView.context.getString(R.string.dialog_action_copy_swipe_details,
                        formatDuration(action.swipeDuration!!), action.fromX, action.fromY, action.toX, action.toY)
                }
                is Action.Pause -> {
                    actionTypeIcon.setImageResource(R.drawable.ic_wait)
                    actionName.text = action.name
                    actionDetails.text = itemView.context.getString(R.string.dialog_action_copy_pause_details,
                        formatDuration(action.pauseDuration!!))
                }
            }
        }

        itemView.setOnClickListener { actionClickedListener.invoke(action) }
    }

    @OptIn(ExperimentalTime::class)
    private fun formatDuration(msDuration: Long): String {
        val duration = Duration.milliseconds(msDuration)
        var value = ""
        if (duration.inWholeHours > 0) {
            value += "${duration.inWholeHours}h "
        }
        if (duration.inWholeMinutes % 60 > 0) {
            value += "${duration.inWholeMinutes % 60}m"
        }
        if (duration.inWholeSeconds % 60 > 0) {
            value += "${duration.inWholeSeconds % 60}s"
        }
        if (duration.inWholeMilliseconds % 1000 > 0) {
            value += "${duration.inWholeMilliseconds % 1000}ms"
        }

        return value.trim()
    }
}
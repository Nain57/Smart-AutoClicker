/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.debugging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemLiveDebuggingActionBinding

class LiveDebuggingActionsAdapter : ListAdapter<LiveDebuggingActionsItem, LiveActionViewHolder>(LiveActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveActionViewHolder =
        LiveActionViewHolder(
            ItemLiveDebuggingActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: LiveActionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

object LiveActionDiffUtilCallback: DiffUtil.ItemCallback<LiveDebuggingActionsItem>() {
    override fun areItemsTheSame(oldItem: LiveDebuggingActionsItem, newItem: LiveDebuggingActionsItem): Boolean =
        oldItem.icon == newItem.icon

    override fun areContentsTheSame(oldItem: LiveDebuggingActionsItem, newItem: LiveDebuggingActionsItem): Boolean =
        oldItem == newItem
}

class LiveActionViewHolder(
    private val viewBinding: ItemLiveDebuggingActionBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: LiveDebuggingActionsItem) {
        viewBinding.actionIcon.setImageResource(item.icon)
    }
}
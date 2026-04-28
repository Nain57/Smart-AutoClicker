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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.eventtry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemLiveDebuggingEventTryActionBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.TryEventActionsItem

class TryEventActionsAdapter : ListAdapter<TryEventActionsItem, TryEventActionViewHolder>(TryEventActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TryEventActionViewHolder =
        TryEventActionViewHolder(
            ItemLiveDebuggingEventTryActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: TryEventActionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

object TryEventActionDiffUtilCallback: DiffUtil.ItemCallback<TryEventActionsItem>() {
    override fun areItemsTheSame(oldItem: TryEventActionsItem, newItem: TryEventActionsItem): Boolean =
        oldItem.icon == newItem.icon

    override fun areContentsTheSame(oldItem: TryEventActionsItem, newItem: TryEventActionsItem): Boolean =
        oldItem == newItem
}

class TryEventActionViewHolder(
    private val viewBinding: ItemLiveDebuggingEventTryActionBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: TryEventActionsItem) {
        viewBinding.actionIcon.setImageResource(item.icon)
    }
}
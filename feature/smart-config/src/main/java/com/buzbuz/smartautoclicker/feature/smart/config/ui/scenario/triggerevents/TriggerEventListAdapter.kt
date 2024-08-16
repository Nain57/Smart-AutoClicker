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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.triggerevents

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiTriggerEvent

/**
 * Adapter displaying a list of trigger events.
 * @param itemClickedListener listener called when the user clicks on an item.
 */
class TriggerEventListAdapter(
    private val itemClickedListener: (TriggerEvent) -> Unit,
) : ListAdapter<UiTriggerEvent, TriggerEventViewHolder>(TriggerEventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerEventViewHolder =
        TriggerEventViewHolder(ItemTriggerEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TriggerEventViewHolder, position: Int) {
        holder.bindEvent(getItem(position), itemClickedListener)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [TriggerEventListAdapter] list. */
object TriggerEventDiffUtilCallback: DiffUtil.ItemCallback<UiTriggerEvent>() {
    override fun areItemsTheSame(oldItem: UiTriggerEvent, newItem: UiTriggerEvent): Boolean =
        oldItem.event.id == newItem.event.id

    override fun areContentsTheSame(oldItem: UiTriggerEvent, newItem: UiTriggerEvent): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying a click in the [TriggerEventListAdapter].
 * @param holderViewBinding the view binding for this item.
 */
class TriggerEventViewHolder(
    private val holderViewBinding: ItemTriggerEventBinding,
) : RecyclerView.ViewHolder(holderViewBinding.root) {

    /**
     * Bind this view holder to an event.
     *
     * @param item the item providing the binding data.
     * @param itemClickedListener listener called when an event is clicked.
     */
    fun bindEvent(item: UiTriggerEvent, itemClickedListener: (TriggerEvent) -> Unit) {
        holderViewBinding.bind(item, itemClickedListener)
    }
}

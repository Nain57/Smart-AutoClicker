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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemEventChildDescriptionBinding


internal class EventChildrenCardsAdapter(
    private val itemClickedListener: (index: Int) -> Unit,
) : ListAdapter<EventChildrenItem, EventChildCardViewHolder>(CardIconResDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventChildCardViewHolder =
        EventChildCardViewHolder(
            ItemEventChildDescriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            itemClickedListener,
        )

    override fun onBindViewHolder(holder: EventChildCardViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

internal class EventChildCardViewHolder (
    private val viewBinding: ItemEventChildDescriptionBinding,
    private val itemClickedListener: (index: Int) -> Unit,
): ViewHolder(viewBinding.root) {

    fun onBind(item: EventChildrenItem) {
        viewBinding.apply {
            conditionImage.setImageResource(item.iconRes)
            errorBadge.visibility = if (item.isInError) View.VISIBLE else View.GONE
            root.setOnClickListener { itemClickedListener(bindingAdapterPosition) }
        }
    }
}

internal object CardIconResDiffUtilCallback: DiffUtil.ItemCallback<EventChildrenItem>() {
    override fun areItemsTheSame(oldItem: EventChildrenItem, newItem: EventChildrenItem): Boolean =
        oldItem.iconRes == newItem.iconRes
    override fun areContentsTheSame(oldItem: EventChildrenItem, newItem: EventChildrenItem): Boolean =
        oldItem == newItem
}

data class EventChildrenItem(
    @DrawableRes val iconRes: Int,
    val isInError: Boolean,
)
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.counter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterNameBinding

class CounterNameSelectionAdapter(
    private val onCounterNameSelected: (String) -> Unit,
): ListAdapter<String, CounterNameViewHolder>(CounterNameDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CounterNameViewHolder =
        CounterNameViewHolder(
            ItemCounterNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onCounterNameSelected,
        )

    override fun onBindViewHolder(holder: CounterNameViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

/** DiffUtil Callback comparing two counter names when updating the [CounterNameSelectionAdapter] list. */
object CounterNameDiffUtilCallback: DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}


/**
 * View holder displaying an counter name in the [CounterNameSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class CounterNameViewHolder(
    private val viewBinding: ItemCounterNameBinding,
    private val onCounterNameSelected: (String) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: String) {
        viewBinding.textCounterName.text = item
        viewBinding.root.setOnClickListener { onCounterNameSelected(item) }
    }
}

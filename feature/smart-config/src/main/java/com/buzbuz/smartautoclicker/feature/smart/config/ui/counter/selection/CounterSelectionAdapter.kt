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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterNameBinding

class CounterSelectionAdapter(
    private val onCounterSelected: (CounterSelectionUiItem) -> Unit,
): ListAdapter<CounterSelectionUiItem, CounterSelectionViewHolder>(CounterSelectionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CounterSelectionViewHolder =
        CounterSelectionViewHolder(
            ItemCounterNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onCounterSelected,
        )

    override fun onBindViewHolder(holder: CounterSelectionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

/** DiffUtil Callback comparing two counter names when updating the [CounterSelectionAdapter] list. */
object CounterSelectionDiffUtilCallback: DiffUtil.ItemCallback<CounterSelectionUiItem>() {
    override fun areItemsTheSame(oldItem: CounterSelectionUiItem, newItem: CounterSelectionUiItem): Boolean =
        oldItem.counterName == newItem.counterName
    override fun areContentsTheSame(oldItem: CounterSelectionUiItem, newItem: CounterSelectionUiItem): Boolean =
        oldItem == newItem
}


/**
 * View holder displaying an counter name in the [CounterSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class CounterSelectionViewHolder(
    private val viewBinding: ItemCounterNameBinding,
    private val onCounterSelected: (CounterSelectionUiItem) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: CounterSelectionUiItem) {
        viewBinding.title.text = item.counterName
        viewBinding.description.text = item.counterStartingValueDesc
        viewBinding.root.setOnClickListener { onCounterSelected(item) }
    }
}

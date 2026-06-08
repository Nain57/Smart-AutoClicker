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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.reference

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterReferenceBinding

/** Adapter for the list of counter references. */
class CounterReferenceAdapter : ListAdapter<CounterReferenceUiItem, CounterReferenceViewHolder>(ReferenceDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CounterReferenceViewHolder =
        CounterReferenceViewHolder(
            ItemCounterReferenceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(holder: CounterReferenceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object ReferenceDiffCallback : DiffUtil.ItemCallback<CounterReferenceUiItem>() {
    override fun areItemsTheSame(oldItem: CounterReferenceUiItem, newItem: CounterReferenceUiItem): Boolean =
        oldItem == newItem // Data classes, equals is fine as there is no ID in the UI state

    override fun areContentsTheSame(oldItem: CounterReferenceUiItem, newItem: CounterReferenceUiItem): Boolean =
        oldItem == newItem
}

class CounterReferenceViewHolder(
    private val binding: ItemCounterReferenceBinding,
) : RecyclerView.ViewHolder(binding.root) {

    private var item: CounterReferenceUiItem? = null

    fun bind(newItem: CounterReferenceUiItem) {
        item = newItem
        binding.apply {
            referenceName.apply {
                text = newItem.elementName
                setCompoundDrawablesWithIntrinsicBounds(newItem.elementIconRes, 0, 0, 0)
            }
            eventName.apply {
                text = newItem.eventName
                setCompoundDrawablesWithIntrinsicBounds(newItem.eventIconRes, 0, 0, 0)
            }
            effectDescription.text = newItem.referenceDesc
        }
    }
}

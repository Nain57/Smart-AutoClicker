/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter.CounterStateItem


class CounterStateAdapter : ListAdapter<CounterStateItem, CounterStateViewHolder>(CounterStateDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CounterStateViewHolder =
        CounterStateViewHolder(parent)

    override fun onBindViewHolder(holder: CounterStateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object CounterStateDiffUtilCallback : DiffUtil.ItemCallback<CounterStateItem>() {

    override fun areItemsTheSame(oldItem: CounterStateItem, newItem: CounterStateItem): Boolean =
        oldItem === newItem

    override fun areContentsTheSame(oldItem: CounterStateItem, newItem: CounterStateItem): Boolean =
        oldItem == newItem

}
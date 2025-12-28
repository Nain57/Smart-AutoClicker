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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemCounterStateBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details.counter.CounterStateItem

class CounterStateViewHolder private constructor(
    private val viewBinding: ItemCounterStateBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    constructor(parent: ViewGroup) : this(
        viewBinding = ItemCounterStateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    fun bind(item: CounterStateItem) {
        viewBinding.apply {
            counterNameText.text = item.counterName
            valueText.text = item.toValueDisplayText(root.context)
        }
    }

    private fun CounterStateItem.toValueDisplayText(context: Context): String  {
        val oldValue = oldCounterValue
        return if (oldValue == null) {
            context.getString(
                R.string.item_counter_state_value_same,
                currentCounterValue,
            )
        } else {
            context.getString(
                R.string.item_counter_state_value_changed,
                oldValue,
                currentCounterValue,
            )
        }
    }
}
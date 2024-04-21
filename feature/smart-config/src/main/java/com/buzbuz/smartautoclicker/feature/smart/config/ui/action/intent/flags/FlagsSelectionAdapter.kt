/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.flags

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemIntentFlagBinding

class FlagsSelectionAdapter(
    private val onFlagCheckClicked: (Int, Boolean) -> Unit,
    private val onFlagHelpClicked: (Uri) -> Unit,
) : ListAdapter<ItemFlag, ItemFlagViewHolder>(ItemFlagDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFlagViewHolder =
        ItemFlagViewHolder(
            viewBinding = ItemIntentFlagBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFlagCheckClicked = onFlagCheckClicked,
            onFlagHelpClicked = onFlagHelpClicked,
        )

    override fun onBindViewHolder(holder: ItemFlagViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [FlagsSelectionAdapter] list. */
object ItemFlagDiffUtilCallback: DiffUtil.ItemCallback<ItemFlag>() {
    override fun areItemsTheSame(oldItem: ItemFlag, newItem: ItemFlag): Boolean =
        oldItem.flag.value == newItem.flag.value
    override fun areContentsTheSame(oldItem: ItemFlag, newItem: ItemFlag): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying an action in the [FlagsSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ItemFlagViewHolder(
    private val viewBinding: ItemIntentFlagBinding,
    private val onFlagCheckClicked: (Int, Boolean) -> Unit,
    private val onFlagHelpClicked: (Uri) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemFlag) {
        viewBinding.apply {
            flagName.text = item.flag.displayName
            buttonState.isChecked = item.isSelected

            btnHelp.setOnClickListener { onFlagHelpClicked(item.flag.helpUri) }
            buttonState.setOnClickListener { onFlagCheckClicked(item.flag.value, !item.isSelected) }
        }
    }
}

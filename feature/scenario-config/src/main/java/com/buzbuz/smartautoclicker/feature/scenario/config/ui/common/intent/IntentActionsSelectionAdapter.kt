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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.common.intent

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemIntentActionBinding

class IntentActionsSelectionAdapter(
    private val onActionCheckClicked: (String, Boolean) -> Unit,
    private val onActionHelpClicked: (Uri) -> Unit,
) : ListAdapter<ItemAction, ItemIntentActionViewHolder>(ItemIntentActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemIntentActionViewHolder =
        ItemIntentActionViewHolder(
            viewBinding = ItemIntentActionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onActionCheckClicked = onActionCheckClicked,
            onActionHelpClicked = onActionHelpClicked,
        )

    override fun onBindViewHolder(holder: ItemIntentActionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [IntentActionsSelectionAdapter] list. */
object ItemIntentActionDiffUtilCallback: DiffUtil.ItemCallback<ItemAction>() {
    override fun areItemsTheSame(oldItem: ItemAction, newItem: ItemAction): Boolean =
        oldItem.action.value == newItem.action.value
    override fun areContentsTheSame(oldItem: ItemAction, newItem: ItemAction): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying an action in the [IntentActionsSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ItemIntentActionViewHolder(
    private val viewBinding: ItemIntentActionBinding,
    private val onActionCheckClicked: (String, Boolean) -> Unit,
    private val onActionHelpClicked: (Uri) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemAction) {
        viewBinding.apply {
            actionName.text = item.action.displayName
            buttonState.isChecked = item.isSelected

            btnHelp.setOnClickListener { onActionHelpClicked(item.action.helpUri) }
            buttonState.setOnClickListener { onActionCheckClicked(item.action.value, !item.isSelected) }
        }
    }
}

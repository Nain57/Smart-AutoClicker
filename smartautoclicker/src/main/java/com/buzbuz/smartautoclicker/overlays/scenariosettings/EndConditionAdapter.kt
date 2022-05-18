/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.scenariosettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.databinding.ItemEndConditionCardBinding
import com.buzbuz.smartautoclicker.databinding.ItemNewCopyCardBinding

/**
 * Adapter displaying a list of end conditions.
 *
 * @param addEndConditionClickedListener listener called when the user clicks on the add end condition item.
 * @param endConditionClickedListener listener called when the user clicks on an existing end condition.
 */
class EndConditionAdapter(
    private val addEndConditionClickedListener: () -> Unit,
    private val endConditionClickedListener: (EndCondition, Int) -> Unit,
) : ListAdapter<EndConditionListItem, RecyclerView.ViewHolder>(EndConditionDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is EndConditionListItem.AddEndConditionItem -> R.layout.item_new_copy_card
            is EndConditionListItem.EndConditionItem -> R.layout.item_end_condition_card
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_new_copy_card -> AddEndConditionViewHolder(
                ItemNewCopyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                addEndConditionClickedListener,
            )

            R.layout.item_end_condition_card -> EndConditionViewHolder(
                ItemEndConditionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                endConditionClickedListener,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EndConditionViewHolder -> holder.onBind((getItem(position) as EndConditionListItem.EndConditionItem))
            else -> { /* Nothing to do */}
        }
    }
}

/** DiffUtil Callback comparing two EndConditionListItem when updating the [EndConditionAdapter] list. */
object EndConditionDiffUtilCallback: DiffUtil.ItemCallback<EndConditionListItem>() {
    override fun areItemsTheSame(oldItem: EndConditionListItem, newItem: EndConditionListItem): Boolean = when {
        oldItem is EndConditionListItem.AddEndConditionItem && newItem is EndConditionListItem.AddEndConditionItem -> true
        oldItem is EndConditionListItem.EndConditionItem && newItem is EndConditionListItem.EndConditionItem ->
            oldItem.endCondition.id == newItem.endCondition.id
        else -> false
    }

    override fun areContentsTheSame(oldItem: EndConditionListItem, newItem: EndConditionListItem): Boolean =
        oldItem == newItem
}

/** View holder for the add end condition item. */
class AddEndConditionViewHolder(
    viewBinding: ItemNewCopyCardBinding,
    addEndConditionClickedListener: () -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    init {
        viewBinding.newItem.setOnClickListener { addEndConditionClickedListener() }
        viewBinding.separator.visibility = View.GONE
        viewBinding.copyItem.visibility = View.GONE
    }
}

/** View holder for the end condition item. */
class EndConditionViewHolder(
    private val viewBinding: ItemEndConditionCardBinding,
    private val endConditionClickedListener: (EndCondition, Int) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: EndConditionListItem.EndConditionItem) {
        viewBinding.apply {
            root.setOnClickListener { endConditionClickedListener(item.endCondition, bindingAdapterPosition) }
            textEndConditionName.text = item.endCondition.eventName
            textEndConditionExecutions.text = itemView.context.getString(
                R.string.dialog_scenario_settings_end_condition_card_executions,
                item.endCondition.executions
            )
        }
    }
}
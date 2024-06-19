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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemActionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemActionBriefPortBinding


class ActionListAdapter(
    private val displayMetrics: DisplayMetrics,
    private val actionClickedListener: (SmartActionBriefItem) -> Unit,
) : ListAdapter<SmartActionBriefItem, ActionBriefViewHolder>(ActionBriefDiffUtilCallback) {

    private var orientation: Int = displayMetrics.orientation

    override fun getItemViewType(position: Int): Int = orientation

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionBriefViewHolder =
        ActionBriefViewHolder(ActionBriefItemBinding.inflate(LayoutInflater.from(parent.context), orientation, parent))

    override fun onBindViewHolder(holder: ActionBriefViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    @SuppressLint("NotifyDataSetChanged") // Reload the whole list when the orientation is different
    override fun submitList(list: List<SmartActionBriefItem>?) {
        if (orientation != displayMetrics.orientation) {
            orientation = displayMetrics.orientation
            notifyDataSetChanged()
            return
        }

        super.submitList(list)
    }
}

object ActionBriefDiffUtilCallback: DiffUtil.ItemCallback<SmartActionBriefItem>() {
    override fun areItemsTheSame(
        oldItem: SmartActionBriefItem,
        newItem: SmartActionBriefItem,
    ): Boolean = oldItem.action.id == newItem.action.id

    override fun areContentsTheSame(
        oldItem: SmartActionBriefItem,
        newItem: SmartActionBriefItem,
    ): Boolean = oldItem == newItem
}

class ActionBriefViewHolder(
    private val viewBinding: ActionBriefItemBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(details: SmartActionBriefItem, actionClickedListener: (SmartActionBriefItem) -> Unit) {
        viewBinding.apply {
            root.setOnClickListener { actionClickedListener(details) }

            actionName.visibility = View.VISIBLE
            actionTypeIcon.setImageResource(details.actionTypeIcon)
            actionName.text = details.actionName
            actionDescription.text = details.actionDescription
        }
    }
}

class ActionBriefItemBinding private constructor(
    val root: View,
    val actionTypeIcon: ImageView,
    val actionName: TextView,
    val actionDescription: TextView,
) {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ActionBriefItemBinding(ItemActionBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                ActionBriefItemBinding(ItemActionBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemActionBriefPortBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.actionTypeIcon,
        actionName = binding.actionName,
        actionDescription = binding.actionDescription,
    )

    constructor(binding: ItemActionBriefLandBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.actionTypeIcon,
        actionName = binding.actionName,
        actionDescription = binding.actionDescription,
    )
}
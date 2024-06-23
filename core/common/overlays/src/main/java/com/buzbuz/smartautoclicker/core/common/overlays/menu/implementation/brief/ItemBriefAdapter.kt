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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

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

import com.buzbuz.smartautoclicker.core.common.overlays.databinding.ItemBriefLandBinding
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.ItemBriefPortBinding
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics


internal class ItemBriefAdapter(
    private val displayMetrics: DisplayMetrics,
    private val actionClickedListener: (Int, ItemBrief) -> Unit,
) : ListAdapter<ItemBrief, ItemBriefViewHolder>(ItemBriefDiffUtilCallback) {

    private var orientation: Int = displayMetrics.orientation

    override fun getItemViewType(position: Int): Int = orientation

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBriefViewHolder =
        ItemBriefViewHolder(ItemBriefBinding.inflate(LayoutInflater.from(parent.context), orientation, parent))

    override fun onBindViewHolder(holder: ItemBriefViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    public override fun getItem(position: Int): ItemBrief = super.getItem(position)

    @SuppressLint("NotifyDataSetChanged") // Reload the whole list when the orientation is different
    override fun submitList(list: List<ItemBrief>?) {
        if (orientation != displayMetrics.orientation) {
            orientation = displayMetrics.orientation
            notifyDataSetChanged()
            return
        }

        super.submitList(list)
    }
}

object ItemBriefDiffUtilCallback: DiffUtil.ItemCallback<ItemBrief>() {
    override fun areItemsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem == newItem
}

internal class ItemBriefViewHolder(
    private val viewBinding: ItemBriefBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemBrief, actionClickedListener: (Int, ItemBrief) -> Unit) {
        viewBinding.apply {
            root.setOnClickListener { actionClickedListener(bindingAdapterPosition, item) }

            actionName.visibility = View.VISIBLE
            actionTypeIcon.setImageResource(item.icon)
            actionName.text = item.name
            actionDescription.text = item.description
        }
    }
}

internal class ItemBriefBinding private constructor(
    val root: View,
    val actionTypeIcon: ImageView,
    val actionName: TextView,
    val actionDescription: TextView,
) {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ItemBriefBinding(ItemBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                ItemBriefBinding(ItemBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemBriefPortBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.itemIcon,
        actionName = binding.itemName,
        actionDescription = binding.itemDescription,
    )

    constructor(binding: ItemBriefLandBinding) : this(
        root = binding.root,
        actionTypeIcon = binding.itemIcon,
        actionName = binding.itemName,
        actionDescription = binding.itemDescription,
    )
}
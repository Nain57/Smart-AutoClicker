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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemSmartActionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemSmartActionBriefPortBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.UiAction


class SmartActionBriefViewHolder(
    layoutInflater: LayoutInflater,
    orientation: Int,
    parent: ViewGroup,
) : ItemBriefViewHolder<ItemSmartActionBriefBinding>(ItemSmartActionBriefBinding.inflate(layoutInflater, orientation, parent)) {

    override fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit) {
        viewBinding.apply {
            rootView.setOnClickListener { itemClickedListener(bindingAdapterPosition, item) }

            val details = item.data as UiAction
            name.visibility = View.VISIBLE
            icon.setImageResource(details.icon)
            name.text = details.name
            description.text = details.description
        }
    }
}

class ItemSmartActionBriefBinding private constructor(
    val rootView: View,
    val icon: ImageView,
    val name: TextView,
    val description: TextView,
) : ViewBinding {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ItemSmartActionBriefBinding(ItemSmartActionBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                ItemSmartActionBriefBinding(ItemSmartActionBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemSmartActionBriefPortBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        description = binding.itemDescription,
    )

    constructor(binding: ItemSmartActionBriefLandBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        description = binding.itemDescription,
    )

    override fun getRoot(): View = rootView
}
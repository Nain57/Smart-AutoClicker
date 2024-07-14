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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.brief

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbActionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbActionBriefPortBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist.DumbActionDetails


class DumbActionBriefViewHolder(
    layoutInflater: LayoutInflater,
    orientation: Int,
    parent: ViewGroup,
) : ItemBriefViewHolder<ItemDumbActionBriefBinding>(ItemDumbActionBriefBinding.inflate(layoutInflater, orientation, parent)) {

    override fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit) {
        viewBinding.apply {
            viewRoot.setOnClickListener { itemClickedListener(bindingAdapterPosition, item) }

            val details = item.data as DumbActionDetails
            name.visibility = View.VISIBLE
            icon.setImageResource(details.icon)
            name.text = details.name
            duration.text = details.detailsText
            repeat.text = details.repeatCountText
        }
    }
}

class ItemDumbActionBriefBinding private constructor(
    val viewRoot: View,
    val name: TextView,
    val duration: TextView,
    val repeat: TextView,
    val icon: ImageView,
) : ViewBinding {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ItemDumbActionBriefBinding(ItemDumbActionBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                ItemDumbActionBriefBinding(ItemDumbActionBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemDumbActionBriefPortBinding) : this(
        viewRoot = binding.root,
        name = binding.actionName,
        duration = binding.actionDuration,
        repeat = binding.actionRepeat,
        icon = binding.actionTypeIcon,
    )

    constructor(binding: ItemDumbActionBriefLandBinding) : this(
        viewRoot = binding.root,
        name = binding.actionName,
        duration = binding.actionDuration,
        repeat = binding.actionRepeat,
        icon = binding.actionTypeIcon,
    )

    override fun getRoot(): View = viewRoot
}
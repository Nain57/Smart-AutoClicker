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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.brief

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTextConditionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTextConditionBriefPortBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTextCondition


class TextConditionBriefViewHolder(
    layoutInflater: LayoutInflater,
    orientation: Int,
    parent: ViewGroup,
) : ItemBriefViewHolder<TextConditionBriefBinding>(TextConditionBriefBinding.inflate(layoutInflater, orientation, parent)) {

    override fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit) {
        viewBinding.apply {
            rootView.setOnClickListener { itemClickedListener(bindingAdapterPosition, item) }
            val details = item.data as UiTextCondition

            name.visibility = View.VISIBLE
            name.text = details.name
            textToDetect.text = details.conditionTextDescription

            shouldBeDetectedIcon.setImageResource(details.shouldBeVisibleIconRes)
            threshold.text = details.thresholdText
            icon.setImageResource(details.detectionTypeIconRes)

            errorBadge.visibility = if (details.haveError) View.VISIBLE else View.GONE
        }
    }

}

class TextConditionBriefBinding private constructor(
    val rootView: View,
    val icon: ImageView,
    val name: TextView,
    val textToDetect: TextView,
    val shouldBeDetectedIcon: ImageView,
    val threshold: TextView,
    val errorBadge: ImageView,
) : ViewBinding {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                TextConditionBriefBinding(ItemTextConditionBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                TextConditionBriefBinding(ItemTextConditionBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemTextConditionBriefPortBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        textToDetect = binding.textToDetect,
        shouldBeDetectedIcon = binding.iconShouldBeDetected,
        threshold = binding.textThreshold,
        errorBadge = binding.errorBadge,
    )

    constructor(binding: ItemTextConditionBriefLandBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        textToDetect = binding.textToDetect,
        shouldBeDetectedIcon = binding.iconShouldBeDetected,
        threshold = binding.textThreshold,
        errorBadge = binding.errorBadge,
    )

    override fun getRoot(): View = rootView
}
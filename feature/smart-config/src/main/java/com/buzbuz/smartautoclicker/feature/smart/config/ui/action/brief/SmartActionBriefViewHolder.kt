
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
            errorBadge.visibility = if (details.haveError) View.VISIBLE else View.GONE
        }
    }
}

class ItemSmartActionBriefBinding private constructor(
    val rootView: View,
    val icon: ImageView,
    val name: TextView,
    val description: TextView,
    val errorBadge: ImageView,
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
        errorBadge = binding.errorBadge,
    )

    constructor(binding: ItemSmartActionBriefLandBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        description = binding.itemDescription,
        errorBadge = binding.errorBadge,
    )

    override fun getRoot(): View = rootView
}
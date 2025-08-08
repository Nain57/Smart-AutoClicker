
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
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionDetails


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

            if (details.repeatCountText != null) {
                repeat.text = details.repeatCountText
                repeat.visibility = View.VISIBLE
            } else {
                repeat.visibility = View.GONE
            }
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
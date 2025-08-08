
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.brief

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.core.widget.TextViewCompat
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionBriefLandBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionBriefPortBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition


class ImageConditionBriefViewHolder(
    layoutInflater: LayoutInflater,
    orientation: Int,
    parent: ViewGroup,
) : ItemBriefViewHolder<ImageConditionBriefBinding>(ImageConditionBriefBinding.inflate(layoutInflater, orientation, parent)) {

    override fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit) {
        viewBinding.apply {
            rootView.setOnClickListener { itemClickedListener(bindingAdapterPosition, item) }
            val details = item.data as UiImageCondition

            name.visibility = View.VISIBLE
            name.text = details.name

            shouldBeDetectedText.setText(details.shouldBeVisibleTextRes)
            shouldBeDetectedIcon.setImageResource(details.shouldBeVisibleIconRes)

            TextViewCompat.setCompoundDrawableTintList(
                shouldBeDetectedText,
                ColorStateList.valueOf(root.context.getColor(R.color.iconColor))
            )

            threshold.text = details.thresholdText
            icon.setImageResource(details.detectionTypeIconRes)

            errorBadge.visibility = if (details.haveError) View.VISIBLE else View.GONE
        }
    }

}

class ImageConditionBriefBinding private constructor(
    val rootView: View,
    val icon: ImageView,
    val name: TextView,
    val shouldBeDetectedText: TextView,
    val shouldBeDetectedIcon: ImageView,
    val threshold: TextView,
    val errorBadge: ImageView,
) : ViewBinding {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int, parent: ViewGroup) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ImageConditionBriefBinding(ItemImageConditionBriefPortBinding.inflate(layoutInflater, parent, false))
            else
                ImageConditionBriefBinding(ItemImageConditionBriefLandBinding.inflate(layoutInflater, parent, false))
    }

    constructor(binding: ItemImageConditionBriefPortBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        shouldBeDetectedText = binding.textShouldBeDetected,
        shouldBeDetectedIcon = binding.iconShouldBeDetected,
        threshold = binding.textThreshold,
        errorBadge = binding.errorBadge,
    )

    constructor(binding: ItemImageConditionBriefLandBinding) : this(
        rootView = binding.root,
        icon = binding.itemIcon,
        name = binding.itemName,
        shouldBeDetectedText = binding.textShouldBeDetected,
        shouldBeDetectedIcon = binding.iconShouldBeDetected,
        threshold = binding.textThreshold,
        errorBadge = binding.errorBadge,
    )

    override fun getRoot(): View = rootView
}
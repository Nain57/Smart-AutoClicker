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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.recyclerview.ViewBindingHolder
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition

import kotlinx.coroutines.Job


internal class ImageConditionViewHolder (
    parent: ViewGroup,
    val bitmapProvider: ((ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?)?,
    val itemClickedListener: (item: UiImageCondition, index: Int) -> Unit,
): ViewBindingHolder<ItemImageConditionListBinding>(
    viewBinding = ItemImageConditionListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(uiCondition: UiImageCondition) {
        bitmapLoadingJob?.cancel()

        viewBinding.cardImageCondition.apply {
            root.setOnClickListener { itemClickedListener.invoke(uiCondition, bindingAdapterPosition) }
            conditionName.text = uiCondition.name
            conditionShouldBeDetected.setImageResource(uiCondition.shouldBeVisibleIconRes)
            conditionDetectionType.setImageResource(uiCondition.detectionTypeIconRes)
            conditionThreshold.text = uiCondition.thresholdText
        }

        bitmapLoadingJob = bitmapProvider?.invoke(uiCondition.condition) { bitmap ->
            if (bitmap != null) {
                viewBinding.cardImageCondition.conditionImage.setImageBitmap(bitmap)
            } else {
                viewBinding.cardImageCondition.conditionImage.setImageDrawable(
                    ContextCompat.getDrawable(viewBinding.root.context, R.drawable.ic_cancel)?.apply {
                        setTint(Color.RED)
                    }
                )
            }
        }
    }

    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

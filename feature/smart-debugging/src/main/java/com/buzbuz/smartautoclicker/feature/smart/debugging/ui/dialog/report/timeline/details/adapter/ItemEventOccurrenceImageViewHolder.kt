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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.base.extensions.setRightCompoundDrawable
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.debugging.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemConditionResultImageBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.details.EventOccurrenceItem

import kotlinx.coroutines.Job


class ItemEventOccurrenceImageViewHolder private constructor(
    private val viewBinding: ItemConditionResultImageBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    constructor(parent: ViewGroup, bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?) : this(
        bitmapProvider = bitmapProvider,
        viewBinding = ItemConditionResultImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    fun bind(item: EventOccurrenceItem.Image) {
        viewBinding.apply {
            conditionNameText.text = item.conditionName
            conditionDurationText.text = item.durationText

            conditionConfidenceText.apply {
                text = item.confidenceText
                setRightCompoundDrawable(
                    if (item.confidenceValid) R.drawable.ic_debug_confirm else R.drawable.ic_debug_cancel
                )
            }
            conditionShouldBeDetectedText.setRightCompoundDrawable(
                if (item.shouldDetectedValue) R.drawable.ic_debug_confirm else R.drawable.ic_debug_cancel
            )
            conditionFulfilledText.setRightCompoundDrawable(
                if (item.isFulfilledValue) R.drawable.ic_debug_confirm else R.drawable.ic_debug_cancel
            )

            bitmapLoadingJob?.cancel()
            bitmapLoadingJob = bitmapProvider(item.condition) { bitmap ->
                if (bitmap != null) conditionImage.setImageBitmap(bitmap)
                else conditionImage.setImageDrawable(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_cancel)?.apply {
                        setTint(Color.RED)
                    }
                )
            }
        }
    }

    fun unbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}
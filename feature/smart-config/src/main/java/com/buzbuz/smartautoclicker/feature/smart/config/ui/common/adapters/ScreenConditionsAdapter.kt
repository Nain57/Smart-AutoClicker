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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTextConditionListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTextCondition

import kotlinx.coroutines.Job


class ScreenConditionsAdapter(
    private val itemClickedListener: (item: UiScreenCondition, index: Int) -> Unit,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val itemViewBound: ((Int, View?) -> Unit)? = null,
) : ListAdapter<UiScreenCondition, ViewHolder>(ScreenConditionDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UiImageCondition -> R.layout.item_image_condition_list
            is UiTextCondition -> R.layout.item_text_condition_list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.item_image_condition_list -> EventImageConditionViewHolder(
                ItemImageConditionListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider,
                itemClickedListener,
            )

            R.layout.item_text_condition_list -> EventTextConditionViewHolder(
                ItemTextConditionListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                itemClickedListener,
            )

            else -> throw IllegalArgumentException("Unsupported item type")
        }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is EventImageConditionViewHolder -> {
                holder.onBind((getItem(position) as UiImageCondition))
                itemViewBound?.invoke(position, holder.viewBinding.cardImageCondition.root)
            }
            is EventTextConditionViewHolder -> {
                holder.onBind((getItem(position) as UiTextCondition))
                itemViewBound?.invoke(position, holder.viewBinding.cardTextCondition.root)
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        (holder as? EventImageConditionViewHolder)?.onUnbind()
        itemViewBound?.invoke(holder.bindingAdapterPosition, null)
    }
}

internal class EventImageConditionViewHolder (
    val viewBinding: ItemImageConditionListBinding,
    val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    val itemClickedListener: (item: UiScreenCondition, index: Int) -> Unit,
): ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(condition: UiImageCondition) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.cardImageCondition.bind(condition, bitmapProvider) {
            itemClickedListener(condition, bindingAdapterPosition)
        }
    }

    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

internal class EventTextConditionViewHolder (
    val viewBinding: ItemTextConditionListBinding,
    val itemClickedListener: (item: UiScreenCondition, index: Int) -> Unit,
): ViewHolder(viewBinding.root) {

    fun onBind(condition: UiTextCondition) {
       viewBinding.cardTextCondition.bind(condition) {
           itemClickedListener(condition, bindingAdapterPosition)
       }
    }
}

internal object ScreenConditionDiffUtilCallback: DiffUtil.ItemCallback<UiScreenCondition>() {
    override fun areItemsTheSame(oldItem: UiScreenCondition, newItem: UiScreenCondition): Boolean =
        oldItem.condition.id == newItem.condition.id
    override fun areContentsTheSame(oldItem: UiScreenCondition, newItem: UiScreenCondition): Boolean =
        oldItem == newItem
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition

import kotlinx.coroutines.Job


internal class EventImageConditionsAdapter(
    private val itemClickedListener: (index: Int) -> Unit,
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<UiScreenCondition, EventImageConditionViewHolder>(ImageConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventImageConditionViewHolder =
        EventImageConditionViewHolder(
            ItemImageConditionListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            itemClickedListener,
        )

    override fun onBindViewHolder(holder: EventImageConditionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onViewRecycled(holder: EventImageConditionViewHolder) {
        holder.onUnbind()
    }
}

internal class EventImageConditionViewHolder (
    private val viewBinding: ItemImageConditionListBinding,
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val itemClickedListener: (index: Int) -> Unit,
): ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(condition: UiScreenCondition) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.cardImageCondition.bind(condition, bitmapProvider) {
            itemClickedListener(bindingAdapterPosition)
        }
    }

    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

internal object ImageConditionDiffUtilCallback: DiffUtil.ItemCallback<UiScreenCondition>() {
    override fun areItemsTheSame(oldItem: UiScreenCondition, newItem: UiScreenCondition): Boolean =
        oldItem.condition.id == newItem.condition.id
    override fun areContentsTheSame(oldItem: UiScreenCondition, newItem: UiScreenCondition): Boolean =
        oldItem == newItem
}
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.conditions

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind

import kotlinx.coroutines.Job

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 * @param itemViewBound listener called when a view is bound to a Condition item.
 */
class ImageConditionAdapter(
    private val conditionClickedListener: (ImageCondition) -> Unit,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val itemViewBound: ((Int, View?) -> Unit),
    ) : ListAdapter<ImageCondition, ImageConditionViewHolder>(ImageConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ImageConditionViewHolder(
            ItemImageConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
        )

    override fun onBindViewHolder(holder: ImageConditionViewHolder, position: Int) {
        holder.onBindCondition((getItem(position)), conditionClickedListener)
        itemViewBound(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ImageConditionViewHolder) {
        holder.onUnbind()
        itemViewBound(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [ImageConditionAdapter] list. */
object ImageConditionDiffUtilCallback: DiffUtil.ItemCallback<ImageCondition>() {
    override fun areItemsTheSame(oldItem: ImageCondition, newItem: ImageCondition): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ImageCondition, newItem: ImageCondition): Boolean = oldItem == newItem
}

/**
 * View holder displaying a condition in the [ImageConditionAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ImageConditionViewHolder(
    private val viewBinding: ItemImageConditionBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: ImageCondition, conditionClickedListener: (ImageCondition) -> Unit) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.bind(condition, bitmapProvider, conditionClickedListener)
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}
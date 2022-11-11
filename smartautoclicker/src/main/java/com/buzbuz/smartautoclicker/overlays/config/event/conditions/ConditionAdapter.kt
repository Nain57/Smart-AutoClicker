/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.event.conditions

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.overlays.base.bindings.bind
import com.buzbuz.smartautoclicker.overlays.base.utils.setIconTint

import kotlinx.coroutines.Job

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionAdapter(
    private val conditionClickedListener: (Condition, Int) -> Unit,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<Condition, ConditionViewHolder>(ConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ConditionViewHolder(
            ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
        )

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        holder.onBindCondition((getItem(position)), conditionClickedListener)
    }

    override fun onViewRecycled(holder: ConditionViewHolder) {
        holder.onUnbind()
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [ConditionAdapter] list. */
object ConditionDiffUtilCallback: DiffUtil.ItemCallback<Condition>() {
    override fun areItemsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem == newItem
}

/**
 * View holder displaying a condition in the [ConditionAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionViewHolder(
    private val viewBinding: ItemConditionBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: Condition, conditionClickedListener: (Condition, Int) -> Unit) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.bind(condition, bindingAdapterPosition, bitmapProvider, conditionClickedListener)
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}
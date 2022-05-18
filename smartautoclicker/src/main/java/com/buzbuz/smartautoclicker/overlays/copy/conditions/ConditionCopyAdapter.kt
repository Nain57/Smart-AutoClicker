/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.copy.conditions

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.databinding.ItemConditionBinding

import kotlinx.coroutines.Job

/**
 * Adapter displaying all conditions in a list.
 * @param conditionClickedListener called when the user presses a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionCopyAdapter(
    private val conditionClickedListener: (Condition) -> Unit,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
): ListAdapter<Condition, ConditionViewHolder>(ConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder =
        ConditionViewHolder(
            ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider
        )

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        holder.onBindCondition(getItem(position), conditionClickedListener)
    }
}

/** DiffUtil Callback comparing two Conditions when updating the [ConditionCopyAdapter] list. */
object ConditionDiffUtilCallback: DiffUtil.ItemCallback<Condition>(){
    override fun areItemsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem == newItem
}

/**
 * View holder displaying a condition in the [ConditionCopyAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmap.
 */
class ConditionViewHolder(
    private val viewBinding: ItemConditionBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a action item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: Condition, conditionClickedListener: (Condition) -> Unit) {
        viewBinding.apply {
            conditionName.text = condition.name
            conditionThreshold.text = itemView.context.getString(
                R.string.dialog_condition_copy_threshold,
                condition.threshold
            )

            conditionShouldBeDetected.apply {
                if (condition.shouldBeDetected) {
                    setImageResource(R.drawable.ic_confirm)
                } else {
                    setImageResource(R.drawable.ic_cancel)
                }
            }

            conditionDetectionType.setImageResource(
                if (condition.detectionType == EXACT) R.drawable.ic_detect_exact else R.drawable.ic_detect_whole_screen
            )

            bitmapLoadingJob?.cancel()
            bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
                if (bitmap != null) {
                    conditionImage.setImageBitmap(bitmap)
                } else {
                    conditionImage.setImageDrawable(
                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                }
            }
        }

        itemView.setOnClickListener { conditionClickedListener.invoke(condition) }
    }
}
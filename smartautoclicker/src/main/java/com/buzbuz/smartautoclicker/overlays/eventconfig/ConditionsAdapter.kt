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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.databinding.ItemConditionCardBinding

import kotlinx.coroutines.Job

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param addConditionClickedListener the listener called when the user clicks on the add item. True if this is the
 *                                    first item, false if not.
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionAdapter(
    private val addConditionClickedListener: (Boolean) -> Unit,
    private val conditionClickedListener: (Int, Condition) -> Unit,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?
) : RecyclerView.Adapter<ConditionViewHolder>() {

    /** The list of conditions to be shown by this adapter.*/
    var conditions: ArrayList<Condition>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = conditions?.size?.plus(1) ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder =
        ConditionViewHolder(
            ItemConditionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
        )

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        // The last item is the add item, allowing the user to add a new condition.
        if (position == itemCount - 1) {
            holder.onBindAddCondition(addConditionClickedListener)
        } else {
            holder.onBindCondition(conditions!![position], conditionClickedListener)
        }
    }

    override fun onViewRecycled(holder: ConditionViewHolder) {
        holder.onUnbind()
    }
}

/**
 * View holder displaying a condition in the [ConditionAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionViewHolder(
    private val viewBinding: ItemConditionCardBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a 'Add condition' item.
     *
     * @param addConditionClickedListener listener notified upon user click on this item.
     */
    fun onBindAddCondition(addConditionClickedListener: (Boolean) -> Unit) {
        viewBinding.imageCondition.apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_add)
        }
        itemView.setOnClickListener { addConditionClickedListener.invoke(bindingAdapterPosition == 0) }
    }

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: Condition, conditionClickedListener: (Int, Condition) -> Unit) {
        viewBinding.imageCondition.scaleType = ImageView.ScaleType.FIT_CENTER
        itemView.setOnClickListener { conditionClickedListener.invoke(bindingAdapterPosition, condition) }

        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
            if (bitmap != null) {
                viewBinding.imageCondition.setImageBitmap(bitmap)
            } else {
                viewBinding.imageCondition.setImageDrawable(
                    ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                        setTint(Color.RED)
                    }
                )
            }
        }

        // TODO: is the "waiting state" needed for the wait during the bitmap conditions loading ?
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}
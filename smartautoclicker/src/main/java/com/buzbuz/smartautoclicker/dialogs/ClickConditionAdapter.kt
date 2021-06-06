/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.dialogs

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.model.BitmapManager
import com.buzbuz.smartautoclicker.model.DetectorModel

import kotlinx.coroutines.Job

/**
 * Adapter displaying the conditions for the click displayed by the dialog.
 * Also provide a item displayed in the last position to add a new click condition.
 *
 * @param addConditionClickedListener the listener called when the user clicks on the add item.
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 */
class ConditionAdapter(
    private val addConditionClickedListener: () -> Unit,
    private val conditionClickedListener: (ClickCondition, Int) -> Unit
) : RecyclerView.Adapter<ConditionViewHolder>() {

    /** The list of content bitmap to be shown by this adapter. Contains only the database conditions. */
    var conditions: ArrayList<ClickCondition>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * The list of new conditions created after the display of the dialog. Those conditions are temporary and not yet
     * added to the database.
     */
    private var newConditions: ArrayList<ClickCondition> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * Get all current condition.
     * @return the conditions.
     */
    fun getAllConditions(): List<ClickCondition> {
        val allConditions = ArrayList<ClickCondition>()
        conditions?.let { allConditions.addAll(it) }
        allConditions.addAll(newConditions)
        return allConditions
    }

    /**
     * Add a click condition for the click displayed by the dialog.
     *
     * @param condition the condition to be added.
     */
    fun addCondition(area: Rect, condition: Bitmap) {
        newConditions.add(ClickCondition(area, condition))
        notifyDataSetChanged()
    }

    /**
     * Remove a click condition for the click displayed by the dialog.
     *
     * @param index the index in the list of the condition to be removed.
     */
    fun removeCondition(index: Int) {
        conditions?.size?.let { conditionsCount ->
            if (index < conditionsCount) {
                conditions!!.removeAt(index)
            } else {
                newConditions.removeAt(index - conditionsCount)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = newConditions.size.plus(conditions?.size?.plus(1) ?: 0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder =
        ConditionViewHolder(ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        // The last item is the add item, allowing the user to add a new condition.
        if (position == itemCount - 1) {
            holder.onBindAddCondition(addConditionClickedListener)
        } else {
            conditions?.size?.let { conditionsCount ->
                if (position < conditionsCount) {
                    holder.onBindCondition(conditions!![position], conditionClickedListener)
                } else {
                    holder.onBindCondition(newConditions[position - conditionsCount], conditionClickedListener)
                }
            }
        }
    }

    override fun onViewRecycled(holder: ConditionViewHolder) {
        holder.onUnbind()
    }
}

/**
 * View holder displaying a click condition in the [ConditionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ConditionViewHolder(private val viewBinding: ItemConditionBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    /** The coroutine job fetching asynchronously the condition bitmap from the [BitmapManager]. */
    private var bitmapJob: Job? = null

    /**
     * Bind this view holder as a 'Add condition' item.
     *
     * @param addConditionClickedListener listener notified upon user click on this item.
     */
    fun onBindAddCondition(addConditionClickedListener: () -> Unit) {
        viewBinding.imageCondition.apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_add)
        }
        itemView.setOnClickListener { addConditionClickedListener.invoke() }
    }

    /**
     * Bind this view holder as a 'Click condition' item.
     *
     * @param condition the click condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: ClickCondition, conditionClickedListener: (ClickCondition, Int) -> Unit) {
        viewBinding.imageCondition.scaleType = ImageView.ScaleType.FIT_CENTER
        itemView.setOnClickListener { conditionClickedListener.invoke(condition, bindingAdapterPosition) }

        condition.bitmap?.let {
            viewBinding.imageCondition.setImageBitmap(it)
        } ?: let {
            bitmapJob = DetectorModel.get().getClickConditionBitmap(condition) { bitmap ->
                if (bitmap != null) {
                    viewBinding.imageCondition.setImageBitmap(bitmap)
                } else {
                    viewBinding.imageCondition.setImageDrawable(
                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                }

                bitmapJob = null
            }
        }
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapJob?.cancel()
        bitmapJob = null
    }
}
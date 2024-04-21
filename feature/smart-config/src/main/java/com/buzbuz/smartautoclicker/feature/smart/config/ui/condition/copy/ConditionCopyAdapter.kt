/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemListHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerConditionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.event.conditions.TriggerConditionAdapter

import kotlinx.coroutines.Job

/**
 * Adapter displaying all conditions in a list.
 * @param conditionClickedListener called when the user presses a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionCopyAdapter(
    private val conditionClickedListener: (Condition) -> Unit,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
): ListAdapter<ConditionCopyModel.ConditionCopyItem, RecyclerView.ViewHolder>(ConditionDiffUtilCallback) {

    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int =
            when (getItem(position)) {
                is ConditionCopyModel.ConditionCopyItem.HeaderItem -> 2
                is ConditionCopyModel.ConditionCopyItem.ConditionItem.Trigger -> 2
                is ConditionCopyModel.ConditionCopyItem.ConditionItem.Image -> 1
            }
    }

    override fun getItemViewType(position: Int): Int =
        when(getItem(position)) {
            is ConditionCopyModel.ConditionCopyItem.HeaderItem -> R.layout.item_list_header
            is ConditionCopyModel.ConditionCopyItem.ConditionItem.Image -> R.layout.item_image_condition
            is ConditionCopyModel.ConditionCopyItem.ConditionItem.Trigger -> R.layout.item_trigger_condition
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_list_header -> HeaderViewHolder(
                ItemListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_image_condition -> ImageConditionViewHolder(
                ItemImageConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider
            )
            R.layout.item_trigger_condition -> TriggerConditionViewHolder(
                ItemTriggerConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            )
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as ConditionCopyModel.ConditionCopyItem.HeaderItem)
            is ImageConditionViewHolder -> holder.onBind(
                getItem(position) as ConditionCopyModel.ConditionCopyItem.ConditionItem.Image,
                conditionClickedListener,
            )
            is TriggerConditionViewHolder -> holder.onBind(
                getItem(position) as ConditionCopyModel.ConditionCopyItem.ConditionItem.Trigger,
                conditionClickedListener,
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ImageConditionViewHolder) holder.onUnbind()
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two Conditions when updating the [ConditionCopyAdapter] list. */
object ConditionDiffUtilCallback: DiffUtil.ItemCallback<ConditionCopyModel.ConditionCopyItem>() {

    override fun areItemsTheSame(
        oldItem: ConditionCopyModel.ConditionCopyItem,
        newItem: ConditionCopyModel.ConditionCopyItem,
    ): Boolean =  when {
        oldItem is ConditionCopyModel.ConditionCopyItem.HeaderItem && newItem is ConditionCopyModel.ConditionCopyItem.HeaderItem -> true
        oldItem is ConditionCopyModel.ConditionCopyItem.ConditionItem && newItem is ConditionCopyModel.ConditionCopyItem.ConditionItem ->
            oldItem.condition.id == newItem.condition.id
        else -> false
    }

    override fun areContentsTheSame(
        oldItem: ConditionCopyModel.ConditionCopyItem,
        newItem: ConditionCopyModel.ConditionCopyItem,
    ): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [ConditionCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class HeaderViewHolder(
    private val viewBinding: ItemListHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: ConditionCopyModel.ConditionCopyItem.HeaderItem) {
        viewBinding.textHeader.setText(header.title)
    }
}

/**
 * View holder displaying a condition in the [ConditionCopyAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmap.
 */
private class ImageConditionViewHolder(
    private val viewBinding: ItemImageConditionBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a action item.
     *
     * @param item the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBind(
        item: ConditionCopyModel.ConditionCopyItem.ConditionItem.Image,
        conditionClickedListener: (ImageCondition) -> Unit,
    ) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.bind(
            item.condition,
            bitmapProvider,
            conditionClickedListener,
        )
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

/**
 * View holder displaying a condition in the [TriggerConditionAdapter].
 * @param viewBinding the view binding for this item.
 */
private class TriggerConditionViewHolder(
    private val viewBinding: ItemTriggerConditionBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a condition item.
     *
     * @param item the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBind(
        item: ConditionCopyModel.ConditionCopyItem.ConditionItem.Trigger,
        conditionClickedListener: (TriggerCondition) -> Unit
    ) {
        viewBinding.bind(item.condition, conditionClickedListener)
    }
}
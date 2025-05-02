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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.recyclerview.ViewBindingHolder
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder.ConditionHeaderViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder.ImageConditionViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder.TextConditionViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.viewholder.TriggerConditionViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionHeader
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition

import kotlinx.coroutines.Job

/** Adapter displaying all conditions in a horizontal list. */
internal class UiConditionListAdapter(
    private val onItemClicked: (item: UiCondition, index: Int) -> Unit,
    private val bitmapProvider: ((ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?)? = null,
    private val onItemViewBound: ((Int, View?) -> Unit)? = null,
): ListAdapter<UiConditionItem, RecyclerView.ViewHolder>(UiConditionItemDiffUtilCallback) {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, RecyclerView.HORIZONTAL, false)
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is UiConditionHeader -> R.layout.item_list_header
            is UiImageCondition -> R.layout.item_image_condition_list
            is UiTextCondition -> R.layout.item_text_condition_list
            is UiTriggerCondition -> R.layout.item_trigger_condition
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_list_header -> ConditionHeaderViewHolder(parent)
            R.layout.item_image_condition_list -> ImageConditionViewHolder(parent, bitmapProvider, onItemClicked)
            R.layout.item_text_condition_list -> TextConditionViewHolder(parent, onItemClicked)
            R.layout.item_trigger_condition -> TriggerConditionViewHolder(parent, onItemClicked)
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is UiConditionHeader ->
                (holder as? ConditionHeaderViewHolder)?.onBind(item)
            is UiImageCondition ->
                (holder as? ImageConditionViewHolder)?.onBind(item)
            is UiTextCondition ->
                (holder as? TextConditionViewHolder)?.onBind(item)
            is UiTriggerCondition ->
                (holder as? TriggerConditionViewHolder)?.onBind(item)
        }

        (holder as? ViewBindingHolder<*>)?.viewBinding?.root?.let { itemRoot ->
            onItemViewBound?.invoke(position, itemRoot)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ImageConditionViewHolder) holder.onUnbind()
        onItemViewBound?.invoke(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }
}

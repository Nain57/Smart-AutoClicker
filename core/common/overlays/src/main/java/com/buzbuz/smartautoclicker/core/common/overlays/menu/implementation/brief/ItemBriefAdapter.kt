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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager


internal class ItemBriefAdapter(
    private val displayConfigManager: DisplayConfigManager,
    private val viewHolderTypeProvider: (position: Int) -> Int,
    private val viewHolderCreator: (parent: ViewGroup, viewType: Int, orientation: Int) -> ItemBriefViewHolder<*>,
    private val itemBoundListener: (index: Int, itemView: View?) -> Unit,
    private val onItemClickedListener: (Int, ItemBrief) -> Unit,
) : ListAdapter<ItemBrief, ItemBriefViewHolder<*>>(ItemBriefDiffUtilCallback) {

    private var orientation: Int = displayConfigManager.displayConfig.orientation

    override fun getItemViewType(position: Int): Int {
        val type = viewHolderTypeProvider(position)
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) -type else type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBriefViewHolder<*> =
        viewHolderCreator(
            parent,
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) -viewType else viewType,
            orientation,
        )

    override fun onBindViewHolder(holder: ItemBriefViewHolder<*>, position: Int) {
        holder.onBind(getItem(position), onItemClickedListener)
        itemBoundListener(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ItemBriefViewHolder<*>) {
        itemBoundListener(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }

    public override fun getItem(position: Int): ItemBrief = super.getItem(position)

    @SuppressLint("NotifyDataSetChanged") // Reload the whole list when the orientation is different
    override fun submitList(list: List<ItemBrief>?) {
        if (orientation != displayConfigManager.displayConfig.orientation) {
            orientation = displayConfigManager.displayConfig.orientation
            notifyDataSetChanged()
            return
        }

        super.submitList(list)
    }
}

internal object ItemBriefDiffUtilCallback: DiffUtil.ItemCallback<ItemBrief>() {
    override fun areItemsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem.id == newItem.id

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem.data == newItem.data
}

abstract class ItemBriefViewHolder<T: ViewBinding>(
    protected val viewBinding: T,
) : RecyclerView.ViewHolder(viewBinding.root) {

    abstract fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit)
}

data class ItemBrief(
    val id: Identifier,
    val data: Any,
)
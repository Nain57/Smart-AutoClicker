/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setImageDrawable
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.ItemTutorialBinding
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.ItemTutorialCategoryHeaderBinding
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.ItemTutorialCategoryDividerBinding
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiItems

class TutorialListAdapter(
    private val onItemClicked: (item: TutorialCategoryUiItems.Item) -> Unit,
) : ListAdapter<TutorialCategoryUiItems, ViewHolder>(TutorialCategoryUiItemsDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is TutorialCategoryUiItems.Header -> R.layout.item_tutorial_category_header
            is TutorialCategoryUiItems.Item -> R.layout.item_tutorial
            TutorialCategoryUiItems.SectionDivider -> R.layout.item_tutorial_category_divider
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.item_tutorial_category_header ->
                TutorialCategoryHeaderViewHolder(
                    ItemTutorialCategoryHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )

            R.layout.item_tutorial ->
                TutorialItemViewHolder(
                    ItemTutorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )

            else ->
                TutorialCategoryDividerViewHolder(
                    ItemTutorialCategoryDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TutorialCategoryUiItems.Header -> (holder as TutorialCategoryHeaderViewHolder).onBind(item)
            is TutorialCategoryUiItems.Item -> (holder as TutorialItemViewHolder).onBind(item, onItemClicked)
            TutorialCategoryUiItems.SectionDivider -> Unit // Nothing to do
        }
    }
}

object TutorialCategoryUiItemsDiffUtilCallback : DiffUtil.ItemCallback<TutorialCategoryUiItems>() {

    override fun areItemsTheSame(oldItem: TutorialCategoryUiItems, newItem: TutorialCategoryUiItems): Boolean =
        when (oldItem) {
            is TutorialCategoryUiItems.Header if newItem is TutorialCategoryUiItems.Header -> true
            is TutorialCategoryUiItems.Item.Category if newItem is TutorialCategoryUiItems.Item.Category ->
                oldItem.type == newItem.type

            is TutorialCategoryUiItems.Item.Tutorial if newItem is TutorialCategoryUiItems.Item.Tutorial ->
                oldItem.type == newItem.type

            else -> false
        }

    override fun areContentsTheSame(oldItem: TutorialCategoryUiItems, newItem: TutorialCategoryUiItems): Boolean =
        oldItem == newItem
}

class TutorialCategoryHeaderViewHolder(
    private val binding: ItemTutorialCategoryHeaderBinding,
) : ViewHolder(binding.root) {

    fun onBind(item: TutorialCategoryUiItems.Header) {
        binding.apply {
            titleCategory.setText(item.categoryNameRes)
            descriptionCategory.setText(item.descriptionRes)
            iconCategory.setImageResource(item.iconRes)
        }
    }
}

class TutorialCategoryDividerViewHolder(
    binding: ItemTutorialCategoryDividerBinding,
) : ViewHolder(binding.root)

class TutorialItemViewHolder(private val binding: ItemTutorialBinding) : ViewHolder(binding.root) {

    fun onBind(item: TutorialCategoryUiItems.Item, onItemClicked: (item: TutorialCategoryUiItems.Item) -> Unit) {
        binding.includeFieldSelector.apply {
            setTitle(root.context.getString(item.nameRes))
            setDescription(root.context.getString(item.descriptionRes))
            setImageDrawable(
                AppCompatResources.getDrawable(
                    root.context,
                    item.getIcon(),
                )
            )
        }

        binding.root.setOnClickListener { onItemClicked(item) }
    }

    @DrawableRes
    private fun TutorialCategoryUiItems.Item.getIcon(): Int =
        when (this) {
            is TutorialCategoryUiItems.Item.Category ->
                iconRes

            is TutorialCategoryUiItems.Item.Tutorial ->
                if (tutorialCompleted) R.drawable.ic_tutorial_completed
                else R.drawable.ic_tutorial_not_completed

            is TutorialCategoryUiItems.Item.Slideshow -> R.drawable.ic_tutorial_slideshow
        }
}

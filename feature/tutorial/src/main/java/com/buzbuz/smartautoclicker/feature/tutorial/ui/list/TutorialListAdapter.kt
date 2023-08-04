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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.feature.tutorial.databinding.ItemTutorialBinding

class TutorialListAdapter(
    private val onGameClicked: (gameIndex: Int) -> Unit,
) : ListAdapter<TutorialItem, TutorialItemViewHolder>(TutorialItemDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialItemViewHolder =
        TutorialItemViewHolder(ItemTutorialBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TutorialItemViewHolder, position: Int) {
        holder.onBind(getItem(position), onGameClicked)
    }
}

object TutorialItemDiffUtilCallback : DiffUtil.ItemCallback<TutorialItem>() {

    override fun areItemsTheSame(oldItem: TutorialItem, newItem: TutorialItem): Boolean =
        oldItem.nameResId == newItem.nameResId

    override fun areContentsTheSame(oldItem: TutorialItem, newItem: TutorialItem): Boolean =
        oldItem == newItem
}

class TutorialItemViewHolder(private val binding: ItemTutorialBinding) : ViewHolder(binding.root) {

    fun onBind(item: TutorialItem, onGameClicked: (gameIndex: Int) -> Unit) {
        binding.apply {
            choiceTitle.setText(item.nameResId)
            choiceDescription.setText(item.descResId)
            root.setOnClickListener { onGameClicked(item.index) }
        }
    }
}
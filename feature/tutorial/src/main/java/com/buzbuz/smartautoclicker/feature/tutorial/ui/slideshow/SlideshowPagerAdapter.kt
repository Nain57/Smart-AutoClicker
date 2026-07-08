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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.slideshow

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.tutorial.databinding.ItemTutorialSlideshowPageBinding
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow

internal class SlideshowPagerAdapter(
    private val items: List<TutorialSlideshow.SlideshowItem>,
) : RecyclerView.Adapter<SlideshowPagerAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(ItemTutorialSlideshowPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class PageViewHolder(
        private val binding: ItemTutorialSlideshowPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TutorialSlideshow.SlideshowItem) {
            binding.textDescription.setText(item.tutorialTextRes)
            binding.imageTutorial.setImageResource(item.tutorialImage)

            val metrics = binding.imageTutorial.context.resources.displayMetrics
            val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, item.tutorialImageFormat.widthDp.toFloat(), metrics).toInt()
            val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, item.tutorialImageFormat.heightDp.toFloat(), metrics).toInt()
            binding.imageTutorial.layoutParams = binding.imageTutorial.layoutParams.apply {
                width = widthPx
                height = heightPx
            }
        }
    }
}

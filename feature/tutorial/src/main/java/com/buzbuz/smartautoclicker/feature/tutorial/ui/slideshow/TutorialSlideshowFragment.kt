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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.mapping.toTutorialSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialSlideshowBinding

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TutorialSlideshowFragment : Fragment() {

    private val args: TutorialSlideshowFragmentArgs by navArgs()

    private lateinit var viewBinding: FragmentTutorialSlideshowBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTutorialSlideshowBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val slideshow = args.slideshowType.toTutorialSlideshow()
        val adapter = SlideshowPagerAdapter(slideshow.slideshowItems)

        viewBinding.apply {
            titleSlideshow.setText(slideshow.nameRes)

            viewPager.adapter = adapter
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateButtonLabel(position, adapter.itemCount)
                }
            })
            updateButtonLabel(0, adapter.itemCount)

            buttonNext.setOnClickListener {
                val currentItem = viewPager.currentItem
                if (currentItem < adapter.itemCount - 1) {
                    viewPager.currentItem = currentItem + 1
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun updateButtonLabel(currentPage: Int, pageCount: Int) {
        val isLastPage = currentPage == pageCount - 1
        viewBinding.buttonNext.setText(
            if (isLastPage) R.string.button_text_tutorial_close else R.string.button_text_tutorial_next
        )
    }
}

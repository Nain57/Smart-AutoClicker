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

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2

import com.buzbuz.smartautoclicker.core.ui.utils.getDynamicColorsContext
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.mapping.toTutorialSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.DialogTutorialSlideshowBinding
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow

import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun Context.createTutorialSlideshowDialog(
    slideshowType: TutorialSlideshow.Type,
    pageIndex: Int,
    onDismissed: (() -> Unit)?,
): AlertDialog? = createDialog(slideshowType.toTutorialSlideshow(), IntRange(pageIndex, pageIndex), onDismissed)

internal fun Context.createTutorialSlideshowDialog(
    slideshowType: TutorialSlideshow.Type,
    pageRange: IntRange? = null,
    onDismissed: (() -> Unit)?,
): AlertDialog? = createDialog(slideshowType.toTutorialSlideshow(), pageRange, onDismissed)

private fun Context.createDialog(
    slideshow: TutorialSlideshow,
    pageRange: IntRange?,
    onDismissed: (() -> Unit)?,
): AlertDialog? {

    val pages = pageRange ?: IntRange(0, slideshow.slideshowItems.lastIndex)
    if (pageRange != null && (pageRange.first < 0 || pageRange.last > slideshow.slideshowItems.lastIndex)) {
        Log.e(TAG, "Can't create slideshow dialog, page range is invalid: $pageRange; " +
                "slideshowItems=${slideshow.slideshowItems.size}")
        return null
    }

    val dialogContext = getDynamicColorsContext(R.style.AppTheme)
    val dialogViewBinding = DialogTutorialSlideshowBinding.inflate(LayoutInflater.from(dialogContext))
    val dialog = MaterialAlertDialogBuilder(dialogContext)
        .setView(dialogViewBinding.root)
        .setOnDismissListener { onDismissed?.invoke() }
        .create()

    dialogViewBinding.bind(
        slideshow = slideshow,
        pageRange = pages,
        onCloseClicked = { dialog.dismiss() },
    )

    return dialog
}

private fun DialogTutorialSlideshowBinding.bind(
    slideshow: TutorialSlideshow,
    pageRange: IntRange,
    onCloseClicked: () -> Unit,
) {
    titleSlideshow.setText(slideshow.nameRes)

    val adapter = SlideshowPagerAdapter(slideshow.slideshowItems.subList(pageRange.first, pageRange.last + 1))
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
            onCloseClicked()
        }
    }
}

private fun DialogTutorialSlideshowBinding.updateButtonLabel(currentPage: Int, pageCount: Int) {
    val isLastPage = currentPage == pageCount - 1
    buttonNext.setText(
        if (isLastPage) R.string.button_text_tutorial_close else R.string.button_text_tutorial_next
    )
}

private const val TAG = "TutorialSlideshowDialog"
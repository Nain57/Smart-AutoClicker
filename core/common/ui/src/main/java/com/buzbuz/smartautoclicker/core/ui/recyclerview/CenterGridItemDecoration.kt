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
package com.buzbuz.smartautoclicker.core.ui.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class CenterGridItemDecoration(
    private val itemWidthPx: Int,
    private val horizontalSpacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return

        val spanSizeLookup = layoutManager.spanSizeLookup

        // Only apply to item (span size == 1)
        if (spanSizeLookup.getSpanSize(position) != 1) return

        val totalSpanWidth = parent.width - parent.paddingStart - parent.paddingEnd
        val cellWidth = totalSpanWidth / layoutManager.spanCount
        val extraSpace = cellWidth - itemWidthPx

        val leftMargin = extraSpace / 2
        val rightMargin = extraSpace - leftMargin

        outRect.left = leftMargin + horizontalSpacing / 2
        outRect.right = rightMargin + horizontalSpacing / 2
    }
}
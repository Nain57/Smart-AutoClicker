/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.baseui.utils

import android.content.Context

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlin.math.max

/**
 * [GridLayoutManager] implementation automatically setting the number of columns according to the available width in
 * the [RecyclerView].
 *
 * @param context the Android context.
 * @param columnWidth the width of a column in the grid.
 */
class GridAutoFitLayoutManager(
    context: Context,
    private val columnWidth: Int,
) : GridLayoutManager(context, 1) {

    /** The previous recycler view width. */
    private var lastWidth = 0
    /** The previous recycler view height. */
    private var lastHeight = 0

    /** Ensure the correctness of the provided width. */
    init {
        if (columnWidth <= 0) throw IllegalArgumentException("Column width must be positive.")
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)

        val width = this.width
        val height = this.height
        if (width > 0 && height > 0 && (lastWidth != width || lastHeight != height)) {

            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = max(1, totalSpace / columnWidth)
            if (spanCount != getSpanCount()) {
                setSpanCount(spanCount)
            }
        }

        lastWidth = width
        lastHeight = height
    }
}

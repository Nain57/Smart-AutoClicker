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
package com.buzbuz.smartautoclicker.feature.smart.debugging.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** A list location based on row position, rather than the identity of the row currently displayed there. */
internal data class RecyclerViewScrollPosition(
    val adapterPosition: Int,
    val offsetFromStart: Int,
)

internal fun RecyclerView.captureScrollPosition(): RecyclerViewScrollPosition? {
    val linearLayoutManager = layoutManager as? LinearLayoutManager ?: return null
    val position = linearLayoutManager.findFirstVisibleItemPosition()
    if (position == RecyclerView.NO_POSITION) return null

    val firstVisibleView = linearLayoutManager.findViewByPosition(position) ?: return null
    return RecyclerViewScrollPosition(
        adapterPosition = position,
        offsetFromStart = firstVisibleView.top - paddingTop,
    )
}

internal fun RecyclerView.restoreScrollPosition(position: RecyclerViewScrollPosition?) {
    if (position == null || adapter?.itemCount == 0) return
    val linearLayoutManager = layoutManager as? LinearLayoutManager ?: return
    val restoredPosition = position.adapterPosition.coerceAtMost(adapter!!.itemCount - 1)
    linearLayoutManager.scrollToPositionWithOffset(restoredPosition, position.offsetFromStart)
}

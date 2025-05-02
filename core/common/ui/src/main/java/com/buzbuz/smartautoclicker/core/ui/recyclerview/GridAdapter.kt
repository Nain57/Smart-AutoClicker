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

import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


abstract class GridAdapter<T, VH : RecyclerView.ViewHolder?>(
    diffUtilCallback: DiffUtil.ItemCallback<T>,
) : ListAdapter<T, VH>(diffUtilCallback), ViewTreeObserver.OnGlobalLayoutListener {

    private var recyclerView: RecyclerView? = null
    private var spanCount: Int = 1

    private val spanLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int =
            getSpanSize(position, spanCount)
    }

    /** The width of a single column item, in pixels. */
    abstract fun getSingleSpanItemWidthPx(): Int

    /** Number of columns in the grid. */
    abstract fun getSpanCount(recyclerWidthPx: Int, items: List<T>?): Int

    /** How many columns an item takes. */
    abstract fun getSpanSize(position: Int, spanCount: Int): Int

    @CallSuper
    override fun onAttachedToRecyclerView(view: RecyclerView) {
        recyclerView = view
        view.apply {
            viewTreeObserver.addOnGlobalLayoutListener(this@GridAdapter)
            addItemDecoration(
                CenterGridItemDecoration(getSingleSpanItemWidthPx(), 0)
            )
        }
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(view: RecyclerView) {
        recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        recyclerView = null
    }

    @CallSuper
    override fun submitList(list: List<T>?) {
        setupGridSpan(list)
        super.submitList(list)
    }

    @CallSuper
    override fun submitList(list: List<T>?, commitCallback: Runnable?) {
        setupGridSpan(list)
        super.submitList(list, commitCallback)
    }

    final override fun onGlobalLayout() {
        val recycler = recyclerView ?: return
        if (recycler.width <= 0) return

        setupGridSpan(currentList)

        recycler.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    private fun setupGridSpan(items: List<T>?) {
        val recycler = recyclerView ?: return
        if (recycler.width <= 0 || items.isNullOrEmpty()) return

        spanCount = getSpanCount(recycler.width, items)
        recycler.layoutManager = GridLayoutManager(recycler.context, spanCount).apply {
            spanSizeLookup = spanLookup
        }
    }
}


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

import android.util.Log
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

internal class ItemsBriefSnapHelper : PagerSnapHelper() {

    private var attachedRecyclerView: RecyclerView? = null

    private val adapterDataObserver: RecyclerView.AdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (!initialItemConsumed) {
                val initialIndex = initialItemIndex
                if (initialIndex != null && initialIndex in 1 until itemCount) {
                    Log.d(TAG, "onItemRangeInserted: consume initial item index")
                    snapTo(initialIndex, animateScroll = false)
                }
                initialItemConsumed = true
                return
            }

            Log.d(TAG, "onItemRangeInserted: new item, snapping to last insertion index")
            snapTo(positionStart + itemCount - 1, animateScroll = false)
        }
    }

    private var initialItemConsumed: Boolean = false
    var initialItemIndex: Int? = null

    private val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val newSnapPosition = attachedRecyclerView.findSnapPosition()
            if (newSnapPosition != snapPosition) onSnapPositionUpdated(newSnapPosition)
        }
    }

    var onSnapPositionChangeListener: ((position: Int) -> Unit)? = null
    var snapPosition: Int = 0
        private set

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (attachedRecyclerView == recyclerView) return

        if (attachedRecyclerView != null) detachFromRecyclerView()
        attachedRecyclerView = recyclerView
        snapPosition = attachedRecyclerView.findSnapPosition()

        attachedRecyclerView?.addOnScrollListener(onScrollListener)
        attachedRecyclerView?.adapter?.registerAdapterDataObserver(adapterDataObserver)

        super.attachToRecyclerView(recyclerView)
    }

    fun snapTo(position: Int, animateScroll: Boolean = true) {
        val itemCount = attachedRecyclerView.getListItemCount() ?: return
        if (position < 0 || position >= itemCount) return

        attachedRecyclerView?.snapTo(position, animateScroll)
    }

    private fun onSnapPositionUpdated(newPosition: Int) {
        Log.d(TAG, "onSnapPositionUpdated: $newPosition")

        snapPosition = newPosition
        onSnapPositionChangeListener?.invoke(newPosition)
    }

    private fun detachFromRecyclerView() {
        attachedRecyclerView?.adapter?.unregisterAdapterDataObserver(adapterDataObserver)
        attachedRecyclerView?.removeOnScrollListener(onScrollListener)
        attachedRecyclerView = null
    }

    private fun RecyclerView?.getListItemCount(): Int? =
        this?.adapter?.itemCount

    private fun RecyclerView?.findSnapPosition(): Int {
        val layoutManager = this?.layoutManager ?: return RecyclerView.NO_POSITION
        val snapView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION

        return layoutManager.getPosition(snapView)
    }

    private fun RecyclerView.snapTo(index: Int, animateScroll: Boolean) {
        Log.d(TAG, "snapTo: $index animate=$animateScroll")
        if (animateScroll) smoothScrollToPosition(index)
        else scrollToPosition(index)
    }
}

private const val TAG = "ItemsBriefSnapHelper"
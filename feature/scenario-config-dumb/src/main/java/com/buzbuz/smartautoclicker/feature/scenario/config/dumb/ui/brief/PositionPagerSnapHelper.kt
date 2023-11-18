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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief

import android.util.Log

import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class PositionPagerSnapHelper : PagerSnapHelper() {

    private var attachedRecyclerView: RecyclerView? = null

    private val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val newSnapPosition = findSnapPosition()
            if (newSnapPosition != snapPosition) onSnapPositionUpdated(newSnapPosition)
        }
    }

    var onSnapPositionChangeListener: ((position: Int) -> Unit)? = null
    var snapPosition: Int = 0
        private set

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (attachedRecyclerView == recyclerView) return

        attachedRecyclerView?.removeOnScrollListener(onScrollListener)
        attachedRecyclerView = recyclerView
        snapPosition = findSnapPosition()
        attachedRecyclerView?.addOnScrollListener(onScrollListener)

        super.attachToRecyclerView(recyclerView)
    }

    fun snapTo(position: Int) {
        val itemCount = getListItemCount() ?: return
        if (position < 0 || position >= itemCount) return

        Log.d(TAG, "snapTo: $position")
        attachedRecyclerView?.smoothScrollToPosition(position)
    }

    fun snapToNext() {
        val itemCount = getListItemCount() ?: return
        if (snapPosition == itemCount - 1) return

        val newPosition = snapPosition + 1
        Log.d(TAG, "snapToNext: $newPosition")
        attachedRecyclerView?.smoothScrollToPosition(newPosition)
    }

    fun snapToPrevious() {
        if (snapPosition == 0) return

        val newPosition = snapPosition - 1
        Log.d(TAG, "snapToPrevious: $newPosition")
        attachedRecyclerView?.smoothScrollToPosition(newPosition)
    }

    private fun findSnapPosition(): Int {
        val layoutManager = attachedRecyclerView?.layoutManager ?: return RecyclerView.NO_POSITION
        val snapView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION

        return layoutManager.getPosition(snapView)
    }

    private fun onSnapPositionUpdated(newPosition: Int) {
        Log.d(TAG, "onSnapPositionUpdated: $newPosition")

        snapPosition = newPosition
        onSnapPositionChangeListener?.invoke(newPosition)
    }

    private fun getListItemCount(): Int? =
        attachedRecyclerView?.adapter?.itemCount
}

private const val TAG = "PositionPagerSnapHelper"
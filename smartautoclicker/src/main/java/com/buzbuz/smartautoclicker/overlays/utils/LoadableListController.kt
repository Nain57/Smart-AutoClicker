/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.overlays.utils

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.databinding.MergeLoadableListBinding

/** String res id for the case where the list is empty. */
class LoadableListController<T, VH : RecyclerView.ViewHolder> (
    root: View,
    owner: LifecycleOwner,
    private val adapter: ListAdapter<T, VH>,
    private val emptyTextId: Int,
): DefaultLifecycleObserver {

    /** ViewBinding containing the views for the loadable list merge layout. */
    val listBinding = MergeLoadableListBinding.bind(root)
    /** The list view. */
    val listView: RecyclerView get() = listBinding.list

    init {
        owner.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        listBinding.empty.setText(emptyTextId)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    fun submitList(items: List<T>?) {
        updateViewsVisibility(items)
        adapter.submitList(items)
    }

    private fun updateViewsVisibility(items: List<T>?) {
        listBinding.apply {
            when {
                items == null -> {
                    loading.visibility = View.VISIBLE
                    list.visibility = View.GONE
                    empty.visibility = View.GONE
                }
                items.isEmpty() -> {
                    loading.visibility = View.GONE
                    list.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                }
                else -> {
                    loading.visibility = View.GONE
                    list.visibility = View.VISIBLE
                    empty.visibility = View.GONE
                }
            }
        }
    }
}
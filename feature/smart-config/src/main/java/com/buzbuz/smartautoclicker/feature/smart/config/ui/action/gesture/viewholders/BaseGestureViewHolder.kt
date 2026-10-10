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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem
import kotlinx.coroutines.Job

sealed class BaseGestureViewHolder<T : GestureDialogUiStateItem>(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root) {

    private var displayJob: Job? = null
    protected var boundItem: T? = null
        private set

    fun bind(item: T) {
        boundItem = item

        displayJob?.cancel()
        displayJob = onBind(item)
    }

    fun unbind() {
        displayJob?.cancel()
        displayJob = null

        boundItem = null
    }

    protected open fun onBind(item: T): Job? = null
}